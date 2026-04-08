package com.onetattva.infron.core.events;

import com.onetattva.infron.api.model.EntityType;
import com.onetattva.infron.core.spi.queue.EntityEventMessage;
import com.onetattva.infron.core.spi.queue.EntityEventQueue;
import com.onetattva.infron.core.spi.queue.EntityEventQueue.EntityEventTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Polls the {@link EntityEventQueue} and routes each event to the appropriate entity handler.
 *
 * <p>This is the <strong>only</strong> component in {@code core-services} that updates entity tables
 * in response to worker-reported state changes. Previously this was done by the orchestrator's
 * {@code VmOrchestrator} via direct {@code vmRepository.save()} calls — that violation is now removed.
 *
 * <p>Entity state machine transitions (e.g. VM PENDING → ACTIVE) happen exclusively here.
 */
@Service
public class EntityEventProcessor {

    private static final Logger log = LoggerFactory.getLogger(EntityEventProcessor.class);
    private static final int POLL_BATCH = 50;

    @Autowired
    private EntityEventQueue entityEventQueue;

    @Autowired
    private VmEntityEventHandler vmEventHandler;

    @Scheduled(fixedDelay = 2000)
    public void processEntityEvents() {
        List<EntityEventMessage> events = entityEventQueue.pollEntityEvents(POLL_BATCH);
        if (events.isEmpty()) {
            return;
        }
        log.debug("Processing {} entity event(s)", events.size());
        for (EntityEventMessage event : events) {
            try {
                dispatch(event);
                entityEventQueue.markProcessed(event.id());
            } catch (Exception ex) {
                log.error("Failed to process entity event {} (type={}): {}",
                        event.id(), event.eventType(), ex.getMessage(), ex);
                entityEventQueue.markFailed(event.id(), ex.getMessage());
            }
        }
    }

    private void dispatch(EntityEventMessage event) {
        if (event.entityType() == EntityType.VM) {
            dispatchVmEvent(event);
        } else {
            log.debug("No handler for entity type {} (event {})", event.entityType(), event.eventType());
        }
    }

    private void dispatchVmEvent(EntityEventMessage event) {
        switch (event.eventType()) {
            case EntityEventTypes.VM_CREATED          -> vmEventHandler.onCreated(event);
            case EntityEventTypes.VM_CREATION_FAILED  -> vmEventHandler.onCreationFailed(event);
            case EntityEventTypes.VM_POWER_ON         -> vmEventHandler.onPowerOn(event);
            case EntityEventTypes.VM_POWER_OFF        -> vmEventHandler.onPowerOff(event);
            case EntityEventTypes.VM_SUSPENDED        -> vmEventHandler.onSuspended(event);
            case EntityEventTypes.VM_DELETED          -> vmEventHandler.onDeleted(event);
            case EntityEventTypes.VM_DELETION_FAILED  -> vmEventHandler.onDeletionFailed(event);
            case EntityEventTypes.VM_OPERATION_FAILED -> vmEventHandler.onOperationFailed(event);
            case EntityEventTypes.VM_MIGRATED         -> vmEventHandler.onMigrated(event);
            default -> log.debug("Unhandled VM event type '{}' for entity {}", event.eventType(), event.entityId());
        }
    }
}
