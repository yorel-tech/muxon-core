package com.onetattva.infron.core.services;

import com.onetattva.infron.db.model.EntityType;
import com.onetattva.infron.db.model.QueueEntry;
import com.onetattva.infron.db.model.QueueStatus;
import com.onetattva.infron.db.repository.QueueEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for consuming queue entries from the database queue.
 * The orchestrator polls this service to get pending entries.
 */
@Service
public class QueueConsumer {

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    /**
     * Poll for pending queue entries
     * 
     * @param entityType The entity type to filter by (null for all)
     * @param queueType The queue type to filter by (null for all)
     * @param limit Maximum number of entries to return
     * @return List of pending queue entries
     */
    @Transactional
    public List<QueueEntry> poll(EntityType entityType, String queueType, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        Page<QueueEntry> page;
        
        if (entityType != null && queueType != null) {
            page = queueEntryRepository.findPendingByEntityTypeAndQueueType(
                    entityType, queueType, pageRequest);
        } else if (entityType != null) {
            page = queueEntryRepository.findPendingByEntityType(entityType, pageRequest);
        } else if (queueType != null) {
            page = queueEntryRepository.findPendingByQueueType(queueType, pageRequest);
        } else {
            page = queueEntryRepository.findPendingEntries(pageRequest);
        }
        
        // Mark entries as processing
        List<QueueEntry> entries = page.getContent();
        for (QueueEntry entry : entries) {
            entry.setStatus(QueueStatus.PROCESSING);
            entry.setProcessedAt(Instant.now());
        }
        
        if (!entries.isEmpty()) {
            queueEntryRepository.saveAll(entries);
        }
        
        return entries;
    }

    /**
     * Poll for pending entries by correlation ID
     * 
     * @param correlationId The correlation ID to filter by
     * @param limit Maximum number of entries to return
     * @return List of pending queue entries
     */
    @Transactional
    public List<QueueEntry> pollByCorrelationId(String correlationId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        Page<QueueEntry> page = queueEntryRepository.findPendingByCorrelationId(
                correlationId, pageRequest);
        
        // Mark entries as processing
        List<QueueEntry> entries = page.getContent();
        for (QueueEntry entry : entries) {
            entry.setStatus(QueueStatus.PROCESSING);
            entry.setProcessedAt(Instant.now());
        }
        
        if (!entries.isEmpty()) {
            queueEntryRepository.saveAll(entries);
        }
        
        return entries;
    }

    /**
     * Mark a queue entry as completed
     * 
     * @param entryId The queue entry ID
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
     * 
     * @param entryId The queue entry ID
     * @param errorMessage Error message
     */
    @Transactional
    public void markFailed(UUID entryId, String errorMessage) {
        queueEntryRepository.findById(entryId).ifPresent(entry -> {
            entry.setStatus(QueueStatus.FAILED);
            entry.setProcessedAt(Instant.now());
            entry.setErrorMessage(errorMessage);
            queueEntryRepository.save(entry);
        });
    }

    /**
     * Get count of pending entries by entity type
     * 
     * @param entityType The entity type
     * @return Count of pending entries
     */
    public long getPendingCount(EntityType entityType) {
        return queueEntryRepository.countPendingByEntityType(entityType);
    }

    /**
     * Get count of processing entries by entity type
     * 
     * @param entityType The entity type
     * @return Count of processing entries
     */
    public long getProcessingCount(EntityType entityType) {
        return queueEntryRepository.countProcessingByEntityType(entityType);
    }

    /**
     * Get entries that have been stalled (processing for too long)
     * 
     * @param stallThresholdMinutes Threshold in minutes
     * @return List of stalled entries
     */
    @Transactional
    public List<QueueEntry> getStalledEntries(int stallThresholdMinutes) {
        Instant threshold = Instant.now().minusSeconds(stallThresholdMinutes * 60L);
        List<QueueEntry> stalled = queueEntryRepository.findStalledProcessingEntries(threshold);
        
        // Reset stalled entries to pending for retry
        for (QueueEntry entry : stalled) {
            entry.setStatus(QueueStatus.PENDING);
            entry.setProcessedAt(null);
        }
        
        if (!stalled.isEmpty()) {
            queueEntryRepository.saveAll(stalled);
        }
        
        return stalled;
    }

    /**
     * Get queue entry by ID
     * 
     * @param entryId The queue entry ID
     * @return The queue entry
     */
    public QueueEntry getEntry(UUID entryId) {
        return queueEntryRepository.findById(entryId).orElse(null);
    }

    /**
     * Get entries for a specific entity
     * 
     * @param entityType The entity type
     * @param entityId The entity ID
     * @return List of queue entries
     */
    public List<QueueEntry> getEntriesForEntity(EntityType entityType, UUID entityId) {
        return queueEntryRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                entityType, entityId);
    }
}
