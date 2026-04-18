package com.krito.muxon.core.services;

import com.krito.muxon.api.dto.TaskCreateRequest;
import com.krito.muxon.api.enums.EntityType;
import com.krito.muxon.api.enums.JobType;
import com.krito.muxon.db.model.ProviderEntity;
import com.krito.muxon.db.repository.ProviderRepository;
import com.krito.muxon.db.repository.QueueEntryRepository;
import com.krito.muxon.core.services.task.TaskOrchestrationService;
import com.krito.muxon.core.spi.queue.CommandMessage;
import com.krito.muxon.core.spi.queue.CommandQueue;
import com.krito.muxon.core.spi.queue.ProviderQueueCommands;
import com.krito.muxon.core.spi.queue.ProviderQueueMetadataKeys;
import com.krito.muxon.db.model.JobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Enqueues full provider inventory sync (capabilities where applicable, node cluster + nodes, storage).
 */
@Service
public class ProviderInventorySyncService {

    private static final Logger log = LoggerFactory.getLogger(ProviderInventorySyncService.class);

    private static final Set<String> SUPPORTED_TYPES = Set.of("libvirt", "proxmox");

    private final ProviderRepository providerRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final CommandQueue commandQueue;
    private final TaskOrchestrationService taskOrchestrationService;
    private final int taskTimeoutSeconds;
    private final int executionTimeoutSeconds;

    public ProviderInventorySyncService(
            ProviderRepository providerRepository,
            QueueEntryRepository queueEntryRepository,
            CommandQueue commandQueue,
            TaskOrchestrationService taskOrchestrationService,
            @Value("${muxon.provider.inventory-sync-task-timeout-seconds:7200}") int taskTimeoutSeconds,
            @Value("${muxon.provider.inventory-sync-execution-timeout-seconds:900}") int executionTimeoutSeconds) {
        this.providerRepository = providerRepository;
        this.queueEntryRepository = queueEntryRepository;
        this.commandQueue = commandQueue;
        this.taskOrchestrationService = taskOrchestrationService;
        this.taskTimeoutSeconds = taskTimeoutSeconds;
        this.executionTimeoutSeconds = executionTimeoutSeconds;
    }

    /**
     * @return new job id for {@code GET /api/v1/tasks/{taskId}}
     */
    @Transactional
    public UUID enqueueInventorySync(UUID providerId) {
        log.info("Enqueueing provider inventory sync for provider {}", providerId);

        ProviderEntity provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));

        String providerType = provider.getType().toString().toLowerCase();
        if (!SUPPORTED_TYPES.contains(providerType)) {
            throw new IllegalArgumentException("Provider inventory sync is not supported for type: " + providerType);
        }

        TaskCreateRequest taskRequest = new TaskCreateRequest();
        taskRequest.setOperation(JobType.PROVIDER_SYNC);
        taskRequest.setEntityType(EntityType.PROVIDER);
        taskRequest.setEntityId(providerId);
        taskRequest.setTimeoutSeconds(taskTimeoutSeconds);
        taskRequest.setParameters(Map.of("providerType", providerType));
        taskRequest.setMetadata(Map.of(
                "kind", "PROVIDER_INVENTORY_SYNC",
                "providerId", providerId.toString()));

        JobEntity job = taskOrchestrationService.createTask(taskRequest);
        UUID jobId = job.getId();

        Instant now = Instant.now();
        int superseded = queueEntryRepository.failPendingByEntityIdAndQueueType(
                providerId,
                ProviderQueueCommands.INVENTORY_SYNC,
                "Superseded by newer provider inventory sync request (jobId=" + jobId + ")",
                now);
        if (superseded > 0) {
            log.info("Superseded {} stale PENDING inventory-sync queue row(s) for provider {}", superseded, providerId);
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "core-services");
        metadata.put(ProviderQueueMetadataKeys.JOB_ID, jobId.toString());
        metadata.put(ProviderQueueMetadataKeys.EXECUTION_TIMEOUT_SECONDS, String.valueOf(executionTimeoutSeconds));

        CommandMessage command = CommandMessage.builder()
                .queueType(ProviderQueueCommands.INVENTORY_SYNC)
                .entityType(com.krito.muxon.api.model.EntityType.PROVIDER)
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
        log.info("Provider inventory sync enqueued: providerId={}, jobId={}, commandId={}, executionTimeoutSeconds={}",
                providerId, jobId, commandId, executionTimeoutSeconds);

        return jobId;
    }
}
