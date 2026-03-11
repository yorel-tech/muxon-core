package com.onetattva.infron.db.queue;

import com.onetattva.infron.api.enums.QueueCategory;
import com.onetattva.infron.api.enums.QueueStatus;
import com.onetattva.infron.core.spi.queue.CommandMessage;
import com.onetattva.infron.db.model.QueueEntry;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Maps between QueueEntry (JPA) and CommandMessage (transport-agnostic DTO).
 */
public final class QueueEntryMapper {

    private static final String DEFAULT_VERSION = "1.0";

    private QueueEntryMapper() {
    }

    public static CommandMessage toCommandMessage(QueueEntry entry) {
        if (entry == null) {
            return null;
        }
        return CommandMessage.builder()
            .id(entry.getId())
            .queueType(entry.getQueueType())
            .entityType(entry.getEntityType())
            .entityId(entry.getEntityId())
            .payload(entry.getPayload() != null ? entry.getPayload() : Map.of())
            .metadata(entry.getMetadata() != null ? entry.getMetadata() : Map.of())
            .source(entry.getSource())
            .actorType(entry.getActorType())
            .actorUserId(entry.getActorUserId())
            .actorService(entry.getActorService())
            .createdAt(entry.getCreatedAt() != null ? entry.getCreatedAt() : Instant.now())
            .requestId(entry.getRequestId())
            .correlationId(entry.getCorrelationId())
            .build();
    }

    public static QueueEntry toQueueEntry(CommandMessage msg) {
        if (msg == null) {
            return null;
        }
        QueueEntry entry = new QueueEntry();
        if (msg.id() != null) {
            entry.setId(msg.id());
        }
        entry.setQueueType(msg.queueType());
        entry.setEntityType(msg.entityType());
        entry.setEntityId(msg.entityId());
        entry.setQueueCategory(QueueCategory.COMMAND);
        entry.setStatus(QueueStatus.PENDING);
        entry.setPayload(msg.payload());
        entry.setMetadata(msg.metadata());
        entry.setSource(msg.source());
        entry.setActorType(msg.actorType());
        entry.setActorUserId(msg.actorUserId());
        entry.setActorService(msg.actorService());
        entry.setCreatedAt(msg.createdAt() != null ? msg.createdAt() : Instant.now());
        entry.setRequestId(msg.requestId());
        entry.setCorrelationId(msg.correlationId());
        entry.setVersion(DEFAULT_VERSION);
        return entry;
    }
}
