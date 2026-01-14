package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.QueueEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for queue entry operations
 */
public interface QueueEntryRepository extends JpaRepository<QueueEntry, UUID> {

    /**
     * Find pending entries for processing
     */
    @Query("SELECT q FROM QueueEntry q WHERE q.status = 'PENDING' ORDER BY q.createdAt ASC")
    Page<QueueEntry> findPendingEntries(Pageable pageable);

    /**
     * Find entries by entity type and queue type
     */
    @Query("SELECT q FROM QueueEntry q WHERE q.entityType = :entityType AND q.queueType = :queueType ORDER BY q.createdAt ASC")
    Page<QueueEntry> findByEntityTypeAndQueueType(
        @Param("entityType") String entityType,
        @Param("queueType") String queueType,
        Pageable pageable
    );

    /**
     * Find entries by correlation ID
     */
    @Query("SELECT q FROM QueueEntry q WHERE q.correlationId = :correlationId ORDER BY q.createdAt ASC")
    List<QueueEntry> findByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Find entries by entity ID
     */
    @Query("SELECT q FROM QueueEntry q WHERE q.entityId = :entityId ORDER BY q.createdAt ASC")
    List<QueueEntry> findByEntityId(@Param("entityId") UUID entityId);

    /**
     * Count pending entries
     */
    @Query("SELECT COUNT(q) FROM QueueEntry q WHERE q.status = 'PENDING'")
    long countPendingEntries();

    /**
     * Find old pending entries (for stall detection)
     */
    @Query("SELECT q FROM QueueEntry q WHERE q.status = 'PENDING' AND q.createdAt < :cutoff ORDER BY q.createdAt ASC")
    List<QueueEntry> findStalePendingEntries(
        @Param("cutoff") Instant cutoff
    );
}
