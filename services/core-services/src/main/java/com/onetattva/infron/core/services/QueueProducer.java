package com.onetattva.infron.core.services;

import com.onetattva.infron.api.enums.EntityType;
import com.onetattva.infron.api.enums.QueueCategory;
import com.onetattva.infron.api.enums.QueueStatus;
import com.onetattva.infron.db.model.QueueEntryEntity;
import com.onetattva.infron.db.repository.QueueEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service for producing queue entries to the database queue.
 * This replaces the event bus with a database-backed queue system.
 */
@Service
public class QueueProducer {

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    /**
     * Emit a command event to the queue
     */
    @Transactional
    public QueueEntryEntity emitCommand(String queueType, EntityType entityType, UUID entityId,
                                        Map<String, Object> payload, String correlationId, String requestId) {
        return emitQueueEntry(queueType, entityType, entityId, QueueCategory.COMMAND,
                payload, correlationId, requestId);
    }

    /**
     * Emit a status event to the queue
     */
    @Transactional
    public QueueEntryEntity emitStatus(String queueType, EntityType entityType, UUID entityId,
                                       Map<String, Object> payload, String correlationId, String requestId) {
        return emitQueueEntry(queueType, entityType, entityId, QueueCategory.STATUS,
                payload, correlationId, requestId);
    }

    /**
     * Emit an audit event to the queue
     */
    @Transactional
    public QueueEntryEntity emitAudit(String queueType, EntityType entityType, UUID entityId,
                                      Map<String, Object> payload, String correlationId, String requestId) {
        return emitQueueEntry(queueType, entityType, entityId, QueueCategory.AUDIT,
                payload, correlationId, requestId);
    }

    /**
     * Emit a queue entry with actor information
     */
    @Transactional
    public QueueEntryEntity emitWithActor(String queueType, EntityType entityType, UUID entityId,
                                          QueueCategory category, Map<String, Object> payload,
                                          UUID actorUserId, String actorService, String actorType,
                                          String correlationId, String requestId) {

        QueueEntryEntity entry = new QueueEntryEntity();
        entry.setQueueType(queueType);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setQueueCategory(category);
        entry.setStatus(QueueStatus.PENDING);
        entry.setPayload(payload);
        entry.setActorUserId(actorUserId);
        entry.setActorService(actorService);
        entry.setActorType(actorType);
        entry.setSource("core-services");
        entry.setCorrelationId(correlationId);
        entry.setRequestId(requestId);
        entry.setCreatedAt(Instant.now());

        return queueEntryRepository.save(entry);
    }

    /**
     * Internal method to emit a queue entry
     */
    private QueueEntryEntity emitQueueEntry(String queueType, EntityType entityType, UUID entityId,
                                            QueueCategory category, Map<String, Object> payload,
                                            String correlationId, String requestId) {

        QueueEntryEntity entry = new QueueEntryEntity();
        entry.setQueueType(queueType);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setQueueCategory(category);
        entry.setStatus(QueueStatus.PENDING);
        entry.setPayload(payload);
        entry.setActorType("SYSTEM");
        entry.setSource("core-services");
        entry.setCorrelationId(correlationId);
        entry.setRequestId(requestId);
        entry.setCreatedAt(Instant.now());

        return queueEntryRepository.save(entry);
    }

    /**
     * Mark a queue entry as completed
     */
    @Transactional
    public void markCompleted(UUID entryId) {
        queueEntryRepository.findById(entryId).ifPresent(entry -> {
            entry.setStatus(QueueStatus.COMPLETED);
            entry.setProcessedAt(Instant.now());
            queueEntryRepository.save(entry);
        });
    }

    /**
     * Mark a queue entry as failed
     */
    @Transactional
    public void markFailed(UUID entryId, String errorMessage, Map<String, String> errorDetails) {
        queueEntryRepository.findById(entryId).ifPresent(entry -> {
            entry.setStatus(QueueStatus.FAILED);
            entry.setProcessedAt(Instant.now());
            entry.setErrorMessage(errorMessage);
            // Note: errorDetails could be stored in metadata if needed
            if (errorDetails != null) {
                entry.setMetadata(errorDetails);
            }
            queueEntryRepository.save(entry);
        });
    }
}
