package com.onetattva.infron.core.spi.queue;

import com.onetattva.infron.api.model.EntityType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Transport-agnostic port for entity state events flowing from the worker to core-services.
 *
 * <p>OSS: backed by the {@code queue_entry} table (ENTITY_EVENT category) in {@code DbEntityEventQueue}.
 * <p>Enterprise: replaced by a Kafka implementation in infron-nexus via {@code @ConditionalOnMissingBean}.
 *
 * <p>Event type naming: {@code <entity>.<action>} — e.g. {@code vm.created}, {@code vm.power.on}.
 * See {@link EntityEventTypes} for well-known constants.
 */
public interface EntityEventQueue {

    /**
     * Publish an entity state change event (called by the worker after a successful provider operation).
     *
     * @param entityType type of entity affected (VM, NODE, etc.)
     * @param entityId   ID of the affected entity in core-services DB
     * @param eventType  e.g. {@code "vm.created"}, {@code "vm.power.on"}
     * @param taskId     the task that produced this event (may be null)
     * @param payload    additional context (external_id, ip_addresses, etc.)
     */
    void publishEntityEvent(EntityType entityType, UUID entityId,
                            String eventType, UUID taskId,
                            Map<String, Object> payload);

    /**
     * Poll for pending entity events.
     * Implementation must claim entries (mark PROCESSING) so the same event is not double-processed.
     *
     * @param limit maximum number of events to return
     */
    List<EntityEventMessage> pollEntityEvents(int limit);

    /**
     * Mark an entity event as successfully processed.
     */
    void markProcessed(UUID eventId);

    /**
     * Mark an entity event as failed with an error description.
     */
    void markFailed(UUID eventId, String error);

    /** Well-known entity event type constants. */
    interface EntityEventTypes {
        // VM events
        String VM_CREATED           = "vm.created";
        String VM_CREATION_FAILED   = "vm.creation.failed";
        String VM_POWER_ON          = "vm.power.on";
        String VM_POWER_OFF         = "vm.power.off";
        String VM_SUSPENDED         = "vm.suspended";
        String VM_DELETED           = "vm.deleted";
        String VM_DELETION_FAILED   = "vm.deletion.failed";
        String VM_OPERATION_FAILED  = "vm.operation.failed";
        String VM_MIGRATED          = "vm.migrated";

        // Content library distribution (replication to a datacenter)
        String CL_DISTRIBUTION_ITEM_UPDATED = "content_library_distribution.item_updated";
        String CL_DISTRIBUTION_COMPLETED    = "content_library_distribution.completed";
        String CL_DISTRIBUTION_FAILED       = "content_library_distribution.failed";
    }
}
