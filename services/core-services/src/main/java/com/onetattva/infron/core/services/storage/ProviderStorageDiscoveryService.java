package com.onetattva.infron.core.services.storage;

import com.onetattva.infron.api.model.EntityType;
import com.onetattva.infron.api.enums.QueueStatus;
import com.onetattva.infron.db.model.ProviderEntity;
import com.onetattva.infron.db.model.ProviderStorageEntity;
import com.onetattva.infron.db.repository.ProviderRepository;
import com.onetattva.infron.db.repository.ProviderStorageRepository;
import com.onetattva.infron.db.repository.QueueEntryRepository;
import com.onetattva.infron.core.spi.queue.CommandMessage;
import com.onetattva.infron.core.spi.queue.CommandQueue;
import com.onetattva.infron.core.spi.queue.ProviderQueueCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Coordinates provider storage discovery by enqueueing work for the orchestrator.
 * <p>
 * The orchestrator runs {@link com.onetattva.infron.core.providers.storage.StorageDiscoveryProvider}
 * implementations (Libvirt, Proxmox, etc.) and persists {@link ProviderStorageEntity} rows.
 * </p>
 */
@Service
public class ProviderStorageDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(ProviderStorageDiscoveryService.class);

    private static final Set<String> ORCHESTRATED_DISCOVERY_TYPES = Set.of("libvirt", "proxmox");

    private final ProviderRepository providerRepository;
    private final ProviderStorageRepository providerStorageRepository;
    private final CommandQueue commandQueue;
    private final QueueEntryRepository queueEntryRepository;
    private final long storageDiscoveryTimeoutMs;

    public ProviderStorageDiscoveryService(
            ProviderRepository providerRepository,
            ProviderStorageRepository providerStorageRepository,
            CommandQueue commandQueue,
            QueueEntryRepository queueEntryRepository,
            @Value("${infron.provider.storage-discovery-timeout-ms:120000}") long storageDiscoveryTimeoutMs) {
        this.providerRepository = providerRepository;
        this.providerStorageRepository = providerStorageRepository;
        this.commandQueue = commandQueue;
        this.queueEntryRepository = queueEntryRepository;
        this.storageDiscoveryTimeoutMs = storageDiscoveryTimeoutMs;
    }

    /**
     * Enqueue storage discovery and wait for the orchestrator to complete it.
     *
     * @return number of storage rows after sync
     */
    @Transactional
    public int discoverAndSyncStorage(UUID providerId) {
        log.info("Starting storage discovery for provider {}", providerId);

        ProviderEntity provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));

        String providerType = provider.getType().toString().toLowerCase();
        if (!ORCHESTRATED_DISCOVERY_TYPES.contains(providerType)) {
            log.info("Storage discovery not orchestrated for provider type {}; skipping", providerType);
            return 0;
        }

        String correlationId = UUID.randomUUID().toString();
        CommandMessage command = CommandMessage.builder()
                .queueType(ProviderQueueCommands.STORAGE_DISCOVERY)
                .entityType(EntityType.PROVIDER)
                .entityId(providerId)
                .payload(Map.of())
                .metadata(Map.of("source", "core-services"))
                .source("core-services")
                .actorType("SYSTEM")
                .actorService("core-services")
                .createdAt(Instant.now())
                .correlationId(correlationId)
                .build();

        UUID commandId = commandQueue.sendCommand(command);
        log.info("Enqueued storage discovery: providerId={}, commandId={}", providerId, commandId);

        long deadline = System.currentTimeMillis() + storageDiscoveryTimeoutMs;
        while (System.currentTimeMillis() < deadline) {
            QueueEntryRepository.QueueStatusErrorProjection state =
                    queueEntryRepository.findStatusAndErrorById(commandId).orElse(null);
            if (state != null) {
                QueueStatus status = state.getStatus();
                if (status == QueueStatus.COMPLETED) {
                    int count = providerStorageRepository.findByProviderId(providerId).size();
                    log.info("Storage discovery finished for provider {}: {} entries", providerId, count);
                    return count;
                }
                if (status == QueueStatus.FAILED) {
                    String err = state.getErrorMessage() != null ? state.getErrorMessage() : "Storage discovery failed";
                    throw new IllegalStateException(err);
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for storage discovery", ie);
            }
        }

        throw new IllegalStateException(
                "Storage discovery timed out for provider " + providerId + " (commandId: " + commandId + ")");
    }

    /**
     * Discover storage for all providers (sequential; each waits on the orchestrator).
     */
    @Transactional
    public Map<UUID, Integer> discoverAllProviders() {
        Map<UUID, Integer> results = new HashMap<>();
        List<ProviderEntity> providers = providerRepository.findAll();
        log.info("Discovering storage for {} providers", providers.size());
        for (ProviderEntity provider : providers) {
            try {
                results.put(provider.getId(), discoverAndSyncStorage(provider.getId()));
            } catch (Exception e) {
                log.error("Failed to discover storage for provider {}: {}", provider.getId(), e.getMessage(), e);
                results.put(provider.getId(), 0);
            }
        }
        return results;
    }

    public List<ProviderStorageEntity> getProviderStorage(UUID providerId) {
        return providerStorageRepository.findByProviderId(providerId);
    }

    public List<ProviderStorageEntity> getEnabledProviderStorage(UUID providerId) {
        return providerStorageRepository.findByProviderIdAndEnabled(providerId, true);
    }

    public boolean hasDiscoveredStorage(UUID providerId) {
        return !providerStorageRepository.findByProviderId(providerId).isEmpty();
    }

    /**
     * Types for which the orchestrator is expected to handle storage discovery.
     */
    public boolean isDiscoverySupported(String providerType) {
        if (providerType == null) {
            return false;
        }
        return ORCHESTRATED_DISCOVERY_TYPES.contains(providerType.toLowerCase());
    }
}
