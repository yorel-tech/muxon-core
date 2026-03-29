package com.onetattva.infron.core.orch;

import com.onetattva.infron.api.model.EntityType;
import com.onetattva.infron.api.model.ProviderStatus;
import com.onetattva.infron.core.providers.*;
import com.onetattva.infron.core.providers.libvirt.LibvirtProviderSdk;
import com.onetattva.infron.core.providers.proxmox.ProxmoxProviderSdk;
import com.onetattva.infron.core.providers.storage.StorageDiscoveryProvider;
import com.onetattva.infron.core.providers.storage.StorageDiscoveryProviderRegistry;
import com.onetattva.infron.api.enums.JobStatus;
import com.onetattva.infron.core.spi.queue.CommandMessage;
import com.onetattva.infron.core.spi.queue.CommandQueue;
import com.onetattva.infron.core.spi.queue.ProviderQueueCommands;
import com.onetattva.infron.core.spi.queue.ProviderQueueMetadataKeys;
import com.onetattva.infron.db.model.ProviderEntity;
import com.onetattva.infron.db.model.ProviderStorageEntity;
import com.onetattva.infron.db.repository.JobRepository;
import com.onetattva.infron.db.repository.ProviderRepository;
import com.onetattva.infron.db.repository.ProviderStorageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Orchestrator worker that executes provider connection tests.
 * <p>
 * It polls the DB-backed command queue for commands targeted at {@link EntityType#PROVIDER}.
 */
@Service
public class ProviderConnectionOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(ProviderConnectionOrchestrator.class);

    private static final int POLL_BATCH_SIZE = 10;

    private static final String META_CORRELATION_ID = "connectionTestCorrelationId";
    private static final String META_MESSAGE = "connectionTestMessage";
    private static final String META_LATENCY_MS = "connectionTestLatencyMs";

    @Autowired
    private CommandQueue commandQueue;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private ProviderStorageRepository providerStorageRepository;

    @Autowired
    private StorageDiscoveryProviderRegistry storageDiscoveryProviderRegistry;

    @Autowired
    private JobRepository jobRepository;

    @Value("${infron.provider.storage-discovery-execution-timeout-seconds:300}")
    private int defaultStorageDiscoveryExecutionTimeoutSeconds;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void pollProviderQueue() {
        try {
            List<CommandMessage> entries = commandQueue.pollCommands(EntityType.PROVIDER, POLL_BATCH_SIZE);
            logger.debug("Provider queue poll finished: claimed {} PROVIDER command(s)", entries.size());
            if (!entries.isEmpty()) {
                logger.info("Provider queue poll claimed {} command(s)", entries.size());
            }
            for (CommandMessage entry : entries) {
                processQueueEntry(entry);
            }
        } catch (Exception e) {
            logger.error("Error polling provider queue", e);
        }
    }

    private void processQueueEntry(CommandMessage entry) {
        String queueType = entry.queueType();
        logger.info("Processing provider queue command: id={}, queueType={}, providerId={}, correlationId={}",
                entry.id(), queueType, entry.entityId(), entry.correlationId());
        if (Objects.equals(queueType, ProviderQueueCommands.CONNECTION_TEST)) {
            processConnectionTest(entry);
            return;
        }
        if (Objects.equals(queueType, ProviderQueueCommands.CAPABILITIES_DISCOVERY)) {
            processCapabilitiesDiscovery(entry);
            return;
        }
        if (Objects.equals(queueType, ProviderQueueCommands.STORAGE_DISCOVERY)) {
            processStorageDiscovery(entry);
            return;
        }
        // Unknown queue type for this worker; consider it handled.
        commandQueue.markCompleted(entry.id());
    }

    private void processConnectionTest(CommandMessage entry) {
        UUID providerId = entry.entityId();
        ProviderEntity entity = providerRepository.findById(providerId)
            .orElse(null);
        if (entity == null) {
            commandQueue.markFailed(entry.id(), "Provider not found: " + providerId);
            return;
        }

        try {
            ProviderSdk sdk = resolveProviderSdk(entity.getType());
            ProviderConnectionInfo connectionInfo = ProviderConnectionInfo.builder()
                .endpoint(entity.getEndpoint())
                .credentials(entity.getCredentials())
                .connectionConfig(Map.of())
                .build();

            ProviderSdkConnectionTestResult testResult = sdk.testConnection(connectionInfo);

            String correlationId = entry.correlationId();
            if (entity.getMetadata() == null) {
                entity.setMetadata(new HashMap<>());
            }
            if (correlationId != null) {
                entity.getMetadata().put(META_CORRELATION_ID, correlationId);
            }
            entity.getMetadata().put(META_MESSAGE, testResult.message());
            entity.getMetadata().put(META_LATENCY_MS, String.valueOf(testResult.latencyMs()));

            if (testResult.capabilities() != null) {
                entity.setCapabilities(testResult.capabilities());
            }

            entity.setStatus(testResult.success() ? ProviderStatus.ACTIVE.name() : ProviderStatus.ERROR.name());
            entity.setUpdatedAt(Instant.now());
            providerRepository.save(entity);
            logger.info("Provider connection test completed: providerId={}, success={}, status={}, message={}",
                    providerId, testResult.success(), entity.getStatus(), testResult.message());

            if (testResult.success()) {
                commandQueue.markCompleted(entry.id());
            } else {
                commandQueue.markFailed(entry.id(), testResult.message());
            }
        } catch (Exception e) {
            logger.error("Provider connection test failed unexpectedly for {}: {}", providerId, e.getMessage(), e);

            if (entity.getMetadata() == null) {
                entity.setMetadata(new HashMap<>());
            }
            entity.getMetadata().put(META_MESSAGE, "Connection failed: " + e.getMessage());
            entity.getMetadata().put(META_LATENCY_MS, String.valueOf(0));
            entity.setStatus(ProviderStatus.ERROR.name());
            entity.setUpdatedAt(Instant.now());
            providerRepository.save(entity);

            commandQueue.markFailed(entry.id(), e.getMessage());
        }
    }

    private void processStorageDiscovery(CommandMessage entry) {
        UUID providerId = entry.entityId();
        UUID jobId = parseUuid(entry.metadata().get(ProviderQueueMetadataKeys.JOB_ID));
        int execTimeoutSec = parseExecutionTimeoutSeconds(entry.metadata().get(ProviderQueueMetadataKeys.EXECUTION_TIMEOUT_SECONDS));

        logger.info(
                "Storage discovery command claimed: commandId={}, providerId={}, jobId={}, executionTimeoutSeconds={}",
                entry.id(), providerId, jobId, execTimeoutSec);

        ProviderEntity entity = providerRepository.findById(providerId).orElse(null);
        if (entity == null) {
            String msg = "Provider not found: " + providerId;
            failStorageDiscoveryJob(jobId, msg);
            commandQueue.markFailed(entry.id(), msg);
            return;
        }

        String providerType = entity.getType().toString().toLowerCase();
        Optional<StorageDiscoveryProvider> discoveryProvider = storageDiscoveryProviderRegistry.getProvider(providerType);
        if (discoveryProvider.isEmpty()) {
            String msg = "No storage discovery provider for type: " + providerType;
            logger.warn("Storage discovery aborted: providerId={}, {}", providerId, msg);
            failStorageDiscoveryJob(jobId, msg);
            commandQueue.markFailed(entry.id(), msg);
            return;
        }

        Map<String, Object> connectionInfo = new HashMap<>();
        connectionInfo.put("endpoint", entity.getEndpoint());
        connectionInfo.put("credentials", entity.getCredentials());
        if ("libvirt".equals(providerType)) {
            connectionInfo.put("uri", entity.getEndpoint());
        }

        markStorageDiscoveryJobRunning(jobId);

        try {
            logger.info(
                    "Storage discovery invoking provider adapter: providerId={}, providerType={}, jobId={}, timeoutSeconds={}",
                    providerId, providerType, jobId, execTimeoutSec);

            List<StorageDiscoveryProvider.DiscoveredStorage> discoveredStorage = discoveryProvider.get()
                    .discoverStorage(providerId, connectionInfo)
                    .get(execTimeoutSec, TimeUnit.SECONDS);

            logger.info(
                    "Storage discovery adapter returned {} entries: providerId={}, jobId={}",
                    discoveredStorage.size(), providerId, jobId);

            // Upsert by (provider_id, external_id): discovery may return the same external_id
            // more than once (e.g. per-node pools named alike), and re-sync must be idempotent.
            Instant syncTime = Instant.now();
            Set<String> discoveredExternalIds = new HashSet<>();
            for (StorageDiscoveryProvider.DiscoveredStorage discovered : discoveredStorage) {
                String externalId = discovered.externalId();
                if (externalId == null || externalId.isBlank()) {
                    logger.warn("Skipping discovered storage with blank external_id: providerId={}, jobId={}",
                            providerId, jobId);
                    continue;
                }
                discoveredExternalIds.add(externalId);
                ProviderStorageEntity storageEntity = providerStorageRepository
                        .findByProviderIdAndExternalId(providerId, externalId)
                        .orElseGet(() -> {
                            ProviderStorageEntity e = new ProviderStorageEntity();
                            e.setProviderId(providerId);
                            return e;
                        });
                storageEntity.setProviderType(providerType);
                storageEntity.setExternalId(externalId);
                storageEntity.setName(discovered.name());
                storageEntity.setStorageType(discovered.storageType());
                storageEntity.setCapabilities(discovered.capabilities());
                storageEntity.setMetrics(discovered.metrics());
                storageEntity.setNodeId(discovered.nodeId());
                storageEntity.setEnabled(true);
                storageEntity.setSyncedAt(syncTime);
                providerStorageRepository.save(storageEntity);
            }

            List<ProviderStorageEntity> toRemove = providerStorageRepository.findByProviderId(providerId).stream()
                    .filter(row -> !discoveredExternalIds.contains(row.getExternalId()))
                    .toList();
            if (!toRemove.isEmpty()) {
                providerStorageRepository.deleteAll(toRemove);
            }

            completeStorageDiscoveryJob(jobId, providerId, discoveredStorage.size());
            providerStorageRepository.flush();
            logger.info(
                    "Storage discovery persisted and job completed: commandId={}, providerId={}, entries={}, jobId={}",
                    entry.id(), providerId, discoveredStorage.size(), jobId);
            commandQueue.markCompleted(entry.id());
        } catch (TimeoutException e) {
            String msg = "Storage discovery timed out after " + execTimeoutSec + "s";
            logger.error("Storage discovery timed out: providerId={}, jobId={}, timeoutSeconds={}",
                    providerId, jobId, execTimeoutSec, e);
            failStorageDiscoveryJob(jobId, msg);
            commandQueue.markFailed(entry.id(), msg);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String msg = cause.getMessage() != null ? cause.getMessage() : "Storage discovery failed";
            logger.error("Storage discovery failed: providerId={}, jobId={}, error={}", providerId, jobId, msg, cause);
            failStorageDiscoveryJob(jobId, msg);
            commandQueue.markFailed(entry.id(), msg);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String msg = "Storage discovery interrupted";
            logger.error("Storage discovery interrupted: providerId={}, jobId={}", providerId, jobId, e);
            failStorageDiscoveryJob(jobId, msg);
            commandQueue.markFailed(entry.id(), msg);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Storage discovery failed";
            logger.error("Storage discovery failed: providerId={}, jobId={}", providerId, jobId, e);
            failStorageDiscoveryJob(jobId, msg);
            commandQueue.markFailed(entry.id(), msg);
        }
    }

    private int parseExecutionTimeoutSeconds(String raw) {
        if (raw == null || raw.isBlank()) {
            return defaultStorageDiscoveryExecutionTimeoutSeconds;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            return v > 0 ? v : defaultStorageDiscoveryExecutionTimeoutSeconds;
        } catch (NumberFormatException e) {
            return defaultStorageDiscoveryExecutionTimeoutSeconds;
        }
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void markStorageDiscoveryJobRunning(UUID jobId) {
        if (jobId == null) {
            return;
        }
        jobRepository.findById(jobId).ifPresent(job -> {
            if (job.getStatus() != JobStatus.PENDING) {
                logger.warn("Storage discovery job {} not in PENDING (was {}); still marking RUNNING", jobId, job.getStatus());
            }
            job.setStatus(JobStatus.RUNNING);
            job.setStartedAt(Instant.now());
            job.setLastHeartbeatAt(Instant.now());
            job.setCurrentStep("Discovering storage from provider");
            jobRepository.save(job);
            logger.info("Storage discovery job marked RUNNING: jobId={}", jobId);
        });
    }

    private void completeStorageDiscoveryJob(UUID jobId, UUID providerId, int discoveredCount) {
        if (jobId == null) {
            return;
        }
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            job.setProgressPercentage(100);
            job.setCurrentStep(null);
            job.setResult("{\"discoveredCount\":" + discoveredCount + ",\"providerId\":\"" + providerId + "\"}");
            jobRepository.save(job);
            logger.info("Storage discovery job COMPLETED: jobId={}, discoveredCount={}", jobId, discoveredCount);
        });
    }

    private void failStorageDiscoveryJob(UUID jobId, String message) {
        if (jobId == null) {
            return;
        }
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JobStatus.FAILED);
            job.setCompletedAt(Instant.now());
            job.setErrorMessage(message);
            job.setCurrentStep(null);
            jobRepository.save(job);
            logger.info("Storage discovery job FAILED: jobId={}, message={}", jobId, message);
        });
    }

    private void processCapabilitiesDiscovery(CommandMessage entry) {
        UUID providerId = entry.entityId();
        ProviderEntity entity = providerRepository.findById(providerId).orElse(null);
        if (entity == null) {
            commandQueue.markFailed(entry.id(), "Provider not found: " + providerId);
            return;
        }

        try {
            ProviderSdk sdk = resolveProviderSdk(entity.getType());
            ProviderConnectionInfo connectionInfo = ProviderConnectionInfo.builder()
                    .endpoint(entity.getEndpoint())
                    .credentials(entity.getCredentials())
                    .connectionConfig(Map.of())
                    .build();

            Map<String, String> capabilities = sdk.getCapabilities(connectionInfo);
            if (capabilities != null) {
                entity.setCapabilities(capabilities);
            }
            if (entity.getMetadata() == null) {
                entity.setMetadata(new HashMap<>());
            }
            entity.getMetadata().put("capabilitiesLastDiscoveredAt", Instant.now().toString());
            entity.setUpdatedAt(Instant.now());
            providerRepository.save(entity);
            logger.info("Provider capabilities discovery completed: providerId={}, capabilitiesKeys={}",
                    providerId, capabilities != null ? capabilities.keySet() : java.util.Set.of());
            commandQueue.markCompleted(entry.id());
        } catch (Exception e) {
            logger.warn("Capabilities discovery failed for provider {}: {}", providerId, e.getMessage());
            commandQueue.markFailed(entry.id(), e.getMessage());
        }
    }

    private ProviderSdk resolveProviderSdk(com.onetattva.infron.api.model.ProviderType type) {
        return switch (type) {
            case PROXMOX -> new ProxmoxProviderSdk();
            case LIBVIRT -> new LibvirtProviderSdk();
        };
    }
}

