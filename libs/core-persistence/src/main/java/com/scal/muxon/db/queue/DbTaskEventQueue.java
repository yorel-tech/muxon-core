package com.scal.muxon.db.queue;

import com.scal.muxon.api.enums.EntityType;
import com.scal.muxon.api.enums.QueueCategory;
import com.scal.muxon.api.enums.QueueStatus;
import com.scal.muxon.spi.queue.TaskEventMessage;
import com.scal.muxon.spi.queue.TaskEventQueue;
import com.scal.muxon.db.model.QueueEntryEntity;
import com.scal.muxon.db.repository.QueueEntryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Database-backed implementation of {@link TaskEventQueue}.
 * Uses the {@code orchestrator_queue} table with {@code TASK_EVENT} category.
 * The entity_id column stores the task ID for this category.
 */
public class DbTaskEventQueue implements TaskEventQueue {

    private static final String SOURCE = "worker";

    private final QueueEntryRepository repository;

    public DbTaskEventQueue(QueueEntryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void publishTaskEvent(UUID taskId, String eventType, Map<String, Object> payload) {
        QueueEntryEntity entry = new QueueEntryEntity();
        entry.setQueueType(eventType);
        entry.setQueueCategory(QueueCategory.TASK_EVENT);
        // Reuse entity_id to store taskId; entity_type is a required column — use a sentinel
        entry.setEntityType(EntityType.valueOf("VM")); // placeholder; actual lookup is by taskId in payload
        entry.setEntityId(taskId);
        entry.setStatus(QueueStatus.PENDING);
        entry.setPayload(payload != null ? payload : Map.of());
        entry.setActorType("WORKER");
        entry.setSource(SOURCE);
        Instant now = Instant.now();
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        repository.save(entry);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<TaskEventMessage> pollTaskEvents(int limit) {
        List<QueueEntryEntity> entries = repository.findPendingByCategoryForUpdate(
                QueueCategory.TASK_EVENT, PageRequest.of(0, limit));
        Instant now = Instant.now();
        for (QueueEntryEntity e : entries) {
            e.setStatus(QueueStatus.PROCESSING);
            e.setProcessedAt(now);
            e.setUpdatedAt(now);
        }
        if (!entries.isEmpty()) {
            repository.saveAll(entries);
        }
        return entries.stream().map(this::toMessage).toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(UUID eventId) {
        repository.markCompleted(eventId, Instant.now());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID eventId, String error) {
        repository.markFailed(eventId, error, Instant.now());
    }

    private TaskEventMessage toMessage(QueueEntryEntity e) {
        return TaskEventMessage.builder()
                .id(e.getId())
                .taskId(e.getEntityId())   // entity_id holds the taskId
                .eventType(e.getQueueType())
                .payload(e.getPayload() != null ? e.getPayload() : Map.of())
                .source(e.getSource())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
