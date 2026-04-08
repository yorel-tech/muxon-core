package com.onetattva.infron.core.spi.queue;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Transport-agnostic DTO for a task status event emitted by the worker.
 * Consumed by the orchestrator's TaskEventProcessor to update job/task state.
 */
public record TaskEventMessage(
        UUID id,
        UUID taskId,
        String eventType,      // e.g. "task.started", "task.completed", "task.failed"
        Map<String, Object> payload,
        String source,
        Instant createdAt
) {
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

        public Builder id(UUID id)                        { this.id = id; return this; }
        public Builder taskId(UUID taskId)                { this.taskId = taskId; return this; }
        public Builder eventType(String eventType)        { this.eventType = eventType; return this; }
        public Builder payload(Map<String, Object> p)     { this.payload = p; return this; }
        public Builder source(String source)              { this.source = source; return this; }
        public Builder createdAt(Instant createdAt)       { this.createdAt = createdAt; return this; }

        public TaskEventMessage build() {
            return new TaskEventMessage(
                    id, taskId, eventType,
                    payload != null ? Map.copyOf(payload) : Map.of(),
                    source,
                    createdAt != null ? createdAt : Instant.now());
        }
    }
}
