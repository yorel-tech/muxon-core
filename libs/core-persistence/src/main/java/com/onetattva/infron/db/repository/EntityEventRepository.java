package com.onetattva.infron.db.repository;

import com.onetattva.infron.api.enums.EntityType;
import com.onetattva.infron.api.enums.EventType;
import com.onetattva.infron.db.model.EntityEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for EntityEvent entity (audit trail).
 */
@Repository
public interface EntityEventRepository extends JpaRepository<EntityEventEntity, UUID> {

    /**
     * Find events by entity
     */
    Page<EntityEventEntity> findByEntityTypeAndEntityId(
            EntityType entityType, UUID entityId, Pageable pageable);

    /**
     * Find events by entity type
     */
    Page<EntityEventEntity> findByEntityType(EntityType entityType, Pageable pageable);

    /**
     * Find events by event type
     */
    Page<EntityEventEntity> findByEventType(EventType eventType, Pageable pageable);

    /**
     * Find events by correlation ID
     */
    List<EntityEventEntity> findByCorrelationId(String correlationId);

    /**
     * Find events by request ID
     */
    List<EntityEventEntity> findByRequestId(String requestId);

    /**
     * Find events by actor user ID
     */
    Page<EntityEventEntity> findByActorUserId(UUID actorUserId, Pageable pageable);

    /**
     * Find events by actor service
     */
    Page<EntityEventEntity> findByActorService(String actorService, Pageable pageable);

    /**
     * Find events created after a certain time
     */
    @Query("SELECT e FROM EntityEventEntity e WHERE e.createdAt > :afterTime ORDER BY e.createdAt DESC")
    List<EntityEventEntity> findEventsAfter(@Param("afterTime") Instant afterTime);

    /**
     * Find events created within a time range
     */
    @Query("SELECT e FROM EntityEventEntity e WHERE e.createdAt BETWEEN :startTime AND :endTime ORDER BY e.createdAt DESC")
    List<EntityEventEntity> findEventsBetween(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Count events by entity
     */
    long countByEntityTypeAndEntityId(EntityType entityType, UUID entityId);

    /**
     * Count events by entity type
     */
    long countByEntityType(EntityType entityType);

    /**
     * Count events by event type
     */
    long countByEventType(EventType eventType);

    /**
     * Find recent events
     */
    @Query("SELECT e FROM EntityEventEntity e ORDER BY e.createdAt DESC")
    List<EntityEventEntity> findRecentEvents(Pageable pageable);

    /**
     * Find events by source
     */
    Page<EntityEventEntity> findBySource(String source, Pageable pageable);
}
