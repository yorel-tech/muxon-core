package com.scal.muxon.db.repository;

import com.scal.muxon.api.enums.EntityType;
import com.scal.muxon.api.enums.QueueCategory;
import com.scal.muxon.api.enums.QueueStatus;
import com.scal.muxon.db.model.QueueEntryEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for queue entry operations
 */
@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntryEntity, UUID> {

    /**
     * Find pending entries for processing
     */
    @Query("SELECT q FROM QueueEntryEntity q WHERE q.status = 'PENDING' ORDER BY q.createdAt ASC")
    Page<QueueEntryEntity> findPendingEntries(Pageable pageable);

    /**
     * Find entries by entity type and queue type
     */
    @Query("SELECT q FROM QueueEntryEntity q WHERE q.entityType = :entityType AND q.queueType = :queueType ORDER BY q.createdAt ASC")
    Page<QueueEntryEntity> findByEntityTypeAndQueueType(
        @Param("entityType") String entityType,
        @Param("queueType") String queueType,
        Pageable pageable
    );

    /**
     * Find entries by correlation ID
     */
    @Query("SELECT q FROM QueueEntryEntity q WHERE q.correlationId = :correlationId ORDER BY q.createdAt ASC")
    List<QueueEntryEntity> findByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Minimal status/error projection for timeout diagnostics.
     */
    @Query("SELECT q.status AS status, q.errorMessage AS errorMessage FROM QueueEntryEntity q WHERE q.id = :id")
    Optional<QueueStatusErrorProjection> findStatusAndErrorById(@Param("id") UUID id);

    /**
     * Find entries by entity ID
     */
    @Query("SELECT q FROM QueueEntryEntity q WHERE q.entityId = :entityId ORDER BY q.createdAt ASC")
    List<QueueEntryEntity> findByEntityId(@Param("entityId") UUID entityId);

    /**
     * Count pending entries
     */
    @Query("SELECT COUNT(q) FROM QueueEntryEntity q WHERE q.status = 'PENDING'")
    long countPendingEntries();

    /**
     * Find old pending entries (for stall detection)
     */
    @Query("SELECT q FROM QueueEntryEntity q WHERE q.status = 'PENDING' AND q.createdAt < :cutoff ORDER BY q.createdAt ASC")
    List<QueueEntryEntity> findStalePendingEntries(
        @Param("cutoff") Instant cutoff
    );

    long countPendingByEntityType(EntityType entityType);

    long countProcessingByEntityType(EntityType entityType);

    /**
     * Poll for pending entries by entity type
     */
    @Query("SELECT q FROM QueueEntryEntity q WHERE q.entityType = :entityType AND q.status = 'PENDING' ORDER BY q.createdAt ASC")
    List<QueueEntryEntity> poll(
        @Param("entityType") EntityType entityType,
        @Param("queueType") String queueType,
        @Param("limit") int limit
    );

    /**
     * Find pending entries by entity type and lock for update (claim). Caller must set status to PROCESSING and save.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM QueueEntryEntity q WHERE q.entityType = :entityType AND q.status = :status ORDER BY q.createdAt ASC")
    List<QueueEntryEntity> findPendingByEntityTypeForUpdate(
        @Param("entityType") EntityType entityType,
        @Param("status") QueueStatus status,
        Pageable pageable
    );

    /**
     * Get stalled entries (old PENDING never claimed) older than threshold
     */
    @Query("SELECT q FROM QueueEntryEntity q WHERE q.status = 'PENDING' AND q.createdAt < :cutoff ORDER BY q.createdAt ASC")
    List<QueueEntryEntity> getStalledEntries(
        @Param("cutoff") Instant cutoff
    );

    /**
     * Find entries stuck in PROCESSING (claimed but not completed) older than cutoff.
     * Used for stall reset so they can be retried.
     */
    @Query("SELECT q FROM QueueEntryEntity q WHERE q.status = 'PROCESSING' AND q.processedAt < :cutoff ORDER BY q.processedAt ASC")
    List<QueueEntryEntity> findStaleProcessingEntries(@Param("cutoff") Instant cutoff);

    /**
     * Poll for pending entries by queue category with pessimistic lock (claim).
     * Used by TaskEventQueue and EntityEventQueue DB implementations.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM QueueEntryEntity q WHERE q.queueCategory = :category AND q.status = 'PENDING' ORDER BY q.createdAt ASC")
    List<QueueEntryEntity> findPendingByCategoryForUpdate(
            @Param("category") QueueCategory category,
            Pageable pageable);

    /**
     * Fail all pending queue rows for a given entity and command type (e.g. supersede stale work).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE QueueEntryEntity q SET q.status = 'FAILED', q.errorMessage = :errorMessage, q.processedAt = :now, q.updatedAt = :now WHERE q.entityId = :entityId AND q.queueType = :queueType AND q.status = 'PENDING'")
    int failPendingByEntityIdAndQueueType(
            @Param("entityId") UUID entityId,
            @Param("queueType") String queueType,
            @Param("errorMessage") String errorMessage,
            @Param("now") Instant now);

    /**
     * Mark an entry as failed with error message
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE QueueEntryEntity q SET q.status = 'FAILED', q.errorMessage = :errorMessage, q.processedAt = :processedAt, q.updatedAt = :processedAt WHERE q.id = :id")
    int markFailed(
        @Param("id") UUID id,
        @Param("errorMessage") String errorMessage,
        @Param("processedAt") Instant processedAt
    );

    /**
     * Mark an entry as completed
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE QueueEntryEntity q SET q.status = 'COMPLETED', q.processedAt = :processedAt, q.updatedAt = :processedAt WHERE q.id = :id")
    int markCompleted(
        @Param("id") UUID id,
        @Param("processedAt") Instant processedAt
    );

    interface QueueStatusErrorProjection {
        QueueStatus getStatus();
        String getErrorMessage();
    }
}
