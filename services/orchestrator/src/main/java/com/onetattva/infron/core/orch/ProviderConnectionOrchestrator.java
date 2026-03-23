package com.onetattva.infron.core.orch;

import com.onetattva.infron.api.enums.EntityType;
import com.onetattva.infron.api.model.ProviderStatus;
import com.onetattva.infron.core.providers.*;
import com.onetattva.infron.core.providers.libvirt.LibvirtProviderSdk;
import com.onetattva.infron.core.providers.proxmox.ProxmoxProviderSdk;
import com.onetattva.infron.core.spi.queue.CommandMessage;
import com.onetattva.infron.core.spi.queue.CommandQueue;
import com.onetattva.infron.db.model.ProviderEntity;
import com.onetattva.infron.db.repository.ProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Orchestrator worker that executes provider connection tests.
 * <p>
 * It polls the DB-backed command queue for commands targeted at {@link EntityType#PROVIDER}.
 */
@Service
public class ProviderConnectionOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(ProviderConnectionOrchestrator.class);

    private static final String PROVIDER_CONNECTION_TEST_COMMAND = "PROVIDER_CONNECTION_TEST_COMMAND";
    private static final String PROVIDER_CAPABILITIES_DISCOVERY_COMMAND = "PROVIDER_CAPABILITIES_DISCOVERY_COMMAND";
    private static final int POLL_BATCH_SIZE = 10;

    private static final String META_CORRELATION_ID = "connectionTestCorrelationId";
    private static final String META_MESSAGE = "connectionTestMessage";
    private static final String META_LATENCY_MS = "connectionTestLatencyMs";

    @Autowired
    private CommandQueue commandQueue;

    @Autowired
    private ProviderRepository providerRepository;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void pollProviderQueue() {
        try {
            List<CommandMessage> entries = commandQueue.pollCommands(EntityType.PROVIDER, POLL_BATCH_SIZE);
            for (CommandMessage entry : entries) {
                processQueueEntry(entry);
            }
        } catch (Exception e) {
            logger.error("Error polling provider queue", e);
        }
    }

    private void processQueueEntry(CommandMessage entry) {
        String queueType = entry.queueType();
        if (Objects.equals(queueType, PROVIDER_CONNECTION_TEST_COMMAND)) {
            processConnectionTest(entry);
            return;
        }
        if (Objects.equals(queueType, PROVIDER_CAPABILITIES_DISCOVERY_COMMAND)) {
            processCapabilitiesDiscovery(entry);
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

