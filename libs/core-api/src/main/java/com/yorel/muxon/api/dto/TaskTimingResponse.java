package com.yorel.muxon.api.dto;

import java.time.Instant;

public class TaskTimingResponse {
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant estimatedCompletionAt;
    private Instant timeoutAt;
    private Double durationSeconds;
    private Double estimatedRemainingSeconds;

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

    public Instant getEstimatedCompletionAt() {
        return estimatedCompletionAt;
    }

    public void setEstimatedCompletionAt(Instant estimatedCompletionAt) {
        this.estimatedCompletionAt = estimatedCompletionAt;
    }

    public Instant getTimeoutAt() {
        return timeoutAt;
    }

    public void setTimeoutAt(Instant timeoutAt) {
        this.timeoutAt = timeoutAt;
    }

    public Double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Double durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Double getEstimatedRemainingSeconds() {
        return estimatedRemainingSeconds;
    }

    public void setEstimatedRemainingSeconds(Double estimatedRemainingSeconds) {
        this.estimatedRemainingSeconds = estimatedRemainingSeconds;
    }
}
