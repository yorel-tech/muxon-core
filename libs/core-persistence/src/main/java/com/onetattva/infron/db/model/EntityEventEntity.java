package com.onetattva.infron.db.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity for tracking events (audit trail) across all entities.
 * Events provide an immutable history of all actions.
 */
@Entity
@Table(name = "entity_event")
public class EntityEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Entity type (VM, NODE, DATACENTER, TENANT, PROVIDER, USER)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;

    /**
     * Entity ID
     */
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    /**
     * Event type (CREATED, UPDATED, DELETED, STARTED, STOPPED, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    /**
     * Event message
     */
    @Column(name = "message")
    private String message;

    /**
     * Event details (JSONB)
     */
    @Column(name = "details", columnDefinition = "JSONB")
    private String details;

    /**
     * Actor user ID (if action was by a user)
     */
    @Column(name = "actor_user_id")
    private UUID actorUserId;

    /**
     * Actor service name (if action was by a service)
     */
    @Column(name = "actor_service")
    private String actorService;

    /**
     * Actor type (USER, SERVICE, SYSTEM)
     */
    @Column(name = "actor_type", nullable = false)
    private String actorType = "SYSTEM";

    /**
     * When the event occurred
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /**
     * Correlation ID for tracing related operations
     */
    @Column(name = "correlation_id")
    private String correlationId;

    /**
     * Request ID for tracing
     */
    @Column(name = "request_id")
    private String requestId;

    /**
     * Source of the event (api, orchestrator, provider, etc.)
     */
    @Column(name = "source", nullable = false)
    private String source;

    /**
     * Event version
     */
    @Column(name = "version")
    private String version = "1.0";

    /**
     * Additional metadata (JSONB)
     */
    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(UUID actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorService() {
        return actorService;
    }

    public void setActorService(String actorService) {
        this.actorService = actorService;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
