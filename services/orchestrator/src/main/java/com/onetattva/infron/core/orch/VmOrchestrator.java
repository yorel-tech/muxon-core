package com.onetattva.infron.core.orch;

import com.onetattva.infron.core.providers.ProviderContext;
import com.onetattva.infron.core.providers.VmCreationRequest;
import com.onetattva.infron.core.providers.VmCreationResult;
import com.onetattva.infron.core.providers.VmProvider;
import com.onetattva.infron.core.providers.TenantAwareVmProviderRegistry;
import com.onetattva.infron.core.spi.queue.CommandMessage;
import com.onetattva.infron.core.spi.queue.CommandQueue;
import com.onetattva.infron.core.spi.queue.EventPublisher;
import com.onetattva.infron.api.enums.EntityType;
import com.onetattva.infron.api.enums.VmPowerState;
import com.onetattva.infron.api.enums.VmStatus;
import com.onetattva.infron.db.model.VmEntity;
import com.onetattva.infron.db.repository.VmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * VM Orchestrator service.
 * Processes queue entries and manages VM lifecycle by invoking providers.
 */
@Service
public class VmOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(VmOrchestrator.class);
    private static final int POLL_BATCH_SIZE = 10;
    private static final int STALL_THRESHOLD_MINUTES = 10;

    @Autowired
    private CommandQueue commandQueue;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private VmRepository vmRepository;

    @Autowired
    private TenantAwareVmProviderRegistry providerRegistry;

    /**
     * Scheduled task to poll for pending VM command queue entries
     */
    @Scheduled(fixedDelay = 5000) // Poll every 5 seconds
    @Transactional
    public void pollVmQueue() {
        try {
            List<CommandMessage> entries = commandQueue.pollCommands(EntityType.VM, POLL_BATCH_SIZE);

            if (entries.isEmpty()) {
                return;
            }

            logger.info("Processing {} VM queue entries", entries.size());

            for (CommandMessage entry : entries) {
                processQueueEntry(entry);
            }
        } catch (Exception e) {
            logger.error("Error polling VM queue", e);
        }
    }

    /**
     * Scheduled task to check for stalled queue entries
     */
    @Scheduled(fixedDelay = 60000) // Check every minute
    @Transactional
    public void checkStalledEntries() {
        try {
            int stalled = commandQueue.getStalledCount(STALL_THRESHOLD_MINUTES);
            if (stalled > 0) {
                logger.warn("Found {} stalled queue entries", stalled);
                int reset = commandQueue.resetStalledEntries(STALL_THRESHOLD_MINUTES);
                if (reset > 0) {
                    logger.info("Reset {} stalled entries for retry", reset);
                }
            }
        } catch (Exception e) {
            logger.error("Error checking stalled entries", e);
        }
    }

    /**
     * Process a single queue entry
     */
    private void processQueueEntry(CommandMessage entry) {
        String queueType = entry.queueType();

        try {
            switch (queueType) {
                case "VM_CREATE_COMMAND" -> processVmCreateCommand(entry);
                case "VM_START_COMMAND" -> processVmStartCommand(entry);
                case "VM_STOP_COMMAND" -> processVmStopCommand(entry);
                case "VM_RESTART_COMMAND" -> processVmRestartCommand(entry);
                case "VM_SUSPEND_COMMAND" -> processVmSuspendCommand(entry);
                case "VM_RESUME_COMMAND" -> processVmResumeCommand(entry);
                case "VM_DELETE_COMMAND" -> processVmDeleteCommand(entry);
                default -> {
                    logger.warn("Unknown queue type: {}", queueType);
                    commandQueue.markFailed(entry.id(), "Unknown queue type");
                    return;
                }
            }
        } catch (Exception e) {
            logger.error("Error processing queue entry {}: {}", entry.id(), e);
            commandQueue.markFailed(entry.id(), e.getMessage());
        }
    }

    /**
     * Process VM create command
     */
    private void processVmCreateCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();

        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        vm.setStatus(VmStatus.PLANNED);
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);

        eventPublisher.publishEvent(EntityType.VM, vmId, "VM_STATUS_CHANGED", Map.of(
                "before_status", VmStatus.PENDING.toString(),
                "after_status", VmStatus.PLANNED.toString()
        ));

        // Create provider context instead of using placement(null)
        Optional<ProviderContext> contextOpt = providerRegistry.createContextForTenantDatacenter(vm.getTenantDatacenterGrantId());
        if (contextOpt.isEmpty()) {
            commandQueue.markFailed(entry.id(), "Failed to create provider context");
            return;
        }

        VmProvider provider = providerRegistry.resolveProviderForTenantDatacenter(vm.getTenantDatacenterGrantId())
                .orElseThrow(() -> new RuntimeException("No provider available for datacenter"));

        String specJson = vm.getSpec();
        if (specJson == null || specJson.isEmpty()) {
            Object specPayload = entry.payload() != null ? entry.payload().get("spec") : null;
            specJson = specPayload instanceof String ? (String) specPayload : "{}";
        }

        String requestId = entry.metadata() != null && entry.metadata().get("requestId") != null
                ? entry.metadata().get("requestId")
                : entry.id().toString();

        VmCreationRequest createRequest = VmCreationRequest.builder()
                .vmId(vmId)
                .spec(specJson)
                .providerContext(contextOpt.get())  // Use ProviderContext instead of PlacementHints
                .metadata(null)
                .correlationId(requestId)
                .build();

        logger.info("Creating VM {} with provider {}", vmId, provider.id());

        vm.setStatus(VmStatus.PROVISIONING);
        if (provider.id() != null) {
            try {
                String id = provider.id();
                vm.setProviderId(id.length() > 36 ? UUID.fromString(id.replaceFirst("^[a-z]+-", "")) : UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {
            }
        }
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);

        eventPublisher.publishEvent(EntityType.VM, vmId, "VM_STATUS_CHANGED", Map.of(
                "before_status", VmStatus.PLANNED.toString(),
                "after_status", VmStatus.PROVISIONING.toString(),
                "provider_id", provider.id() != null ? provider.id() : ""
        ));

        VmCreationResult result = provider.createVm(createRequest).join();

        if (result.resultType() == VmCreationResult.ResultType.SUCCESS) {
            vm.setStatus(VmStatus.ACTIVE);
            vm.setPowerState(VmPowerState.ON);
            vm.setExternalId(result.externalVmId());
            vm.setStartedAt(Instant.now());
            if (result.vmInfo() != null && result.vmInfo().ipAddresses() != null && !result.vmInfo().ipAddresses().isEmpty()) {
                vm.setIpAddresses(result.vmInfo().ipAddresses());
            }
            if (result.vmInfo() != null && result.vmInfo().hostname() != null) {
                vm.setHostname(result.vmInfo().hostname());
            }
            vm.setUpdatedAt(Instant.now());
            vmRepository.save(vm);

            eventPublisher.publishEvent(EntityType.VM, vmId, "VM_STATUS_CHANGED", Map.of(
                    "before_status", VmStatus.PROVISIONING.toString(),
                    "after_status", VmStatus.ACTIVE.toString(),
                    "external_id", result.externalVmId() != null ? result.externalVmId() : ""
            ));
            commandQueue.markCompleted(entry.id());
        } else {
            String errorMessage = result.message() != null ? result.message()
                    : (result.error() != null ? result.error().message() : "VM creation failed");
            vm.setStatus(VmStatus.ERROR);
            vm.setUpdatedAt(Instant.now());
            vmRepository.save(vm);
            commandQueue.markFailed(entry.id(), errorMessage);
            logger.error("VM creation failed for {}: {}", vmId, errorMessage);
        }
    }

    private void processVmStartCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        if (vm.getStatus() != VmStatus.STOPPED && vm.getStatus() != VmStatus.SUSPENDED) {
            logger.warn("VM {} is not in a state that can be started: {}", vmId, vm.getStatus());
            commandQueue.markFailed(entry.id(), "VM is not in a valid state for start operation");
            return;
        }

        VmStatus previousStatus = vm.getStatus();
        vm.setStatus(VmStatus.ACTIVE);
        vm.setStartedAt(vm.getStartedAt() != null ? vm.getStartedAt() : Instant.now());
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);

        eventPublisher.publishEvent(EntityType.VM, vmId, "VM_STATUS_CHANGED", Map.of(
                "before_status", previousStatus.toString(),
                "after_status", VmStatus.ACTIVE.toString(),
                "power_state", VmPowerState.ON.toString()
        ));
        commandQueue.markCompleted(entry.id());
    }

    private void processVmStopCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        if (vm.getStatus() != VmStatus.ACTIVE) {
            logger.warn("VM {} is not in a state that can be stopped: {}", vmId, vm.getStatus());
            commandQueue.markFailed(entry.id(), "VM is not in a valid state for stop operation");
            return;
        }

        VmStatus previousStatus = vm.getStatus();
        vm.setStatus(VmStatus.STOPPED);
        vm.setPowerState(VmPowerState.OFF);
        vm.setStoppedAt(Instant.now());
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);

        eventPublisher.publishEvent(EntityType.VM, vmId, "VM_STATUS_CHANGED", Map.of(
                "before_status", previousStatus.toString(),
                "after_status", VmStatus.STOPPED.toString(),
                "power_state", VmPowerState.OFF.toString()
        ));
        commandQueue.markCompleted(entry.id());
    }

    private void processVmRestartCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        if (vm.getStatus() != VmStatus.ACTIVE) {
            logger.warn("VM {} is not in a state that can be restarted: {}", vmId, vm.getStatus());
            commandQueue.markFailed(entry.id(), "VM is not in a valid state for restart operation");
            return;
        }

        eventPublisher.publishEvent(EntityType.VM, vmId, "VM_OPERATION_COMPLETED", Map.of(
                "operation", "RESTART",
                "previous_status", vm.getStatus().toString()
        ));
        commandQueue.markCompleted(entry.id());
    }

    private void processVmSuspendCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        if (vm.getStatus() != VmStatus.ACTIVE) {
            logger.warn("VM {} is not in a state that can be suspended: {}", vmId, vm.getStatus());
            commandQueue.markFailed(entry.id(), "VM is not in a valid state for suspend operation");
            return;
        }

        VmStatus previousStatus = vm.getStatus();
        vm.setStatus(VmStatus.SUSPENDED);
        vm.setPowerState(VmPowerState.SUSPENDED);
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);

        eventPublisher.publishEvent(EntityType.VM, vmId, "VM_STATUS_CHANGED", Map.of(
                "before_status", previousStatus.toString(),
                "after_status", VmStatus.SUSPENDED.toString(),
                "power_state", VmPowerState.SUSPENDED.toString()
        ));
        commandQueue.markCompleted(entry.id());
    }

    private void processVmResumeCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        if (vm.getStatus() != VmStatus.SUSPENDED) {
            logger.warn("VM {} is not in a state that can be resumed: {}", vmId, vm.getStatus());
            commandQueue.markFailed(entry.id(), "VM is not in a valid state for resume operation");
            return;
        }

        VmStatus previousStatus = vm.getStatus();
        vm.setStatus(VmStatus.ACTIVE);
        vm.setPowerState(VmPowerState.ON);
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);

        eventPublisher.publishEvent(EntityType.VM, vmId, "VM_STATUS_CHANGED", Map.of(
                "before_status", previousStatus.toString(),
                "after_status", VmStatus.ACTIVE.toString(),
                "power_state", VmPowerState.ON.toString()
        ));
        commandQueue.markCompleted(entry.id());
    }

    private void processVmDeleteCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        VmStatus previousStatus = vm.getStatus();
        vm.setStatus(VmStatus.DELETING);
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);

        eventPublisher.publishEvent(EntityType.VM, vmId, "VM_STATUS_CHANGED", Map.of(
                "before_status", previousStatus.toString(),
                "after_status", VmStatus.DELETING.toString()
        ));
        commandQueue.markCompleted(entry.id());
    }
}
