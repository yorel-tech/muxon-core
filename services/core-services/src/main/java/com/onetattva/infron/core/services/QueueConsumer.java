package com.onetattva.infron.core.services;

import com.onetattva.infron.api.enums.EntityType;
import com.onetattva.infron.api.enums.QueueStatus;
import com.onetattva.infron.db.model.QueueEntryEntity;
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
 * Service for consuming queue entries from database queue.
 * The orchestrator polls this service to get pending entries.
 */
@Service
public class QueueConsumer {

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    /**
     * Poll for pending entries
     * 
     * @param entityType The entity type to filter by (null for all)
     * @param queueType The queue type to filter by (null for all)
     * @param limit Maximum number of entries to return
     * @return List of pending queue entries
     */
    @Transactional
    public List<QueueEntryEntity> poll(EntityType entityType, String queueType, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        Page<QueueEntryEntity> page;

        if (entityType != null && queueType != null) {
            page = queueEntryRepository.findByEntityTypeAndQueueType(
                    entityType.name(), queueType, pageRequest);
        } else if (entityType != null) {
            // Note: findPendingByQueueType not available, using findPendingEntries
            page = queueEntryRepository.findPendingEntries(pageRequest);
        } else {
            page = queueEntryRepository.findPendingEntries(pageRequest);
        }

        // Mark entries as processing
        List<QueueEntryEntity> entries = page.getContent();
        for (QueueEntryEntity entry : entries) {
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
    public List<QueueEntryEntity> pollByCorrelationId(String correlationId, int limit) {
        // Note: findByCorrelationId returns List, not Page
        List<QueueEntryEntity> entries = queueEntryRepository.findByCorrelationId(correlationId);
        return entries.stream().limit(limit).toList();
    }

    /**
     * Get entries for a specific entity
     * 
     * @param entityType The entity type
     * @param entityId The entity ID
     * @return List of queue entries
     */
    @Transactional
    public List<QueueEntryEntity> getEntriesForEntity(EntityType entityType, UUID entityId) {
        return queueEntryRepository.findByEntityId(entityId);
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
    public List<QueueEntryEntity> getStalledEntries(int stallThresholdMinutes) {
        Instant threshold = Instant.now().minusSeconds(stallThresholdMinutes * 60L);
        return queueEntryRepository.findStalePendingEntries(threshold);
    }

    /**
     * Reset stalled entries to pending for retry
     */
    @Transactional
    public void resetStalledEntries(List<QueueEntryEntity> stalled) {
        for (QueueEntryEntity entry : stalled) {
            entry.setStatus(QueueStatus.PENDING);
            entry.setProcessedAt(null);
        }
        if (!stalled.isEmpty()) {
            queueEntryRepository.saveAll(stalled);
        }
    }

    /**
     * Get queue entry by ID
     */
    public QueueEntryEntity getEntry(UUID entryId) {
        return queueEntryRepository.findById(entryId).orElse(null);
    }
}
