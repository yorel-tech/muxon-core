package com.onetattva.infron.core.spi.queue;

import com.onetattva.infron.api.enums.EntityType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Transport-agnostic DTO for a command enqueued for async processing.
 * Maps to/from queue storage (e.g. QueueEntry for DB, or message payload for Kafka).
 */
public record CommandMessage(
    UUID id,
    String queueType,
    EntityType entityType,
    UUID entityId,
    Map<String, Object> payload,
    Map<String, String> metadata,
    String source,
    String actorType,
    UUID actorUserId,
    String actorService,
    Instant createdAt,
    String requestId,
    String correlationId
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String queueType;
        private EntityType entityType;
        private UUID entityId;
        private Map<String, Object> payload;
        private Map<String, String> metadata;
        private String source;
        private String actorType;
        private UUID actorUserId;
        private String actorService;
        private Instant createdAt;
        private String requestId;
        private String correlationId;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder queueType(String queueType) {
            this.queueType = queueType;
            return this;
        }

        public Builder entityType(EntityType entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder entityId(UUID entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder payload(Map<String, Object> payload) {
            this.payload = payload;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder actorType(String actorType) {
            this.actorType = actorType;
            return this;
        }

        public Builder actorUserId(UUID actorUserId) {
            this.actorUserId = actorUserId;
            return this;
        }

        public Builder actorService(String actorService) {
            this.actorService = actorService;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public CommandMessage build() {
            return new CommandMessage(
                id,
                queueType,
                entityType,
                entityId,
                payload != null ? Map.copyOf(payload) : Map.of(),
                metadata != null ? Map.copyOf(metadata) : Map.of(),
                source,
                actorType,
                actorUserId,
                actorService,
                createdAt != null ? createdAt : Instant.now(),
                requestId,
                correlationId
            );
        }
    }
}
