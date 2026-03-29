package com.onetattva.infron.core.orch;

import com.onetattva.infron.api.model.EntityType;
import com.onetattva.infron.api.model.Node;
import com.onetattva.infron.api.model.NodeCluster;
import com.onetattva.infron.api.model.ProviderStatus;
import com.onetattva.infron.core.providers.*;
import com.onetattva.infron.core.providers.libvirt.LibvirtNodeInventorySupport;
import com.onetattva.infron.core.providers.libvirt.LibvirtProviderSdk;
import com.onetattva.infron.core.providers.proxmox.ProxmoxNodeInventoryProvider;
import com.onetattva.infron.core.providers.proxmox.ProxmoxProviderSdk;
import com.onetattva.infron.core.providers.storage.StorageDiscoveryProvider;
import com.onetattva.infron.core.providers.storage.StorageDiscoveryProviderRegistry;
import com.onetattva.infron.api.enums.JobStatus;
import com.onetattva.infron.core.spi.queue.CommandMessage;
import com.onetattva.infron.core.spi.queue.CommandQueue;
import com.onetattva.infron.core.spi.queue.ProviderQueueCommands;
import com.onetattva.infron.core.spi.queue.ProviderQueueMetadataKeys;
import com.onetattva.infron.db.model.NodeClusterEntity;
import com.onetattva.infron.db.model.NodeEntity;
import com.onetattva.infron.db.model.ProviderEntity;
import com.onetattva.infron.db.model.ProviderStorageEntity;
import com.onetattva.infron.db.repository.JobRepository;
import com.onetattva.infron.db.repository.NodeClusterRepository;
import com.onetattva.infron.db.repository.NodeRepository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

    private static final String META_INVENTORY_CLUSTER_KIND = "infron.inventory.kind";
    private static final String META_INVENTORY_NODE_SOURCE = "infron.inventory.source";
    private static final String CLUSTER_KIND_PROXMOX = "proxmox";

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

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private NodeClusterRepository nodeClusterRepository;

    @Value("${infron.provider.storage-discovery-execution-timeout-seconds:300}")
    private int defaultStorageDiscoveryExecutionTimeoutSeconds;

    @Value("${infron.provider.inventory-sync-execution-timeout-seconds:900}")
    private int defaultInventorySyncExecutionTimeoutSeconds;

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
        if (Objects.equals(queueType, ProviderQueueCommands.INVENTORY_SYNC)) {
            processInventorySync(entry);
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
        if (storageDiscoveryProviderRegistry.getProvider(providerType).isEmpty()) {
            String msg = "No storage discovery provider for type: " + providerType;
            logger.warn("Storage discovery aborted: providerId={}, {}", providerId, msg);
            failStorageDiscoveryJob(jobId, msg);
            commandQueue.markFailed(entry.id(), msg);
            return;
        }

        markStorageDiscoveryJobRunning(jobId);

        try {
            logger.info(
                    "Storage discovery invoking provider adapter: providerId={}, providerType={}, jobId={}, timeoutSeconds={}",
                    providerId, providerType, jobId, execTimeoutSec);

            int discoveredCount = runStorageDiscoveryAndPersist(providerId, providerType, entity, execTimeoutSec);

            completeStorageDiscoveryJob(jobId, providerId, discoveredCount);
            providerStorageRepository.flush();
            logger.info(
                    "Storage discovery persisted and job completed: commandId={}, providerId={}, entries={}, jobId={}",
                    entry.id(), providerId, discoveredCount, jobId);
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

    private void processInventorySync(CommandMessage entry) {
        UUID providerId = entry.entityId();
        UUID jobId = parseUuid(entry.metadata().get(ProviderQueueMetadataKeys.JOB_ID));
        int execTimeoutSec = parseInventorySyncExecutionTimeoutSeconds(
                entry.metadata().get(ProviderQueueMetadataKeys.EXECUTION_TIMEOUT_SECONDS));

        logger.info(
                "Provider inventory sync claimed: commandId={}, providerId={}, jobId={}, executionTimeoutSeconds={}",
                entry.id(), providerId, jobId, execTimeoutSec);

        ProviderEntity entity = providerRepository.findById(providerId).orElse(null);
        if (entity == null) {
            String msg = "Provider not found: " + providerId;
            failInventorySyncJob(jobId, msg);
            commandQueue.markFailed(entry.id(), msg);
            return;
        }

        String providerType = entity.getType().toString().toLowerCase();
        if (!"proxmox".equals(providerType) && !"libvirt".equals(providerType)) {
            String msg = "Provider inventory sync is not supported for type: " + providerType;
            failInventorySyncJob(jobId, msg);
            commandQueue.markFailed(entry.id(), msg);
            return;
        }

        if (storageDiscoveryProviderRegistry.getProvider(providerType).isEmpty()) {
            String msg = "No storage discovery provider for type: " + providerType;
            failInventorySyncJob(jobId, msg);
            commandQueue.markFailed(entry.id(), msg);
            return;
        }

        markInventorySyncJobRunning(jobId);

        try {
            int nodeCount;
            if ("proxmox".equals(providerType)) {
                jobRepository.findById(jobId).ifPresent(j -> {
                    j.setCurrentStep("Refreshing provider capabilities");
                    j.setLastHeartbeatAt(Instant.now());
                    jobRepository.save(j);
                });
                refreshCapabilitiesFromSdk(entity);
                jobRepository.findById(jobId).ifPresent(j -> {
                    j.setCurrentStep("Syncing Proxmox nodes and cluster");
                    j.setLastHeartbeatAt(Instant.now());
                    jobRepository.save(j);
                });
                nodeCount = syncProxmoxNodesAndCluster(providerId, entity);
            } else {
                jobRepository.findById(jobId).ifPresent(j -> {
                    j.setCurrentStep("Refreshing Libvirt node metrics");
                    j.setLastHeartbeatAt(Instant.now());
                    jobRepository.save(j);
                });
                nodeCount = refreshLibvirtNodes(providerId);
            }

            jobRepository.findById(jobId).ifPresent(j -> {
                j.setCurrentStep("Discovering storage from provider");
                j.setLastHeartbeatAt(Instant.now());
                jobRepository.save(j);
            });

            int storageCount = runStorageDiscoveryAndPersist(providerId, providerType, entity, execTimeoutSec);

            completeInventorySyncJob(jobId, providerId, nodeCount, storageCount);
            providerStorageRepository.flush();
            logger.info(
                    "Provider inventory sync completed: commandId={}, providerId={}, nodesTouched={}, storageEntries={}, jobId={}",
                    entry.id(), providerId, nodeCount, storageCount, jobId);
            commandQueue.markCompleted(entry.id());
        } catch (TimeoutException e) {
            String msg = "Provider inventory sync timed out after " + execTimeoutSec + "s";
            logger.error("Provider inventory sync timed out: providerId={}, jobId={}", providerId, jobId, e);
            failInventorySyncJob(jobId, msg);
            commandQueue.markFailed(entry.id(), msg);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String msg = cause.getMessage() != null ? cause.getMessage() : "Provider inventory sync failed";
            logger.error("Provider inventory sync failed: providerId={}, jobId={}, error={}", providerId, jobId, msg, cause);
            failInventorySyncJob(jobId, msg);
            commandQueue.markFailed(entry.id(), msg);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String msg = "Provider inventory sync interrupted";
            logger.error("Provider inventory sync interrupted: providerId={}, jobId={}", providerId, jobId, e);
            failInventorySyncJob(jobId, msg);
            commandQueue.markFailed(entry.id(), msg);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Provider inventory sync failed";
            logger.error("Provider inventory sync failed: providerId={}, jobId={}", providerId, jobId, e);
            failInventorySyncJob(jobId, msg);
            commandQueue.markFailed(entry.id(), msg);
        }
    }

    private int runStorageDiscoveryAndPersist(
            UUID providerId,
            String providerType,
            ProviderEntity entity,
            int execTimeoutSec) throws Exception {
        Optional<StorageDiscoveryProvider> discoveryProvider = storageDiscoveryProviderRegistry.getProvider(providerType);
        if (discoveryProvider.isEmpty()) {
            throw new IllegalStateException("No storage discovery provider for type: " + providerType);
        }
        Map<String, Object> connectionInfo = buildProviderConnectionMap(entity, providerType);
        List<StorageDiscoveryProvider.DiscoveredStorage> discoveredStorage = discoveryProvider.get()
                .discoverStorage(providerId, connectionInfo)
                .get(execTimeoutSec, TimeUnit.SECONDS);

        logger.info(
                "Storage discovery adapter returned {} entries: providerId={}",
                discoveredStorage.size(), providerId);

        persistDiscoveredStorage(providerId, providerType, discoveredStorage);
        return discoveredStorage.size();
    }

    private Map<String, Object> buildProviderConnectionMap(ProviderEntity entity, String providerType) {
        Map<String, Object> connectionInfo = new HashMap<>();
        connectionInfo.put("endpoint", entity.getEndpoint());
        connectionInfo.put("credentials", entity.getCredentials());
        if ("libvirt".equals(providerType)) {
            connectionInfo.put("uri", entity.getEndpoint());
        }
        return connectionInfo;
    }

    private void persistDiscoveredStorage(
            UUID providerId,
            String providerType,
            List<StorageDiscoveryProvider.DiscoveredStorage> discoveredStorage) {
        Instant syncTime = Instant.now();
        Set<String> discoveredExternalIds = new HashSet<>();
        for (StorageDiscoveryProvider.DiscoveredStorage discovered : discoveredStorage) {
            String externalId = discovered.externalId();
            if (externalId == null || externalId.isBlank()) {
                logger.warn("Skipping discovered storage with blank external_id: providerId={}", providerId);
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
    }

    private void refreshCapabilitiesFromSdk(ProviderEntity entity) throws Exception {
        UUID providerId = entity.getId();
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
        logger.info("Provider capabilities refreshed during inventory sync: providerId={}, keys={}",
                providerId, capabilities != null ? capabilities.keySet() : Set.of());
    }

    private int syncProxmoxNodesAndCluster(UUID providerId, ProviderEntity entity) {
        Map<String, Object> connectionInfo = buildProviderConnectionMap(entity, "proxmox");
        ProxmoxNodeInventoryProvider inventory = new ProxmoxNodeInventoryProvider();
        ProxmoxNodeInventoryProvider.InventorySnapshot snapshot = inventory.discoverInventory(connectionInfo);
        List<ProxmoxNodeInventoryProvider.DiscoveredNode> discovered = snapshot.nodes();
        NodeClusterEntity cluster = resolveProxmoxInventoryCluster(entity, snapshot.clusterName());
        Set<String> seen = new HashSet<>();
        Instant now = Instant.now();

        for (ProxmoxNodeInventoryProvider.DiscoveredNode dn : discovered) {
            seen.add(dn.name());
            NodeEntity node = nodeRepository.findByProvider_IdAndExternalId(providerId, dn.name())
                    .orElseGet(() -> {
                        NodeEntity n = new NodeEntity();
                        n.setProvider(entity);
                        n.setExternalId(dn.name());
                        return n;
                    });
            node.setName(dn.name());
            node.setCluster(cluster);
            if (dn.maxCpu() > 0) {
                node.setCpuTotal(dn.maxCpu());
            }
            if (dn.maxMemBytes() > 0) {
                long memMb = dn.maxMemBytes() / (1024L * 1024L);
                if (memMb > 0 && memMb <= Integer.MAX_VALUE) {
                    node.setMemMb((int) memMb);
                }
            }
            node.setStatus(mapProxmoxNodeStatus(dn.rawStatus()));
            node.setLastSeenAt(now);
            if (node.getMetadata() == null) {
                node.setMetadata(new HashMap<>());
            }
            node.getMetadata().put(META_INVENTORY_NODE_SOURCE, CLUSTER_KIND_PROXMOX);
            nodeRepository.save(node);
        }

        removeStaleProxmoxInventoryNodes(providerId, cluster.getId(), seen);
        return discovered.size();
    }

    private NodeClusterEntity resolveProxmoxInventoryCluster(ProviderEntity entity,
            Optional<String> proxmoxClusterName) {
        UUID providerId = entity.getId();
        String external = proxmoxClusterName
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
        String fallbackName = entity.getName() + " (Proxmox)";
        String displayName = external != null ? external : fallbackName;

        List<NodeClusterEntity> clusters = nodeClusterRepository.findByProvider_Id(providerId);
        for (NodeClusterEntity c : clusters) {
            if (CLUSTER_KIND_PROXMOX.equals(clusterInventoryKind(c))) {
                if (external == null) {
                    return c;
                }
                boolean changed = false;
                if (!Objects.equals(external, c.getExternalId())) {
                    c.setExternalId(external);
                    changed = true;
                }
                if (!external.equals(c.getName())) {
                    c.setName(external);
                    changed = true;
                }
                if (changed) {
                    logger.info(
                            "Proxmox inventory cluster row updated from API: providerId={}, name={}, externalId={}",
                            providerId, external, external);
                    return nodeClusterRepository.save(c);
                }
                return c;
            }
        }
        NodeClusterEntity c = new NodeClusterEntity(displayName);
        c.setProvider(entity);
        c.setStatus(NodeCluster.StatusEnum.READY);
        if (external != null) {
            c.setExternalId(external);
        }
        c.getMetadata().put(META_INVENTORY_CLUSTER_KIND, CLUSTER_KIND_PROXMOX);
        NodeClusterEntity saved = nodeClusterRepository.save(c);
        if (external != null) {
            logger.info("Created Proxmox inventory cluster from API name: providerId={}, name={}", providerId, external);
        }
        return saved;
    }

    private static String clusterInventoryKind(NodeClusterEntity c) {
        return c.getMetadata() != null ? c.getMetadata().get(META_INVENTORY_CLUSTER_KIND) : null;
    }

    private void removeStaleProxmoxInventoryNodes(UUID providerId, UUID inventoryClusterId, Set<String> seenExternalIds) {
        List<NodeEntity> all = nodeRepository.findByProviderId(providerId);
        List<NodeEntity> toDelete = new ArrayList<>();
        for (NodeEntity n : all) {
            if (n.getCluster() == null) {
                continue;
            }
            if (!inventoryClusterId.equals(n.getCluster().getId())) {
                continue;
            }
            if (!CLUSTER_KIND_PROXMOX.equals(inventoryNodeSource(n))) {
                continue;
            }
            if (n.getExternalId() != null && !seenExternalIds.contains(n.getExternalId())) {
                toDelete.add(n);
            }
        }
        if (!toDelete.isEmpty()) {
            nodeRepository.deleteAll(toDelete);
            logger.info("Removed {} stale Proxmox inventory node(s) for provider {}", toDelete.size(), providerId);
        }
    }

    private static String inventoryNodeSource(NodeEntity n) {
        return n.getMetadata() != null ? n.getMetadata().get(META_INVENTORY_NODE_SOURCE) : null;
    }

    private static Node.StatusEnum mapProxmoxNodeStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return Node.StatusEnum.UNKNOWN;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "online" -> Node.StatusEnum.READY;
            case "offline" -> Node.StatusEnum.DOWN;
            default -> Node.StatusEnum.UNKNOWN;
        };
    }

    private int refreshLibvirtNodes(UUID providerId) {
        List<NodeEntity> nodes = nodeRepository.findByProviderId(providerId);
        int ok = 0;
        for (NodeEntity node : nodes) {
            try {
                String uri = LibvirtNodeInventorySupport.connectionUriForNode(node);
                Map<String, String> creds = node.getCredentials() != null ? node.getCredentials() : Map.of();
                Optional<LibvirtNodeInventorySupport.Hardware> hw = LibvirtNodeInventorySupport.probe(uri, creds);
                if (hw.isPresent()) {
                    if (hw.get().cpuTotal() > 0) {
                        node.setCpuTotal(hw.get().cpuTotal());
                    }
                    if (hw.get().memMb() > 0) {
                        node.setMemMb(hw.get().memMb());
                    }
                    node.setStatus(Node.StatusEnum.READY);
                    node.setLastSeenAt(Instant.now());
                    nodeRepository.save(node);
                    ok++;
                } else {
                    logger.warn("Libvirt node refresh failed for node {} (no hardware from libvirt)", node.getId());
                    node.setStatus(Node.StatusEnum.DOWN);
                    nodeRepository.save(node);
                }
            } catch (Exception e) {
                logger.warn("Libvirt node refresh failed for node {}: {}", node.getId(), e.getMessage());
                node.setStatus(Node.StatusEnum.DOWN);
                nodeRepository.save(node);
            }
        }
        return ok;
    }

    private void markInventorySyncJobRunning(UUID jobId) {
        if (jobId == null) {
            return;
        }
        jobRepository.findById(jobId).ifPresent(job -> {
            if (job.getStatus() != JobStatus.PENDING) {
                logger.warn("Inventory sync job {} not in PENDING (was {}); still marking RUNNING", jobId, job.getStatus());
            }
            job.setStatus(JobStatus.RUNNING);
            job.setStartedAt(Instant.now());
            job.setLastHeartbeatAt(Instant.now());
            job.setCurrentStep("Syncing provider inventory");
            jobRepository.save(job);
            logger.info("Provider inventory sync job marked RUNNING: jobId={}", jobId);
        });
    }

    private void completeInventorySyncJob(UUID jobId, UUID providerId, int nodeCount, int storageCount) {
        if (jobId == null) {
            return;
        }
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            job.setProgressPercentage(100);
            job.setCurrentStep(null);
            job.setResult(String.format(Locale.US,
                    "{\"providerId\":\"%s\",\"nodeCount\":%d,\"discoveredStorageCount\":%d}",
                    providerId, nodeCount, storageCount));
            jobRepository.save(job);
            logger.info("Provider inventory sync job COMPLETED: jobId={}, nodeCount={}, storageCount={}",
                    jobId, nodeCount, storageCount);
        });
    }

    private void failInventorySyncJob(UUID jobId, String message) {
        if (jobId == null) {
            return;
        }
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JobStatus.FAILED);
            job.setCompletedAt(Instant.now());
            job.setErrorMessage(message);
            job.setCurrentStep(null);
            jobRepository.save(job);
            logger.info("Provider inventory sync job FAILED: jobId={}, message={}", jobId, message);
        });
    }

    private int parseInventorySyncExecutionTimeoutSeconds(String raw) {
        if (raw == null || raw.isBlank()) {
            return defaultInventorySyncExecutionTimeoutSeconds;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            return v > 0 ? v : defaultInventorySyncExecutionTimeoutSeconds;
        } catch (NumberFormatException e) {
            return defaultInventorySyncExecutionTimeoutSeconds;
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

