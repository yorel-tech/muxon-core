package com.onetattva.infron.db.queue;

import com.onetattva.infron.api.enums.EntityType;
import com.onetattva.infron.api.enums.QueueCategory;
import com.onetattva.infron.api.enums.QueueStatus;
import com.onetattva.infron.core.spi.queue.EventPublisher;
import com.onetattva.infron.db.model.QueueEntryEntity;
import com.onetattva.infron.db.repository.QueueEntryRepository;
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
        entry.setEntityType(entityType);
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
}
