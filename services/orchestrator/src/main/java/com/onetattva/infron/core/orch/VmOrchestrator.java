package com.onetattva.infron.core.orch;

/**
 * @deprecated Replaced by {@link com.onetattva.infron.core.orch.worker.WorkerTaskPoller} +
 * {@link com.onetattva.infron.core.orch.worker.VmTaskExecutor} in Stage 3.
 * The @Service annotation below is intentionally disabled; this class is preserved for
 * reference only and will be removed in a cleanup pass.
 */
@Deprecated(since = "stage3", forRemoval = true)

import com.onetattva.infron.core.providers.IsoAttachment;
import com.onetattva.infron.core.providers.ProviderContext;
import com.onetattva.infron.core.providers.VmCreationRequest;
import com.onetattva.infron.core.providers.VmCreationResult;
import com.onetattva.infron.core.providers.VmIsoAttachProviderRequest;
import com.onetattva.infron.core.providers.VmIsoDetachProviderRequest;
import com.onetattva.infron.core.providers.VmOperationResult;
import com.onetattva.infron.core.providers.VmProvider;
import com.onetattva.infron.core.providers.VmConsoleConnectionInfo;
import com.onetattva.infron.core.providers.VmConsoleRequest;
import com.onetattva.infron.core.providers.VmTemplateExportRequest;
import com.onetattva.infron.core.providers.VmTemplateExportResult;
import com.onetattva.infron.core.orch.wiring.TenantAwareVmProviderRegistry;
import com.onetattva.infron.core.spi.queue.CommandMessage;
import com.onetattva.infron.core.spi.queue.CommandQueue;
import com.onetattva.infron.core.spi.queue.EventPublisher;
import com.onetattva.infron.core.spi.queue.VmConsoleResolvePayloadKeys;
import com.onetattva.infron.core.spi.queue.VmQueueCommands;
import com.onetattva.infron.api.model.EntityType;
import com.onetattva.infron.api.enums.VmPowerState;
import com.onetattva.infron.api.enums.VmStatus;
import com.onetattva.infron.db.model.ContentItemEntity;
import com.onetattva.infron.db.model.VmEntity;
import com.onetattva.infron.db.repository.ContentItemRepository;
import com.onetattva.infron.db.repository.QueueEntryRepository;
import com.onetattva.infron.db.repository.VmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * VM Orchestrator service.
 * @deprecated Replaced by {@code WorkerTaskPoller} + {@code VmTaskExecutor} in Stage 3.
 * Disabled via conditional; preserved for reference.
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@org.springframework.context.annotation.Conditional(VmOrchestrator.Disabled.class)
public class VmOrchestrator {

    /** Condition that never matches — permanently disables this bean. */
    static class Disabled implements org.springframework.context.annotation.Condition {
        @Override
        public boolean matches(org.springframework.context.annotation.ConditionContext ctx,
                               org.springframework.core.type.AnnotatedTypeMetadata meta) {
            return false;
        }
    }

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

    @Autowired
    private ContentItemRepository contentItemRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    /**
     * Scheduled task to poll for pending VM command queue entries
     */
    @Scheduled(fixedDelay = 5000) // Poll every 5 seconds
    @Transactional
    public void pollVmQueue() {
        try {
            List<CommandMessage> entries = commandQueue.pollCommands(EntityType.VM, POLL_BATCH_SIZE);
            logger.debug("VM queue poll finished: claimed {} VM command(s)", entries.size());

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
                case "VM_ATTACH_ISO_COMMAND" -> processVmAttachIsoCommand(entry);
                case "VM_DETACH_ISO_COMMAND" -> processVmDetachIsoCommand(entry);
                case "VM_PUBLISH_TEMPLATE_COMMAND" -> processVmPublishTemplateCommand(entry);
                case VmQueueCommands.CONSOLE_RESOLVE -> processVmConsoleResolveCommand(entry);
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

        String sourceImagePath = null;
        List<IsoAttachment> isoAttachments = new ArrayList<>();
        try {
            if (vm.getContentItemId() != null) {
                ContentItemEntity tpl = contentItemRepository.findById(vm.getContentItemId())
                        .orElseThrow(() -> new IllegalStateException("Template content item not found: " + vm.getContentItemId()));
                ContentItemResolution.assertAvailableTemplate(tpl);
                sourceImagePath = tpl.getProviderRelativePath();
            }
            Object rawIsos = entry.payload() != null ? entry.payload().get("isoContentItemIds") : null;
            if (rawIsos instanceof List<?> list) {
                for (Object o : list) {
                    if (o == null) {
                        continue;
                    }
                    UUID isoId = UUID.fromString(o.toString());
                    ContentItemEntity isoItem = contentItemRepository.findById(isoId)
                            .orElseThrow(() -> new IllegalStateException("ISO content item not found: " + isoId));
                    ContentItemResolution.assertAvailableIso(isoItem);
                    isoAttachments.add(ContentItemResolution.toIsoAttachment(isoItem));
                }
            }
        } catch (Exception e) {
            logger.error("Content library resolution failed for VM {}: {}", vmId, e.getMessage());
            commandQueue.markFailed(entry.id(), e.getMessage());
            vm.setStatus(VmStatus.ERROR);
            vm.setUpdatedAt(Instant.now());
            vmRepository.save(vm);
            return;
        }

        VmCreationRequest createRequest = VmCreationRequest.builder()
                .vmId(vmId)
                .spec(specJson)
                .providerContext(contextOpt.get())
                .metadata(null)
                .correlationId(requestId)
                .sourceImagePath(sourceImagePath)
                .isoAttachments(isoAttachments)
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

    private void processVmAttachIsoCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));
        if (vm.getExternalId() == null || vm.getExternalId().isBlank()) {
            commandQueue.markFailed(entry.id(), "VM has no external ID");
            return;
        }
        Object raw = entry.payload() != null ? entry.payload().get("contentItemId") : null;
        if (raw == null) {
            commandQueue.markFailed(entry.id(), "Missing contentItemId");
            return;
        }
        UUID isoItemId = UUID.fromString(raw.toString());
        ContentItemEntity isoItem = contentItemRepository.findById(isoItemId)
                .orElse(null);
        if (isoItem == null) {
            commandQueue.markFailed(entry.id(), "ISO content item not found");
            return;
        }
        try {
            ContentItemResolution.assertAvailableIso(isoItem);
        } catch (Exception e) {
            commandQueue.markFailed(entry.id(), e.getMessage());
            return;
        }
        IsoAttachment att = ContentItemResolution.toIsoAttachment(isoItem);
        Optional<ProviderContext> contextOpt = providerRegistry.createContextForTenantDatacenter(vm.getTenantDatacenterGrantId());
        if (contextOpt.isEmpty()) {
            commandQueue.markFailed(entry.id(), "Failed to create provider context");
            return;
        }
        VmProvider provider = providerRegistry.resolveProviderForTenantDatacenter(vm.getTenantDatacenterGrantId())
                .orElse(null);
        if (provider == null) {
            commandQueue.markFailed(entry.id(), "No provider available");
            return;
        }
        String requestId = entry.metadata() != null && entry.metadata().get("requestId") != null
                ? entry.metadata().get("requestId")
                : entry.id().toString();
        VmIsoAttachProviderRequest req = new VmIsoAttachProviderRequest(
                vmId,
                vm.getExternalId(),
                att.isoPath(),
                att.deviceName(),
                att.bootable(),
                contextOpt.get(),
                requestId);
        VmOperationResult result = provider.attachIso(req).join();
        if (result.resultType() == VmOperationResult.ResultType.SUCCESS) {
            commandQueue.markCompleted(entry.id());
        } else {
            commandQueue.markFailed(entry.id(), result.message() != null ? result.message() : "attachIso failed");
        }
    }

    private void processVmDetachIsoCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));
        if (vm.getExternalId() == null || vm.getExternalId().isBlank()) {
            commandQueue.markFailed(entry.id(), "VM has no external ID");
            return;
        }
        Object raw = entry.payload() != null ? entry.payload().get("deviceName") : null;
        if (raw == null || raw.toString().isBlank()) {
            commandQueue.markFailed(entry.id(), "Missing deviceName");
            return;
        }
        Optional<ProviderContext> contextOpt = providerRegistry.createContextForTenantDatacenter(vm.getTenantDatacenterGrantId());
        if (contextOpt.isEmpty()) {
            commandQueue.markFailed(entry.id(), "Failed to create provider context");
            return;
        }
        VmProvider provider = providerRegistry.resolveProviderForTenantDatacenter(vm.getTenantDatacenterGrantId())
                .orElse(null);
        if (provider == null) {
            commandQueue.markFailed(entry.id(), "No provider available");
            return;
        }
        String requestId = entry.metadata() != null && entry.metadata().get("requestId") != null
                ? entry.metadata().get("requestId")
                : entry.id().toString();
        VmIsoDetachProviderRequest req = new VmIsoDetachProviderRequest(
                vmId,
                vm.getExternalId(),
                raw.toString(),
                contextOpt.get(),
                requestId);
        VmOperationResult result = provider.detachIso(req).join();
        if (result.resultType() == VmOperationResult.ResultType.SUCCESS) {
            commandQueue.markCompleted(entry.id());
        } else {
            commandQueue.markFailed(entry.id(), result.message() != null ? result.message() : "detachIso failed");
        }
    }

    private void processVmPublishTemplateCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found: " + vmId));
        if (vm.getExternalId() == null || vm.getExternalId().isBlank()) {
            commandQueue.markFailed(entry.id(), "VM has no external ID");
            return;
        }
        Object raw = entry.payload() != null ? entry.payload().get("contentItemId") : null;
        if (raw == null) {
            commandQueue.markFailed(entry.id(), "Missing contentItemId");
            return;
        }
        UUID contentItemId = UUID.fromString(raw.toString());
        ContentItemEntity item = contentItemRepository.findById(contentItemId).orElse(null);
        if (item == null) {
            commandQueue.markFailed(entry.id(), "Content item not found");
            return;
        }
        if (item.getProviderRelativePath() == null || item.getProviderRelativePath().isBlank()) {
            commandQueue.markFailed(entry.id(), "Content item has no provider path");
            return;
        }
        Optional<ProviderContext> contextOpt = providerRegistry.createContextForTenantDatacenter(vm.getTenantDatacenterGrantId());
        if (contextOpt.isEmpty()) {
            commandQueue.markFailed(entry.id(), "Failed to create provider context");
            return;
        }
        VmProvider provider = providerRegistry.resolveProviderForTenantDatacenter(vm.getTenantDatacenterGrantId())
                .orElse(null);
        if (provider == null) {
            commandQueue.markFailed(entry.id(), "No provider available");
            return;
        }
        String requestId = entry.metadata() != null && entry.metadata().get("requestId") != null
                ? entry.metadata().get("requestId")
                : entry.id().toString();
        VmTemplateExportRequest exportReq = new VmTemplateExportRequest(
                vm.getExternalId(),
                item.getProviderRelativePath(),
                item.getName(),
                contextOpt.get(),
                requestId);
        VmTemplateExportResult result = provider.cloneVmAsTemplate(exportReq).join();
        if (result.resultType() == VmTemplateExportResult.ResultType.SUCCESS) {
            item.setContentStatus("available");
            if (result.sizeBytes() != null) {
                item.setSizeBytes(result.sizeBytes());
            }
            contentItemRepository.save(item);
            commandQueue.markCompleted(entry.id());
        } else {
            item.setContentStatus("failed");
            contentItemRepository.save(item);
            String msg = result.error() != null && result.error().message() != null
                    ? result.error().message()
                    : "Template export failed";
            commandQueue.markFailed(entry.id(), msg);
        }
    }

    private void processVmConsoleResolveCommand(CommandMessage entry) {
        Map<String, Object> payload = entry.payload();
        if (payload == null || payload.isEmpty()) {
            commandQueue.markFailed(entry.id(), "Missing console resolve payload");
            return;
        }
        Object grantObj = payload.get(VmConsoleResolvePayloadKeys.TENANT_DATACENTER_GRANT_ID);
        if (grantObj == null) {
            commandQueue.markFailed(entry.id(), "Missing tenantDatacenterGrantId");
            return;
        }
        UUID grantId;
        try {
            grantId = UUID.fromString(grantObj.toString());
        } catch (IllegalArgumentException e) {
            commandQueue.markFailed(entry.id(), "Invalid tenantDatacenterGrantId");
            return;
        }

        UUID vmId = entry.entityId();
        String externalId = Optional.ofNullable(payload.get(VmConsoleResolvePayloadKeys.EXTERNAL_ID))
                .map(Object::toString)
                .orElse("");
        String nodeIdStr = Optional.ofNullable(payload.get(VmConsoleResolvePayloadKeys.NODE_ID))
                .map(Object::toString)
                .orElse("");
        UUID nodeId = null;
        if (!nodeIdStr.isBlank()) {
            try {
                nodeId = UUID.fromString(nodeIdStr);
            } catch (IllegalArgumentException e) {
                commandQueue.markFailed(entry.id(), "Invalid nodeId");
                return;
            }
        }

        Optional<VmProvider> providerOpt = providerRegistry.resolveProviderForTenantDatacenter(grantId);
        if (providerOpt.isEmpty()) {
            commandQueue.markFailed(entry.id(), "No infrastructure provider is configured for this VM");
            return;
        }

        VmConsoleRequest consoleRequest = new VmConsoleRequest(vmId, grantId, externalId, nodeId);
        VmConsoleConnectionInfo info;
        try {
            info = providerOpt.get().getConsoleConnection(consoleRequest).join();
        } catch (Exception e) {
            Throwable c = e.getCause() != null ? e.getCause() : e;
            logger.warn("Console resolution failed for vm {}: {}", vmId, c.getMessage());
            commandQueue.markFailed(entry.id(), c.getMessage() != null ? c.getMessage() : "Provider error");
            return;
        }

        Map<String, Object> result = new HashMap<>(payload);
        result.put(VmConsoleResolvePayloadKeys.RESOLVED, true);
        result.put(VmConsoleResolvePayloadKeys.CONSOLE_TYPE, info.consoleType().name());
        result.put(VmConsoleResolvePayloadKeys.HOST, info.host());
        result.put(VmConsoleResolvePayloadKeys.PORT, info.port());
        result.put(VmConsoleResolvePayloadKeys.TLS, info.tls());
        if (info.password() != null) {
            result.put(VmConsoleResolvePayloadKeys.PASSWORD, info.password());
        }
        persistQueuePayloadAndMarkCompleted(entry.id(), result);
    }

    private void persistQueuePayloadAndMarkCompleted(UUID commandId, Map<String, Object> newPayload) {
        queueEntryRepository.findById(commandId).ifPresentOrElse(entry -> {
            entry.setPayload(newPayload);
            entry.setUpdatedAt(Instant.now());
            queueEntryRepository.save(entry);
        }, () -> {
            throw new IllegalStateException("Queue entry not found: " + commandId);
        });
        commandQueue.markCompleted(commandId);
    }
}
