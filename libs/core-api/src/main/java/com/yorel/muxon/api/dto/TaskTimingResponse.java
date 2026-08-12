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
