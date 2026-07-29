package com.yorel.muxon.spi.queue;

import com.yorel.muxon.api.model.EntityType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Transport-agnostic DTO for an entity state event emitted by the worker.
 * Consumed by core-services EntityEventProcessor to update entity tables.
 *
 * <p>Event type naming convention: {@code <entity>.<action>}
 * Examples: {@code vm.created}, {@code vm.power.on}, {@code vm.deleted}
 */
public record EntityEventMessage(
        UUID id,
        EntityType entityType,
        UUID entityId,
        String eventType,       // e.g. "vm.created", "vm.power.on", "vm.deleted"
        UUID taskId,            // the task that triggered this event (nullable)
        Map<String, Object> payload,
        String source,
        Instant createdAt
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID id;
        private EntityType entityType;
        private UUID entityId;
        private String eventType;
        private UUID taskId;
        private Map<String, Object> payload;
        private String source;
        private Instant createdAt;

        public Builder id(UUID id)                        { this.id = id; return this; }
        public Builder entityType(EntityType t)           { this.entityType = t; return this; }
        public Builder entityId(UUID entityId)            { this.entityId = entityId; return this; }
        public Builder eventType(String eventType)        { this.eventType = eventType; return this; }
        public Builder taskId(UUID taskId)                { this.taskId = taskId; return this; }
        public Builder payload(Map<String, Object> p)     { this.payload = p; return this; }
        public Builder source(String source)              { this.source = source; return this; }
        public Builder createdAt(Instant createdAt)       { this.createdAt = createdAt; return this; }

        public EntityEventMessage build() {
            return new EntityEventMessage(
                    id, entityType, entityId, eventType, taskId,
                    payload != null ? Map.copyOf(payload) : Map.of(),
                    source,
                    createdAt != null ? createdAt : Instant.now());
        }
    }
}
