package com.onetattva.infron.core.spi.queue;

import com.onetattva.infron.api.enums.EntityType;

import java.util.Map;
import java.util.UUID;

/**
 * Transport-agnostic port for publishing status/audit events.
 * Implementations may write to the same DB queue table (OSS) or to a message bus topic provided by enterprise extensions.
 */
public interface EventPublisher {

    /**
     * Publish a status or audit event (e.g. VM_STATUS_CHANGED, VM_OPERATION_COMPLETED).
     *
     * @param entityType  type of entity (e.g. VM)
     * @param entityId    id of the entity
     * @param eventType   event type string (e.g. "VM_STATUS_CHANGED")
     * @param payload     event payload (before_status, after_status, etc.)
     */
    void publishEvent(EntityType entityType, UUID entityId, String eventType, Map<String, Object> payload);
}
