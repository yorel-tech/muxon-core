package com.onetattva.infron.core.orch;

import com.onetattva.infron.core.providers.VmCreationRequest;
import com.onetattva.infron.core.providers.VmCreationResult;
import com.onetattva.infron.core.providers.VmProvider;
import com.onetattva.infron.core.providers.VmProviderRegistry;
import com.onetattva.infron.api.enums.EntityType;
import com.onetattva.infron.api.enums.QueueCategory;
import com.onetattva.infron.api.enums.QueueStatus;
import com.onetattva.infron.api.enums.VmPowerState;
import com.onetattva.infron.api.enums.VmStatus;
import com.onetattva.infron.db.model.*;
import com.onetattva.infron.db.repository.VmRepository;
import com.onetattva.infron.db.repository.QueueEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private VmRepository vmRepository;

    @Autowired
    private VmProviderRegistry providerRegistry;

    /**
     * Scheduled task to poll for pending VM command queue entries
     */
    @Scheduled(fixedDelay = 5000) // Poll every 5 seconds
    @Transactional
    public void pollVmQueue() {
        try {
            List<QueueEntry> entries = queueEntryRepository.poll(
                    EntityType.VM, null, POLL_BATCH_SIZE).stream()
                    .limit(POLL_BATCH_SIZE)
                    .toList();

            if (entries.isEmpty()) {
                return;
            }

            logger.info("Processing {} VM queue entries", entries.size());

            for (QueueEntry entry : entries) {
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
            Instant cutoff = Instant.now().minusSeconds(STALL_THRESHOLD_MINUTES * 60L);
            List<QueueEntry> stalled = queueEntryRepository.getStalledEntries(cutoff);
            
            if (!stalled.isEmpty()) {
                logger.warn("Found {} stalled queue entries", stalled.size());
            }
        } catch (Exception e) {
            logger.error("Error checking stalled entries", e);
        }
    }

    /**
     * Process a single queue entry
     */
    private void processQueueEntry(QueueEntry entry) {
        String queueType = entry.getQueueType();
        
        try {
            switch (queueType) {
                case "VM_CREATE_COMMAND":
                    processVmCreateCommand(entry);
                    break;
                case "VM_START_COMMAND":
                    processVmStartCommand(entry);
                    break;
                case "VM_STOP_COMMAND":
                    processVmStopCommand(entry);
                    break;
                case "VM_RESTART_COMMAND":
                    processVmRestartCommand(entry);
                    break;
                case "VM_SUSPEND_COMMAND":
                    processVmSuspendCommand(entry);
                    break;
                case "VM_RESUME_COMMAND":
                    processVmResumeCommand(entry);
                    break;
                case "VM_DELETE_COMMAND":
                    processVmDeleteCommand(entry);
                    break;
                default:
                    logger.warn("Unknown queue type: {}", queueType);
                    queueEntryRepository.markFailed(entry.getId(), "Unknown queue type", Instant.now());
                    return;
            }
        } catch (Exception e) {
            logger.error("Error processing queue entry {}: {}", entry.getId(), e);
            queueEntryRepository.markFailed(entry.getId(), e.getMessage(), Instant.now());
        }
    }

    /**
     * Process VM create command
     */
    private void processVmCreateCommand(QueueEntry entry) {
        UUID vmId = entry.getEntityId();

        // Get VM entity
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        // Update status to PLANNED
        vm.setStatus(VmStatus.PLANNED);
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);

        // Emit status event
        Map<String, Object> payload = Map.of(
                "before_status", VmStatus.PENDING.toString(),
                "after_status", VmStatus.PLANNED.toString()
        );
        queueEntryRepository.save(buildStatusEvent(vmId, "VM_STATUS_CHANGED", payload));

        // Get provider
        VmProvider provider = providerRegistry.getProviderForTenantDatacenter(vm.getTenantDatacenterGrantId())
                .orElseThrow(() -> new RuntimeException("No provider available for datacenter"));

        // Spec from VM entity (persisted by VmsService) or from queue payload
        String specJson = vm.getSpec();
        if (specJson == null || specJson.isEmpty()) {
            Object specPayload = entry.getPayload() != null ? entry.getPayload().get("spec") : null;
            specJson = specPayload instanceof String ? (String) specPayload : "{}";
        }

        String requestId = entry.getMetadata() != null && entry.getMetadata().get("requestId") != null
                ? entry.getMetadata().get("requestId").toString()
                : entry.getId().toString();

        VmCreationRequest createRequest = VmCreationRequest.builder()
                .vmId(vmId)
                .spec(specJson)
                .placement(null)
                .metadata(null)
                .correlationId(requestId)
                .build();

        logger.info("Creating VM {} with provider {}", vmId, provider.id());

        // Update status to PROVISIONING before calling provider
        vm.setStatus(VmStatus.PROVISIONING);
        if (provider.id() != null) {
            try {
                // Provider id may be UUID or "libvirt-<uuid>"
                String id = provider.id();
                vm.setProviderId(id.length() > 36 ? UUID.fromString(id.replaceFirst("^[a-z]+-", "")) : UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {
                // leave providerId null if not parseable as UUID
            }
        }
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);

        payload = Map.of(
                "before_status", VmStatus.PLANNED.toString(),
                "after_status", VmStatus.PROVISIONING.toString(),
                "provider_id", provider.id() != null ? provider.id() : ""
        );
        queueEntryRepository.save(buildStatusEvent(vmId, "VM_STATUS_CHANGED", payload));

        // Call provider to create VM
        VmCreationResult result = provider.createVm(createRequest).join();

        if (result.resultType() == VmCreationResult.ResultType.SUCCESS) {
            vm.setStatus(VmStatus.ACTIVE);
            vm.setPowerState(com.onetattva.infron.api.enums.VmPowerState.ON);
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

            payload = Map.of(
                    "before_status", VmStatus.PROVISIONING.toString(),
                    "after_status", VmStatus.ACTIVE.toString(),
                    "external_id", result.externalVmId() != null ? result.externalVmId() : ""
            );
            queueEntryRepository.save(buildStatusEvent(vmId, "VM_STATUS_CHANGED", payload));
            queueEntryRepository.markCompleted(entry.getId(), Instant.now());
        } else {
            String errorMessage = result.message() != null ? result.message()
                    : (result.error() != null ? result.error().message() : "VM creation failed");
            vm.setStatus(VmStatus.ERROR);
            vm.setUpdatedAt(Instant.now());
            vmRepository.save(vm);
            queueEntryRepository.markFailed(entry.getId(), errorMessage, Instant.now());
            logger.error("VM creation failed for {}: {}", vmId, errorMessage);
        }
    }

    /**
     * Process VM start command
     */
    private void processVmStartCommand(QueueEntry entry) {
        UUID vmId = entry.getEntityId();
        
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        // Validate state
        if (vm.getStatus() != VmStatus.STOPPED && vm.getStatus() != VmStatus.SUSPENDED) {
            logger.warn("VM {} is not in a state that can be started: {}", vmId, vm.getStatus());
            queueEntryRepository.markFailed(entry.getId(),
                    "VM is not in a valid state for start operation", Instant.now());
            return;
        }

        // Update status
        VmStatus previousStatus = vm.getStatus();
        vm.setStatus(VmStatus.ACTIVE);
        vm.setStartedAt(vm.getStartedAt() != null ? vm.getStartedAt() : Instant.now());
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);

        // Emit status event
        Map<String, Object> payload = Map.of(
                "before_status", previousStatus.toString(),
                "after_status", VmStatus.ACTIVE.toString(),
                "power_state", VmPowerState.ON.toString()
        );
        queueEntryRepository.save(buildStatusEvent(vmId, "VM_STATUS_CHANGED", payload));

        // Mark command as completed
        queueEntryRepository.markCompleted(entry.getId(), Instant.now());
    }

    /**
     * Process VM stop command
     */
    private void processVmStopCommand(QueueEntry entry) {
        UUID vmId = entry.getEntityId();
        
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        // Validate state
        if (vm.getStatus() != VmStatus.ACTIVE) {
            logger.warn("VM {} is not in a state that can be stopped: {}", vmId, vm.getStatus());
            queueEntryRepository.markFailed(entry.getId(),
                    "VM is not in a valid state for stop operation", Instant.now());
            return;
        }

        // Update status
        VmStatus previousStatus = vm.getStatus();
        vm.setStatus(VmStatus.STOPPED);
        vm.setPowerState(VmPowerState.OFF);
        vm.setStoppedAt(Instant.now());
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);

        // Emit status event
        Map<String, Object> payload = Map.of(
                "before_status", previousStatus.toString(),
                "after_status", VmStatus.STOPPED.toString(),
                "power_state", VmPowerState.OFF.toString()
        );
        queueEntryRepository.save(buildStatusEvent(vmId, "VM_STATUS_CHANGED", payload));

        // Mark command as completed
        queueEntryRepository.markCompleted(entry.getId(), Instant.now());
    }

    /**
     * Process VM restart command
     */
    private void processVmRestartCommand(QueueEntry entry) {
        UUID vmId = entry.getEntityId();
        
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        // Validate state
        if (vm.getStatus() != VmStatus.ACTIVE) {
            logger.warn("VM {} is not in a state that can be restarted: {}", vmId, vm.getStatus());
            queueEntryRepository.markFailed(entry.getId(),
                    "VM is not in a valid state for restart operation", Instant.now());
            return;
        }

        // Emit operation event
        Map<String, Object> payload = Map.of(
                "operation", "RESTART",
                "previous_status", vm.getStatus().toString()
        );
        queueEntryRepository.save(buildOperationEvent(vmId, "VM_OPERATION_COMPLETED", payload));

        // Mark command as completed
        queueEntryRepository.markCompleted(entry.getId(), Instant.now());
    }

    /**
     * Process VM suspend command
     */
    private void processVmSuspendCommand(QueueEntry entry) {
        UUID vmId = entry.getEntityId();
        
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        // Validate state
        if (vm.getStatus() != VmStatus.ACTIVE) {
            logger.warn("VM {} is not in a state that can be suspended: {}", vmId, vm.getStatus());
            queueEntryRepository.markFailed(entry.getId(),
                    "VM is not in a valid state for suspend operation", Instant.now());
            return;
        }

        // Update status
        VmStatus previousStatus = vm.getStatus();
        vm.setStatus(VmStatus.SUSPENDED);
        vm.setPowerState(VmPowerState.SUSPENDED);
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);

        // Emit status event
        Map<String, Object> payload = Map.of(
                "before_status", previousStatus.toString(),
                "after_status", VmStatus.SUSPENDED.toString(),
                "power_state", VmPowerState.SUSPENDED.toString()
        );
        queueEntryRepository.save(buildStatusEvent(vmId, "VM_STATUS_CHANGED", payload));

        // Mark command as completed
        queueEntryRepository.markCompleted(entry.getId(), Instant.now());
    }

    /**
     * Process VM resume command
     */
    private void processVmResumeCommand(QueueEntry entry) {
        UUID vmId = entry.getEntityId();
        
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        // Validate state
        if (vm.getStatus() != VmStatus.SUSPENDED) {
            logger.warn("VM {} is not in a state that can be resumed: {}", vmId, vm.getStatus());
            queueEntryRepository.markFailed(entry.getId(),
                    "VM is not in a valid state for resume operation", Instant.now());
            return;
        }

        // Update status
        VmStatus previousStatus = vm.getStatus();
        vm.setStatus(VmStatus.ACTIVE);
        vm.setPowerState(VmPowerState.ON);
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);

        // Emit status event
        Map<String, Object> payload = Map.of(
                "before_status", previousStatus.toString(),
                "after_status", VmStatus.ACTIVE.toString(),
                "power_state", VmPowerState.ON.toString()
        );
        queueEntryRepository.save(buildStatusEvent(vmId, "VM_STATUS_CHANGED", payload));

        // Mark command as completed
        queueEntryRepository.markCompleted(entry.getId(), Instant.now());
    }

    /**
     * Process VM delete command
     */
    private void processVmDeleteCommand(QueueEntry entry) {
        UUID vmId = entry.getEntityId();
        
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));

        // Update status to DELETING
        VmStatus previousStatus = vm.getStatus();
        vm.setStatus(VmStatus.DELETING);
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);

        // Emit status event
        Map<String, Object> payload = Map.of(
                "before_status", previousStatus.toString(),
                "after_status", VmStatus.DELETING.toString()
        );
        queueEntryRepository.save(buildStatusEvent(vmId, "VM_STATUS_CHANGED", payload));

        // Mark command as completed
        queueEntryRepository.markCompleted(entry.getId(), Instant.now());
    }

    /**
     * Build a status event queue entry
     */
    private QueueEntry buildStatusEvent(UUID vmId, String eventType, Map<String, Object> payload) {
        QueueEntry entry = new QueueEntry();
        entry.setQueueType(eventType);
        entry.setEntityType(EntityType.VM);
        entry.setEntityId(vmId);
        entry.setQueueCategory(QueueCategory.STATUS);
        entry.setStatus(QueueStatus.PENDING);
        entry.setPayload(payload);
        entry.setActorType("SYSTEM");
        entry.setSource("orchestrator");
        entry.setCreatedAt(Instant.now());
        return entry;
    }

    /**
     * Build an operation event queue entry
     */
    private QueueEntry buildOperationEvent(UUID vmId, String eventType, Map<String, Object> payload) {
        QueueEntry entry = new QueueEntry();
        entry.setQueueType(eventType);
        entry.setEntityType(EntityType.VM);
        entry.setEntityId(vmId);
        entry.setQueueCategory(QueueCategory.STATUS);
        entry.setStatus(QueueStatus.PENDING);
        entry.setPayload(payload);
        entry.setActorType("SYSTEM");
        entry.setSource("orchestrator");
        entry.setCreatedAt(Instant.now());
        return entry;
    }
}
