package com.krito.muxon.worker.executors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krito.muxon.api.model.EntityType;
import com.krito.muxon.customization.model.CustomizationPhase;
import com.krito.muxon.customization.model.CustomizationStatus;
import com.krito.muxon.providers.GuestAgentInfo;
import com.krito.muxon.providers.VmProvider;
import com.krito.muxon.spi.queue.EntityEventQueue;
import com.krito.muxon.spi.queue.EntityEventQueue.EntityEventTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Asynchronously polls the QEMU guest agent after a VM is started.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>VM transitions to RUNNING → {@link #start} is called.</li>
 *   <li>Polls every {@code pollInterval} seconds for up to {@code timeout}.</li>
 *   <li>On agent contact: reads hostname + IPs, determines sub-phase from cloud-init status
 *       (Linux) or hostname-match heuristic (Windows).</li>
 *   <li>On COMPLETE: emits {@code VM_CUSTOMIZATION_COMPLETE} entity event, detaches seed,
 *       deletes ISO file.</li>
 *   <li>On timeout: emits {@code VM_CUSTOMIZATION_FAILED} entity event.</li>
 * </ol>
 */
public class GuestCustomizationMonitor {

    private static final Logger log = LoggerFactory.getLogger(GuestCustomizationMonitor.class);

    private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(10);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(20);

    private final VmProvider provider;
    private final EntityEventQueue entityEventQueue;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;

    private final UUID vmId;
    private final String externalVmId;
    private final String seedIsoPath;
    private final String expectedHostname;
    private final boolean isWindows;

    private final Duration pollInterval;
    private final Instant deadline;

    private ScheduledFuture<?> task;

    public GuestCustomizationMonitor(
            UUID vmId,
            String externalVmId,
            String seedIsoPath,
            String expectedHostname,
            boolean isWindows,
            VmProvider provider,
            EntityEventQueue entityEventQueue,
            ObjectMapper objectMapper,
            ScheduledExecutorService scheduler) {
        this(vmId, externalVmId, seedIsoPath, expectedHostname, isWindows,
                provider, entityEventQueue, objectMapper, scheduler,
                DEFAULT_POLL_INTERVAL, DEFAULT_TIMEOUT);
    }

    public GuestCustomizationMonitor(
            UUID vmId,
            String externalVmId,
            String seedIsoPath,
            String expectedHostname,
            boolean isWindows,
            VmProvider provider,
            EntityEventQueue entityEventQueue,
            ObjectMapper objectMapper,
            ScheduledExecutorService scheduler,
            Duration pollInterval,
            Duration timeout) {
        this.vmId = vmId;
        this.externalVmId = externalVmId;
        this.seedIsoPath = seedIsoPath;
        this.expectedHostname = expectedHostname;
        this.isWindows = isWindows;
        this.provider = provider;
        this.entityEventQueue = entityEventQueue;
        this.objectMapper = objectMapper;
        this.scheduler = scheduler;
        this.pollInterval = pollInterval;
        this.deadline = Instant.now().plus(timeout);
    }

    public void start() {
        log.info("GuestCustomizationMonitor started: vmId={}, externalVmId={}, isWindows={}, deadline={}",
                vmId, externalVmId, isWindows, deadline);
        publishPhaseUpdate(CustomizationPhase.WAITING_AGENT, null, "Waiting for QEMU guest agent");
        task = scheduler.scheduleWithFixedDelay(this::poll,
                pollInterval.toSeconds(), pollInterval.toSeconds(), TimeUnit.SECONDS);
    }

    private void poll() {
        try {
            if (Instant.now().isAfter(deadline)) {
                log.warn("GuestCustomizationMonitor timeout: vmId={}", vmId);
                complete(false, "Customization timed out after " + DEFAULT_TIMEOUT.toMinutes() + " minutes");
                return;
            }

            GuestAgentInfo info = provider.queryGuestAgent(externalVmId).get(30, TimeUnit.SECONDS);

            if (!info.agentReachable()) {
                log.debug("QGA not yet reachable for vmId={}", vmId);
                return;
            }

            publishPhaseUpdate(CustomizationPhase.IN_PROGRESS, null, "QEMU guest agent online");

            if (isWindows) {
                pollWindows(info);
            } else {
                pollLinux(info);
            }
        } catch (TimeoutException e) {
            log.debug("QGA query timed out for vmId={} — will retry", vmId);
        } catch (Exception e) {
            log.warn("GuestCustomizationMonitor poll error for vmId={}: {}", vmId, e.getMessage(), e);
        }
    }

    private void pollLinux(GuestAgentInfo info) {
        String ciStatus = info.cloudInitStatusJson();
        if (ciStatus == null || ciStatus.isBlank()) {
            publishPhaseUpdate(CustomizationPhase.IN_PROGRESS, "WAITING_CLOUD_INIT",
                    "cloud-init status not yet available");
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> status = objectMapper.readValue(ciStatus, Map.class);
            Object v1 = status.get("v1");
            if (v1 instanceof Map<?, ?> v1map) {
                String stage = String.valueOf(v1map.get("stage"));
                String result = String.valueOf(v1map.get("result"));

                if ("done".equalsIgnoreCase(result) || "null".equals(stage)) {
                    String errors = String.valueOf(v1map.get("errors"));
                    if (!"[]".equals(errors) && !"null".equals(errors)) {
                        complete(false, "cloud-init reported errors: " + errors);
                        return;
                    }
                    complete(true, "cloud-init complete", info);
                    return;
                }
                String subPhase = mapCloudInitStage(stage);
                publishPhaseUpdate(CustomizationPhase.IN_PROGRESS, subPhase,
                        "cloud-init stage: " + stage);
            }
        } catch (Exception e) {
            log.debug("Could not parse cloud-init status for vmId={}: {}", vmId, e.getMessage());
        }
    }

    private void pollWindows(GuestAgentInfo info) {
        // Windows: use hostname match as completion signal (sysprep sets it during specialize).
        String reportedHostname = info.hostname();
        if (reportedHostname != null && !reportedHostname.isBlank()) {
            if (expectedHostname == null
                    || reportedHostname.equalsIgnoreCase(expectedHostname)
                    || reportedHostname.equalsIgnoreCase(expectedHostname.substring(0,
                            Math.min(expectedHostname.length(), 15)))) {
                complete(true, "Windows customization complete — hostname confirmed", info);
            } else {
                publishPhaseUpdate(CustomizationPhase.IN_PROGRESS, null,
                        "Windows hostname not yet set (got: " + reportedHostname + ")");
            }
        }
    }

    private void complete(boolean success, String message) {
        complete(success, message, null);
    }

    private void complete(boolean success, String message, GuestAgentInfo info) {
        if (task != null) {
            task.cancel(false);
        }

        CustomizationPhase phase = success ? CustomizationPhase.COMPLETE : CustomizationPhase.FAILED;
        publishPhaseUpdate(phase, null, message);

        Map<String, Object> payload = new HashMap<>();
        payload.put("customizationPhase", phase.name());
        payload.put("customizationMessage", message);
        if (info != null) {
            if (info.ipAddresses() != null && !info.ipAddresses().isEmpty()) {
                payload.put("ip_addresses", info.ipAddresses());
            }
            if (info.hostname() != null) {
                payload.put("hostname", info.hostname());
            }
        }

        String eventType = success
                ? EntityEventTypes.VM_CUSTOMIZATION_COMPLETE
                : EntityEventTypes.VM_CUSTOMIZATION_FAILED;
        entityEventQueue.publishEntityEvent(EntityType.VM, vmId, eventType, null, payload);

        // Clean up seed ISO
        if (seedIsoPath != null && !seedIsoPath.isBlank()) {
            try {
                provider.detachCustomizationSeed(externalVmId, seedIsoPath).get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Failed to detach customization seed for vmId={}: {}", vmId, e.getMessage());
            }
            try {
                Files.deleteIfExists(Path.of(seedIsoPath));
                log.info("Deleted seed ISO: {}", seedIsoPath);
            } catch (IOException e) {
                log.warn("Failed to delete seed ISO {}: {}", seedIsoPath, e.getMessage());
            }
        }

        log.info("GuestCustomizationMonitor complete: vmId={}, success={}, message={}", vmId, success, message);
    }

    private void publishPhaseUpdate(CustomizationPhase phase, String subPhase, String message) {
        try {
            CustomizationStatus status = new CustomizationStatus();
            status.setPhase(phase);
            status.setSubPhase(subPhase);
            status.setMessage(message);
            if (phase == CustomizationPhase.COMPLETE || phase == CustomizationPhase.FAILED) {
                status.setCompletedAt(Instant.now());
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("customizationStatus", objectMapper.writeValueAsString(status));
            entityEventQueue.publishEntityEvent(EntityType.VM, vmId,
                    EntityEventTypes.VM_CUSTOMIZATION_STATUS_UPDATED, null, payload);
        } catch (Exception e) {
            log.warn("Failed to publish customization phase update for vmId={}: {}", vmId, e.getMessage());
        }
    }

    private static String mapCloudInitStage(String stage) {
        if (stage == null) return null;
        return switch (stage.toLowerCase()) {
            case "network" -> "NETWORK_CONFIG";
            case "config" -> "CONFIG";
            case "final" -> "SCRIPTS_POST";
            default -> stage.toUpperCase();
        };
    }
}
