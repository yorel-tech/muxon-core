package com.onetattva.infron.db.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity for tracking async jobs (operations).
 * Jobs represent long-running operations like VM creation, deletion, etc.
 */
@Entity
@Table(name = "job")
public class JobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Job type (VM_CREATE, VM_DELETE, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    private JobType jobType;

    /**
     * Job status (PENDING, RUNNING, COMPLETED, FAILED, CANCELLED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status = JobStatus.PENDING;

    /**
     * Target entity type (VM, NODE, DATACENTER, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_entity_type", nullable = false)
    private EntityType targetEntityType;

    /**
     * Target entity ID
     */
    @Column(name = "target_entity_id", nullable = false)
    private UUID targetEntityId;

    /**
     * Job parameters (JSONB)
     */
    @Column(name = "parameters", columnDefinition = "JSONB")
    private String parameters;

    /**
     * Job result (JSONB)
     */
    @Column(name = "result", columnDefinition = "JSONB")
    private String result;

    /**
     * Provider ID for this job
     */
    @Column(name = "provider_id")
    private UUID providerId;

    /**
     * Provider operation ID (if applicable)
     */
    @Column(name = "provider_operation_id")
    private String providerOperationId;

    /**
     * When the job was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /**
     * When the job started processing
     */
    @Column(name = "started_at")
    private Instant startedAt;

    /**
     * When the job completed
     */
    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Error message if job failed
     */
    @Column(name = "error_message")
    private String errorMessage;

    /**
     * Error details (JSONB)
     */
    @Column(name = "error_details", columnDefinition = "JSONB")
    private String errorDetails;

    /**
     * Number of retry attempts
     */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    /**
     * Maximum retry attempts
     */
    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;

    /**
     * Correlation ID for tracing
     */
    @Column(name = "correlation_id")
    private String correlationId;

    /**
     * Request ID for tracing
     */
    @Column(name = "request_id")
    private String requestId;

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public EntityType getTargetEntityType() {
        return targetEntityType;
    }

    public void setTargetEntityType(EntityType targetEntityType) {
        this.targetEntityType = targetEntityType;
    }

    public UUID getTargetEntityId() {
        return targetEntityId;
    }

    public void setTargetEntityId(UUID targetEntityId) {
        this.targetEntityId = targetEntityId;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public void setProviderId(UUID providerId) {
        this.providerId = providerId;
    }

    public String getProviderOperationId() {
        return providerOperationId;
    }

    public void setProviderOperationId(String providerOperationId) {
        this.providerOperationId = providerOperationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
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
}
