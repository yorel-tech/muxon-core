package com.onetattva.infron.core.services.storage;

import com.onetattva.infron.api.dto.TaskCreateRequest;
import com.onetattva.infron.api.enums.EntityType;
import com.onetattva.infron.api.enums.JobType;
import com.onetattva.infron.db.model.ProviderEntity;
import com.onetattva.infron.db.model.ProviderStorageEntity;
import com.onetattva.infron.db.repository.ProviderRepository;
import com.onetattva.infron.db.repository.ProviderStorageRepository;
import com.onetattva.infron.db.repository.QueueEntryRepository;
import com.onetattva.infron.core.services.task.TaskOrchestrationService;
import com.onetattva.infron.core.spi.queue.CommandMessage;
import com.onetattva.infron.core.spi.queue.CommandQueue;
import com.onetattva.infron.core.spi.queue.ProviderQueueCommands;
import com.onetattva.infron.core.spi.queue.ProviderQueueMetadataKeys;
import com.onetattva.infron.db.model.JobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Coordinates provider storage discovery by enqueueing work for the orchestrator
 * and creating a {@link JobEntity} for API polling.
 */
@Service
public class ProviderStorageDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(ProviderStorageDiscoveryService.class);

    private static final Set<String> ORCHESTRATED_DISCOVERY_TYPES = Set.of("libvirt", "proxmox");

    private final ProviderRepository providerRepository;
    private final ProviderStorageRepository providerStorageRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final CommandQueue commandQueue;
    private final TaskOrchestrationService taskOrchestrationService;
    private final int taskTimeoutSeconds;
    private final int executionTimeoutSeconds;

    public ProviderStorageDiscoveryService(
            ProviderRepository providerRepository,
            ProviderStorageRepository providerStorageRepository,
            QueueEntryRepository queueEntryRepository,
            CommandQueue commandQueue,
            TaskOrchestrationService taskOrchestrationService,
            @Value("${infron.provider.storage-discovery-task-timeout-seconds:3600}") int taskTimeoutSeconds,
            @Value("${infron.provider.storage-discovery-execution-timeout-seconds:300}") int executionTimeoutSeconds) {
        this.providerRepository = providerRepository;
        this.providerStorageRepository = providerStorageRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.commandQueue = commandQueue;
        this.taskOrchestrationService = taskOrchestrationService;
        this.taskTimeoutSeconds = taskTimeoutSeconds;
        this.executionTimeoutSeconds = executionTimeoutSeconds;
    }

    /**
     * Enqueue storage discovery and return the task id to poll via {@code GET /api/v1/tasks/{taskId}}.
     *
     * @return new job id
     */
    @Transactional
    public UUID enqueueStorageDiscovery(UUID providerId) {
        log.info("Enqueueing storage discovery task for provider {}", providerId);

        ProviderEntity provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));

        String providerType = provider.getType().toString().toLowerCase();
        if (!ORCHESTRATED_DISCOVERY_TYPES.contains(providerType)) {
            throw new IllegalArgumentException("Storage discovery is not supported for provider type: " + providerType);
        }

        TaskCreateRequest taskRequest = new TaskCreateRequest();
        taskRequest.setOperation(JobType.PROVIDER_STORAGE_SYNC);
        taskRequest.setEntityType(EntityType.PROVIDER);
        taskRequest.setEntityId(providerId);
        taskRequest.setTimeoutSeconds(taskTimeoutSeconds);
        taskRequest.setParameters(Map.of("providerType", providerType));
        taskRequest.setMetadata(Map.of(
                "kind", "PROVIDER_STORAGE_SYNC",
                "providerId", providerId.toString()));

        JobEntity job = taskOrchestrationService.createTask(taskRequest);
        UUID jobId = job.getId();

        Instant now = Instant.now();
        int superseded = queueEntryRepository.failPendingByEntityIdAndQueueType(
                providerId,
                ProviderQueueCommands.STORAGE_DISCOVERY,
                "Superseded by newer storage discovery request (jobId=" + jobId + ")",
                now);
        if (superseded > 0) {
            log.info("Superseded {} stale PENDING storage-discovery queue row(s) for provider {}", superseded, providerId);
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "core-services");
        metadata.put(ProviderQueueMetadataKeys.JOB_ID, jobId.toString());
        metadata.put(ProviderQueueMetadataKeys.EXECUTION_TIMEOUT_SECONDS, String.valueOf(executionTimeoutSeconds));

        CommandMessage command = CommandMessage.builder()
                .queueType(ProviderQueueCommands.STORAGE_DISCOVERY)
                .entityType(com.onetattva.infron.api.model.EntityType.PROVIDER)
                .entityId(providerId)
                .payload(Map.of())
                .metadata(metadata)
                .source("core-services")
                .actorType("SYSTEM")
                .actorService("core-services")
                .createdAt(now)
                .correlationId(jobId.toString())
                .build();

        UUID commandId = commandQueue.sendCommand(command);
        log.info("Storage discovery enqueued: providerId={}, jobId={}, commandId={}, executionTimeoutSeconds={}",
                providerId, jobId, commandId, executionTimeoutSeconds);

        return jobId;
    }

    /**
     * Discover storage for all supported providers; each gets its own task.
     *
     * @return stable map of provider id → job id (or null if type skipped / error)
     */
    @Transactional
    public Map<UUID, UUID> enqueueStorageDiscoveryForAllProviders() {
        Map<UUID, UUID> results = new LinkedHashMap<>();
        for (ProviderEntity provider : providerRepository.findAll()) {
            String providerType = provider.getType().toString().toLowerCase();
            if (!ORCHESTRATED_DISCOVERY_TYPES.contains(providerType)) {
                results.put(provider.getId(), null);
                continue;
            }
            try {
                results.put(provider.getId(), enqueueStorageDiscovery(provider.getId()));
            } catch (Exception e) {
                log.error("Failed to enqueue storage discovery for provider {}: {}", provider.getId(), e.getMessage(), e);
                results.put(provider.getId(), null);
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

    public boolean isDiscoverySupported(String providerType) {
        if (providerType == null) {
            return false;
        }
        return ORCHESTRATED_DISCOVERY_TYPES.contains(providerType.toLowerCase());
    }
}
