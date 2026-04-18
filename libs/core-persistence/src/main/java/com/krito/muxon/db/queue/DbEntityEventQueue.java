package com.krito.muxon.db.queue;

import com.krito.muxon.api.enums.QueueCategory;
import com.krito.muxon.api.enums.QueueStatus;
import com.krito.muxon.api.model.EntityType;
import com.krito.muxon.core.spi.queue.EntityEventMessage;
import com.krito.muxon.core.spi.queue.EntityEventQueue;
import com.krito.muxon.db.model.QueueEntryEntity;
import com.krito.muxon.db.repository.QueueEntryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Database-backed implementation of {@link EntityEventQueue}.
 * Uses the {@code queue_entry} table with {@code ENTITY_EVENT} category.
 */
public class DbEntityEventQueue implements EntityEventQueue {

    private static final String SOURCE = "worker";
    private static final String TASK_ID_KEY = "_taskId";

    private final QueueEntryRepository repository;

    public DbEntityEventQueue(QueueEntryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void publishEntityEvent(EntityType entityType, UUID entityId,
                                   String eventType, UUID taskId,
                                   Map<String, Object> payload) {
        QueueEntryEntity entry = new QueueEntryEntity();
        entry.setQueueType(eventType);
        entry.setQueueCategory(QueueCategory.ENTITY_EVENT);
        entry.setEntityType(com.krito.muxon.api.enums.EntityType.valueOf(entityType.name()));
        entry.setEntityId(entityId);
        entry.setStatus(QueueStatus.PENDING);

        Map<String, Object> enriched = new HashMap<>(payload != null ? payload : Map.of());
        if (taskId != null) {
            enriched.put(TASK_ID_KEY, taskId.toString());
        }
        entry.setPayload(enriched);
        entry.setActorType("WORKER");
        entry.setSource(SOURCE);
        Instant now = Instant.now();
        entry.setCreatedAt(now);
        entry.setUpdatedAt(now);
        repository.save(entry);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<EntityEventMessage> pollEntityEvents(int limit) {
        List<QueueEntryEntity> entries = repository.findPendingByCategoryForUpdate(
                QueueCategory.ENTITY_EVENT, PageRequest.of(0, limit));
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

    private EntityEventMessage toMessage(QueueEntryEntity e) {
        Map<String, Object> payload = e.getPayload() != null ? e.getPayload() : Map.of();
        UUID taskId = null;
        Object rawTaskId = payload.get(TASK_ID_KEY);
        if (rawTaskId != null) {
            try { taskId = UUID.fromString(rawTaskId.toString()); } catch (IllegalArgumentException ignored) {}
        }
        EntityType modelType = EntityType.valueOf(e.getEntityType().name());
        return EntityEventMessage.builder()
                .id(e.getId())
                .entityType(modelType)
                .entityId(e.getEntityId())
                .eventType(e.getQueueType())
                .taskId(taskId)
                .payload(payload)
                .source(e.getSource())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
