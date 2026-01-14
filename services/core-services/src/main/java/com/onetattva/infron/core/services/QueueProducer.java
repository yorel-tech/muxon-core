package com.onetattva.infron.core.services;

import com.onetattva.infron.db.model.*;
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
    public QueueEntry emitCommand(String queueType, EntityType entityType, UUID entityId, 
            Map<String, Object> payload, String correlationId, String requestId) {
        return emitQueueEntry(queueType, entityType, entityId, QueueCategory.COMMAND, 
                payload, correlationId, requestId);
    }

    /**
     * Emit a status event to the queue
     */
    @Transactional
    public QueueEntry emitStatus(String queueType, EntityType entityType, UUID entityId,
            Map<String, Object> payload, String correlationId, String requestId) {
        return emitQueueEntry(queueType, entityType, entityId, QueueCategory.STATUS,
                payload, correlationId, requestId);
    }

    /**
     * Emit an audit event to the queue
     */
    @Transactional
    public QueueEntry emitAudit(String queueType, EntityType entityType, UUID entityId,
            Map<String, Object> payload, String correlationId, String requestId) {
        return emitQueueEntry(queueType, entityType, entityId, QueueCategory.AUDIT,
                payload, correlationId, requestId);
    }

    /**
     * Emit a queue entry with actor information
     */
    @Transactional
    public QueueEntry emitWithActor(String queueType, EntityType entityType, UUID entityId,
            QueueCategory category, Map<String, Object> payload,
            UUID actorUserId, String actorService, String actorType,
            String correlationId, String requestId) {
        
        QueueEntry entry = QueueEntry.builder()
                .queueType(queueType)
                .entityType(entityType)
                .entityId(entityId)
                .queueCategory(category)
                .status(QueueStatus.PENDING)
                .payload(payload)
                .actorUserId(actorUserId)
                .actorService(actorService)
                .actorType(actorType)
                .source("core-services")
                .correlationId(correlationId)
                .requestId(requestId)
                .createdAt(Instant.now())
                .build();

        return queueEntryRepository.save(entry);
    }

    /**
     * Internal method to emit a queue entry
     */
    private QueueEntry emitQueueEntry(String queueType, EntityType entityType, UUID entityId,
            QueueCategory category, Map<String, Object> payload,
            String correlationId, String requestId) {
        
        QueueEntry entry = QueueEntry.builder()
                .queueType(queueType)
                .entityType(entityType)
                .entityId(entityId)
                .queueCategory(category)
                .status(QueueStatus.PENDING)
                .payload(payload)
                .actorType("SYSTEM")
                .source("core-services")
                .correlationId(correlationId)
                .requestId(requestId)
                .createdAt(Instant.now())
                .build();

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
    public void markFailed(UUID entryId, String errorMessage, Map<String, Object> errorDetails) {
        queueEntryRepository.findById(entryId).ifPresent(entry -> {
            entry.setStatus(QueueStatus.FAILED);
            entry.setProcessedAt(Instant.now());
            entry.setErrorMessage(errorMessage);
            entry.setErrorDetails(errorDetails);
            queueEntryRepository.save(entry);
        });
    }
}
