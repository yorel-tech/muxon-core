/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.db.model;

import com.yorel.muxon.api.enums.EntityType;
import com.yorel.muxon.api.enums.JobStatus;
import com.yorel.muxon.api.enums.JobType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity for tracking async jobs (operations). Jobs represent long-running operations like VM
 * creation, deletion, etc.
 */
@Entity
@Table(name = "jobs")
public class JobEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  /** Job type (VM_CREATE, VM_DELETE, etc.) */
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "job_type", nullable = false)
  private JobType jobType;

  /** Job status (PENDING, RUNNING, COMPLETED, FAILED, CANCELLED) */
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "status", nullable = false)
  private JobStatus status = JobStatus.PENDING;

  /** Target entity type (VM, NODE, DATACENTER, etc.) */
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "target_entity_type", nullable = false)
  private EntityType targetEntityType;

  /** Target entity ID */
  @Column(name = "target_entity_id", nullable = false)
  private UUID targetEntityId;

  /** Job parameters (JSONB) */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "parameters", columnDefinition = "JSONB")
  private String parameters;

  /** Job result (JSONB) */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "result", columnDefinition = "JSONB")
  private String result;

  /** Provider ID for this job */
  @Column(name = "provider_id")
  private UUID providerId;

  /** Provider operation ID (if applicable) */
  @Column(name = "provider_operation_id")
  private String providerOperationId;

  /** When the job was created */
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  /** When the job started processing */
  @Column(name = "started_at")
  private Instant startedAt;

  /** When the job completed */
  @Column(name = "completed_at")
  private Instant completedAt;

  /** Error message if job failed */
  @Column(name = "error_message")
  private String errorMessage;

  /** Error details (JSONB) */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "error_details", columnDefinition = "JSONB")
  private String errorDetails;

  /** Number of retry attempts */
  @Column(name = "retry_count", nullable = false)
  private Integer retryCount = 0;

  /** Maximum retry attempts */
  @Column(name = "max_retries", nullable = false)
  private Integer maxRetries = 3;

  /** Correlation ID for tracing */
  @Column(name = "correlation_id")
  private String correlationId;

  /** Request ID for tracing */
  @Column(name = "request_id")
  private String requestId;

  /** Current step description */
  @Column(name = "current_step")
  private String currentStep;

  /** Total number of steps */
  @Column(name = "total_steps")
  private Integer totalSteps;

  /** Progress percentage (0-100) */
  @Column(name = "progress_percentage")
  private Integer progressPercentage = 0;

  /** When the job should timeout */
  @Column(name = "timeout_at")
  private Instant timeoutAt;

  /** Last heartbeat timestamp */
  @Column(name = "last_heartbeat_at")
  private Instant lastHeartbeatAt;

  /** Cancellation requested flag */
  @Column(name = "cancellation_requested")
  private Boolean cancellationRequested = false;

  /** Cancellation reason */
  @Column(name = "cancellation_reason")
  private String cancellationReason;

  /** Additional metadata (JSONB) */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "JSONB")
  private String metadata;

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

  public String getCurrentStep() {
    return currentStep;
  }

  public void setCurrentStep(String currentStep) {
    this.currentStep = currentStep;
  }

  public Integer getTotalSteps() {
    return totalSteps;
  }

  public void setTotalSteps(Integer totalSteps) {
    this.totalSteps = totalSteps;
  }

  public Integer getProgressPercentage() {
    return progressPercentage;
  }

  public void setProgressPercentage(Integer progressPercentage) {
    this.progressPercentage = progressPercentage;
  }

  public Instant getTimeoutAt() {
    return timeoutAt;
  }

  public void setTimeoutAt(Instant timeoutAt) {
    this.timeoutAt = timeoutAt;
  }

  public Instant getLastHeartbeatAt() {
    return lastHeartbeatAt;
  }

  public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
    this.lastHeartbeatAt = lastHeartbeatAt;
  }

  public Boolean getCancellationRequested() {
    return cancellationRequested;
  }

  public void setCancellationRequested(Boolean cancellationRequested) {
    this.cancellationRequested = cancellationRequested;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }

  public void setCancellationReason(String cancellationReason) {
    this.cancellationReason = cancellationReason;
  }

  public String getMetadata() {
    return metadata;
  }

  public void setMetadata(String metadata) {
    this.metadata = metadata;
  }
}
