package com.krito.muxon.db.queue;

import com.krito.muxon.api.model.EntityType;
import com.krito.muxon.api.enums.QueueCategory;
import com.krito.muxon.api.enums.QueueStatus;
import com.krito.muxon.core.spi.queue.EventPublisher;
import com.krito.muxon.db.model.QueueEntryEntity;
import com.krito.muxon.db.repository.QueueEntryRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Database-backed implementation of EventPublisher.
 * Writes status/audit events as rows in the queue_entry table.
 */
public class DbEventPublisher implements EventPublisher {

    private static final String DEFAULT_VERSION = "1.0";

    private final QueueEntryRepository repository;

    public DbEventPublisher(QueueEntryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void publishEvent(EntityType entityType, UUID entityId, String eventType, Map<String, Object> payload) {
        QueueEntryEntity entry = new QueueEntryEntity();
        entry.setQueueType(eventType);
        entry.setEntityType(com.krito.muxon.api.enums.EntityType.valueOf(entityType.name()));
        entry.setEntityId(entityId);
        entry.setQueueCategory(QueueCategory.STATUS);
        entry.setStatus(QueueStatus.PENDING);
        entry.setPayload(payload != null ? payload : Map.of());
        entry.setActorType("SYSTEM");
        entry.setSource("orchestrator");
        entry.setCreatedAt(Instant.now());
        entry.setUpdatedAt(Instant.now());
        entry.setVersion(DEFAULT_VERSION);
        repository.save(entry);
    }

    private com.krito.muxon.api.enums.EntityType convertToEntityTypeEnum(EntityType modelType) {
        return com.krito.muxon.api.enums.EntityType.valueOf(modelType.name());
    }
}
