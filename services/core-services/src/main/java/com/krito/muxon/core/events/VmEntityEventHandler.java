package com.krito.muxon.core.events;

import com.krito.muxon.api.enums.VmPowerState;
import com.krito.muxon.api.enums.VmStatus;
import com.krito.muxon.core.spi.queue.EntityEventMessage;
import com.krito.muxon.db.model.VmEntity;
import com.krito.muxon.db.repository.VmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles entity events for VM entities.
 * Updates the {@code vm} table based on events published by the worker via {@code EntityEventQueue}.
 *
 * <p>This replaces the direct VM state updates that were previously performed by the orchestrator's
 * the legacy orchestrator VM poller (which directly called {@code vmRepository.save()}).
 */
@Component
public class VmEntityEventHandler {

    private static final Logger log = LoggerFactory.getLogger(VmEntityEventHandler.class);

    @Autowired
    private VmRepository vmRepository;

    @Transactional
    public void onCreated(EntityEventMessage event) {
        updateVm(event.entityId(), vm -> {
            vm.setStatus(VmStatus.ACTIVE);
            vm.setPowerState(VmPowerState.ON);
            vm.setStartedAt(Instant.now());
            String extId = (String) event.payload().get("external_id");
            if (extId != null) vm.setExternalId(extId);
            Object ips = event.payload().get("ip_addresses");
            if (ips instanceof List<?> list) {
                vm.setIpAddresses(list.stream().map(Object::toString).toList());
            }
            log.info("VM {} created: external_id={}", event.entityId(), extId);
        });
    }

    @Transactional
    public void onCreationFailed(EntityEventMessage event) {
        updateVm(event.entityId(), vm -> {
            vm.setStatus(VmStatus.ERROR);
            log.warn("VM {} creation failed: {}", event.entityId(), event.payload().get("message"));
        });
    }

    @Transactional
    public void onPowerOn(EntityEventMessage event) {
        updateVm(event.entityId(), vm -> {
            vm.setStatus(VmStatus.ACTIVE);
            vm.setPowerState(VmPowerState.ON);
            vm.setStartedAt(Instant.now());
            log.info("VM {} powered on", event.entityId());
        });
    }

    @Transactional
    public void onPowerOff(EntityEventMessage event) {
        updateVm(event.entityId(), vm -> {
            vm.setStatus(VmStatus.STOPPED);
            vm.setPowerState(VmPowerState.OFF);
            vm.setStoppedAt(Instant.now());
            log.info("VM {} powered off", event.entityId());
        });
    }

    @Transactional
    public void onSuspended(EntityEventMessage event) {
        updateVm(event.entityId(), vm -> {
            vm.setStatus(VmStatus.SUSPENDED);
            vm.setPowerState(VmPowerState.SUSPENDED);
            log.info("VM {} suspended", event.entityId());
        });
    }

    @Transactional
    public void onDeleted(EntityEventMessage event) {
        updateVm(event.entityId(), vm -> {
            vm.setStatus(VmStatus.DELETED);
            vm.setPowerState(VmPowerState.UNKNOWN);
            log.info("VM {} deleted", event.entityId());
        });
    }

    @Transactional
    public void onDeletionFailed(EntityEventMessage event) {
        updateVm(event.entityId(), vm -> {
            vm.setStatus(VmStatus.ERROR);
            log.warn("VM {} deletion failed: {}", event.entityId(), event.payload().get("message"));
        });
    }

    @Transactional
    public void onOperationFailed(EntityEventMessage event) {
        updateVm(event.entityId(), vm -> {
            vm.setStatus(VmStatus.ERROR);
            log.warn("VM {} operation failed (op={}, msg={})",
                    event.entityId(), event.payload().get("operation"), event.payload().get("message"));
        });
    }

    @Transactional
    public void onMigrated(EntityEventMessage event) {
        updateVm(event.entityId(), vm -> {
            Object nodeId = event.payload().get("node_id");
            if (nodeId != null) {
                try { vm.setNodeId(UUID.fromString(nodeId.toString())); }
                catch (IllegalArgumentException ignored) {}
            }
            Object ips = event.payload().get("ip_addresses");
            if (ips instanceof List<?> list) {
                vm.setIpAddresses(list.stream().map(Object::toString).toList());
            }
            log.info("VM {} migrated to node {}", event.entityId(), nodeId);
        });
    }

    // ── private ───────────────────────────────────────────────────────────────

    private void updateVm(UUID vmId, VmUpdater updater) {
        VmEntity vm = vmRepository.findById(vmId).orElse(null);
        if (vm == null) {
            log.warn("VM {} not found for entity event — skipping update", vmId);
            return;
        }
        updater.update(vm);
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);
    }

    @FunctionalInterface
    private interface VmUpdater {
        void update(VmEntity vm);
    }
}
