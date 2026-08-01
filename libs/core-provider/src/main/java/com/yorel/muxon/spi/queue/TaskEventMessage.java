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
package com.yorel.muxon.spi.queue;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Transport-agnostic DTO for a task status event emitted by the worker. Consumed by the
 * orchestrator's TaskEventProcessor to update job/task state.
 */
public record TaskEventMessage(
    UUID id,
    UUID taskId,
    String eventType, // e.g. "task.started", "task.completed", "task.failed"
    Map<String, Object> payload,
    String source,
    Instant createdAt) {
  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private UUID id;
    private UUID taskId;
    private String eventType;
    private Map<String, Object> payload;
    private String source;
    private Instant createdAt;

    public Builder id(UUID id) {
      this.id = id;
      return this;
    }

    public Builder taskId(UUID taskId) {
      this.taskId = taskId;
      return this;
    }

    public Builder eventType(String eventType) {
      this.eventType = eventType;
      return this;
    }

    public Builder payload(Map<String, Object> p) {
      this.payload = p;
      return this;
    }

    public Builder source(String source) {
      this.source = source;
      return this;
    }

    public Builder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public TaskEventMessage build() {
      return new TaskEventMessage(
          id,
          taskId,
          eventType,
          payload != null ? Map.copyOf(payload) : Map.of(),
          source,
          createdAt != null ? createdAt : Instant.now());
    }
  }
}
