package com.onetattva.infron.db.model;

import com.onetattva.infron.db.enums.EntityType;
import com.onetattva.infron.db.enums.QueueCategory;
import com.onetattva.infron.db.enums.QueueStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a queue entry for database-based async communication
 */
@Entity
@Table(name = "queue_entry")
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Queue identification
    @Column(name = "queue_type", nullable = false)
    private String queueType;

    @Enumerated(EnumType.STRING)
    @Column(name = "queue_category", nullable = false)
    private QueueCategory queueCategory;

    @Column(name = "entity_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private QueueStatus status;

    // Queue content
    @Column(name = "payload", nullable = false, columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;

    // Actor information
    @Column(name = "actor_user_id", nullable = true)
    private UUID actorUserId;

    @Column(name = "actor_service", nullable = true)
    private String actorService;

    @Column(name = "actor_type", nullable = false)
    private String actorType;

    // Timing and correlation
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at", nullable = true)
    private Instant processedAt;

    @Column(name = "correlation_id", nullable = true)
    private String correlationId;

    @Column(name = "request_id", nullable = true)
    private String requestId;

    // Metadata
    @Column(name = "source", nullable = false)
    private String source;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "metadata", nullable = true)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> metadata;

    // Audit fields
    @Column(name = "created_by", nullable = true)
    private UUID createdBy;

    @Column(name = "updated_by", nullable = true)
    private UUID updatedBy;

    @Column(name = "error_message", nullable = true)
    private String errorMessage;

    // Constructors
    public QueueEntry() {
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getQueueType() {
        return queueType;
    }

    public void setQueueType(String queueType) {
        this.queueType = queueType;
    }

    public QueueCategory getQueueCategory() {
        return queueCategory;
    }

    public void setQueueCategory(QueueCategory queueCategory) {
        this.queueCategory = queueCategory;
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

    public QueueStatus getStatus() {
        return status;
    }

    public void setStatus(QueueStatus status) {
        this.status = status;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
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

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
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

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
