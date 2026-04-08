package com.onetattva.infron.core.orch.worker;

import com.onetattva.infron.api.model.EntityType;
import com.onetattva.infron.core.orch.ContentItemResolution;
import com.onetattva.infron.core.providers.*;
import com.onetattva.infron.core.spi.queue.*;
import com.onetattva.infron.core.spi.queue.EntityEventQueue.EntityEventTypes;
import com.onetattva.infron.core.spi.queue.TaskEventQueue.TaskEventTypes;
import com.onetattva.infron.db.model.ContentItemEntity;
import com.onetattva.infron.db.model.QueueEntryEntity;
import com.onetattva.infron.db.model.VmEntity;
import com.onetattva.infron.db.repository.ContentItemRepository;
import com.onetattva.infron.db.repository.QueueEntryRepository;
import com.onetattva.infron.db.repository.VmRepository;
import com.onetattva.infron.core.orch.wiring.TenantAwareVmProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Executes VM tasks claimed from the {@link CommandQueue}.
 *
 * <p><strong>Dependency rules for the {@code worker/} module:</strong>
 * <ul>
 *   <li>{@link VmRepository} — READ-ONLY (current state checks). No {@code save()} calls.
 *   <li>{@link ContentItemRepository} — READ-ONLY for path resolution. {@code save()} only for
 *       content-item status after template export (to be replaced with entity event in a future
 *       iteration once {@code EntityEventProcessor} handles content item events).
 *   <li>{@link QueueEntryRepository} — used only for the console-resolve special case (result
 *       written back into the command payload so core-services can poll for it).
 *   <li>No {@code JobRepository} access whatsoever.
 * </ul>
 *
 * <p>All VM entity state changes are communicated through {@link EntityEventQueue}.
 * Task lifecycle (started / completed / failed) is communicated through {@link TaskEventQueue}.
 */
@Service
public class VmTaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(VmTaskExecutor.class);

    @Autowired private CommandQueue commandQueue;
    @Autowired private TaskEventQueue taskEventQueue;
    @Autowired private EntityEventQueue entityEventQueue;

    // READ-ONLY: current state queries only
    @Autowired private VmRepository vmRepository;
    @Autowired private ContentItemRepository contentItemRepository;
    @Autowired private QueueEntryRepository queueEntryRepository;

    @Autowired private TenantAwareVmProviderRegistry providerRegistry;

    public void execute(CommandMessage entry) {
        String queueType = entry.queueType();
        try {
            publishTaskStarted(entry);
            switch (queueType) {
                case "VM_CREATE_COMMAND"          -> processVmCreateCommand(entry);
                case "VM_START_COMMAND"           -> processVmStartCommand(entry);
                case "VM_STOP_COMMAND"            -> processVmStopCommand(entry);
                case "VM_RESTART_COMMAND"         -> processVmRestartCommand(entry);
                case "VM_SUSPEND_COMMAND"         -> processVmSuspendCommand(entry);
                case "VM_RESUME_COMMAND"          -> processVmResumeCommand(entry);
                case "VM_DELETE_COMMAND"          -> processVmDeleteCommand(entry);
                case "VM_ATTACH_ISO_COMMAND"      -> processVmAttachIsoCommand(entry);
                case "VM_DETACH_ISO_COMMAND"      -> processVmDetachIsoCommand(entry);
                case "VM_PUBLISH_TEMPLATE_COMMAND"-> processVmPublishTemplateCommand(entry);
                case "VM_MIGRATE_COMMAND"         -> processVmMigrateCommand(entry);
                case VmQueueCommands.CONSOLE_RESOLVE -> processVmConsoleResolveCommand(entry);
                default -> {
                    log.warn("Unknown VM queue type: {}", queueType);
                    failTask(entry, "Unknown queue type: " + queueType);
                }
            }
        } catch (Exception e) {
            log.error("Error executing task {} ({}): {}", entry.id(), queueType, e.getMessage(), e);
            failTask(entry, e.getMessage());
        }
    }

    // ── VM Create ──────────────────────────────────────────────────────────────

    @Transactional
    protected void processVmCreateCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = requireVm(vmId, entry);
        if (vm == null) return;

        Optional<ProviderContext> contextOpt = providerRegistry.createContextForTenantDatacenter(vm.getTenantDatacenterGrantId());
        if (contextOpt.isEmpty()) {
            failTask(entry, "Failed to create provider context");
            return;
        }
        VmProvider provider = providerRegistry.resolveProviderForTenantDatacenter(vm.getTenantDatacenterGrantId())
                .orElse(null);
        if (provider == null) {
            failTask(entry, "No provider available for datacenter");
            return;
        }

        // Resolve spec — prefer payload (already enriched by gRPC request), fallback to VM entity
        String specJson = vm.getSpec();
        if (specJson == null || specJson.isEmpty()) {
            Object specPayload = entry.payload() != null ? entry.payload().get("specJson") : null;
            specJson = specPayload instanceof String s ? s : "{}";
        }

        String sourceImagePath = null;
        List<IsoAttachment> isoAttachments = new ArrayList<>();
        try {
            // Prefer sourceImagePath from payload (passed by gRPC request)
            Object payloadPath = entry.payload() != null ? entry.payload().get("sourceImagePath") : null;
            if (payloadPath != null && !payloadPath.toString().isBlank()) {
                sourceImagePath = payloadPath.toString();
            } else if (vm.getContentItemId() != null) {
                ContentItemEntity tpl = contentItemRepository.findById(vm.getContentItemId())
                        .orElseThrow(() -> new IllegalStateException("Template not found: " + vm.getContentItemId()));
                ContentItemResolution.assertAvailableTemplate(tpl);
                sourceImagePath = tpl.getProviderRelativePath();
            }

            Object rawIsos = entry.payload() != null ? entry.payload().get("isoContentItemIds") : null;
            if (rawIsos instanceof List<?> list) {
                for (Object o : list) {
                    if (o == null) continue;
                    UUID isoId = UUID.fromString(o.toString());
                    ContentItemEntity isoItem = contentItemRepository.findById(isoId)
                            .orElseThrow(() -> new IllegalStateException("ISO not found: " + isoId));
                    ContentItemResolution.assertAvailableIso(isoItem);
                    isoAttachments.add(ContentItemResolution.toIsoAttachment(isoItem));
                }
            }
        } catch (Exception e) {
            log.error("Content resolution failed for VM {}: {}", vmId, e.getMessage());
            publishEntityEvent(EntityEventTypes.VM_CREATION_FAILED, vmId, entry.id(),
                    Map.of("error_code", "CONTENT_RESOLUTION_FAILED", "message", e.getMessage()));
            failTask(entry, e.getMessage());
            return;
        }

        String requestId = correlationId(entry);
        VmCreationRequest createRequest = VmCreationRequest.builder()
                .vmId(vmId).spec(specJson).providerContext(contextOpt.get())
                .correlationId(requestId).sourceImagePath(sourceImagePath)
                .isoAttachments(isoAttachments).build();

        log.info("Creating VM {} with provider {}", vmId, provider.id());
        VmCreationResult result = provider.createVm(createRequest).join();

        if (result.resultType() == VmCreationResult.ResultType.SUCCESS) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("external_id", result.externalVmId());
            if (result.vmInfo() != null) {
                if (result.vmInfo().ipAddresses() != null) payload.put("ip_addresses", result.vmInfo().ipAddresses());
                if (result.vmInfo().hostname() != null) payload.put("hostname", result.vmInfo().hostname());
            }
            if (provider.id() != null) payload.put("provider_id", provider.id());
            publishEntityEvent(EntityEventTypes.VM_CREATED, vmId, entry.id(), payload);
            completeTask(entry);
        } else {
            String errorMsg = result.message() != null ? result.message()
                    : (result.error() != null ? result.error().message() : "VM creation failed");
            publishEntityEvent(EntityEventTypes.VM_CREATION_FAILED, vmId, entry.id(),
                    Map.of("error_code", "PROVIDER_ERROR", "message", errorMsg));
            failTask(entry, errorMsg);
        }
    }

    // ── VM Start ──────────────────────────────────────────────────────────────

    @Transactional
    protected void processVmStartCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = requireVm(vmId, entry);
        if (vm == null) return;

        VmProvider provider = resolveProvider(vm, entry);
        if (provider == null) return;

        VmOperationRequest opRequest = VmOperationRequest.builder()
                .vmId(vmId).externalVmId(vm.getExternalId()).correlationId(correlationId(entry)).build();
        VmOperationResult result = provider.startVm(opRequest).join();

        if (result.resultType() == VmOperationResult.ResultType.SUCCESS) {
            publishEntityEvent(EntityEventTypes.VM_POWER_ON, vmId, entry.id(), Map.of());
            completeTask(entry);
        } else {
            publishEntityEvent(EntityEventTypes.VM_OPERATION_FAILED, vmId, entry.id(),
                    Map.of("operation", "START", "message", safeMessage(result)));
            failTask(entry, safeMessage(result));
        }
    }

    // ── VM Stop ───────────────────────────────────────────────────────────────

    @Transactional
    protected void processVmStopCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = requireVm(vmId, entry);
        if (vm == null) return;

        VmProvider provider = resolveProvider(vm, entry);
        if (provider == null) return;

        VmOperationRequest opRequest = VmOperationRequest.builder()
                .vmId(vmId).externalVmId(vm.getExternalId()).correlationId(correlationId(entry)).build();
        VmOperationResult result = provider.stopVm(opRequest).join();

        if (result.resultType() == VmOperationResult.ResultType.SUCCESS) {
            publishEntityEvent(EntityEventTypes.VM_POWER_OFF, vmId, entry.id(), Map.of());
            completeTask(entry);
        } else {
            publishEntityEvent(EntityEventTypes.VM_OPERATION_FAILED, vmId, entry.id(),
                    Map.of("operation", "STOP", "message", safeMessage(result)));
            failTask(entry, safeMessage(result));
        }
    }

    // ── VM Restart ────────────────────────────────────────────────────────────

    @Transactional
    protected void processVmRestartCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = requireVm(vmId, entry);
        if (vm == null) return;

        VmProvider provider = resolveProvider(vm, entry);
        if (provider == null) return;

        VmOperationRequest opRequest = VmOperationRequest.builder()
                .vmId(vmId).externalVmId(vm.getExternalId()).correlationId(correlationId(entry)).build();
        VmOperationResult result = provider.restartVm(opRequest).join();

        if (result.resultType() == VmOperationResult.ResultType.SUCCESS) {
            publishEntityEvent(EntityEventTypes.VM_POWER_ON, vmId, entry.id(), Map.of("restarted", "true"));
            completeTask(entry);
        } else {
            publishEntityEvent(EntityEventTypes.VM_OPERATION_FAILED, vmId, entry.id(),
                    Map.of("operation", "RESTART", "message", safeMessage(result)));
            failTask(entry, safeMessage(result));
        }
    }

    // ── VM Suspend ────────────────────────────────────────────────────────────

    @Transactional
    protected void processVmSuspendCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = requireVm(vmId, entry);
        if (vm == null) return;

        VmProvider provider = resolveProvider(vm, entry);
        if (provider == null) return;

        VmOperationRequest opRequest = VmOperationRequest.builder()
                .vmId(vmId).externalVmId(vm.getExternalId()).correlationId(correlationId(entry)).build();
        VmOperationResult result = provider.suspendVm(opRequest).join();

        if (result.resultType() == VmOperationResult.ResultType.SUCCESS) {
            publishEntityEvent(EntityEventTypes.VM_SUSPENDED, vmId, entry.id(), Map.of());
            completeTask(entry);
        } else {
            publishEntityEvent(EntityEventTypes.VM_OPERATION_FAILED, vmId, entry.id(),
                    Map.of("operation", "SUSPEND", "message", safeMessage(result)));
            failTask(entry, safeMessage(result));
        }
    }

    // ── VM Resume ─────────────────────────────────────────────────────────────

    @Transactional
    protected void processVmResumeCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = requireVm(vmId, entry);
        if (vm == null) return;

        VmProvider provider = resolveProvider(vm, entry);
        if (provider == null) return;

        VmOperationRequest opRequest = VmOperationRequest.builder()
                .vmId(vmId).externalVmId(vm.getExternalId()).correlationId(correlationId(entry)).build();
        VmOperationResult result = provider.resumeVm(opRequest).join();

        if (result.resultType() == VmOperationResult.ResultType.SUCCESS) {
            publishEntityEvent(EntityEventTypes.VM_POWER_ON, vmId, entry.id(), Map.of("resumed", "true"));
            completeTask(entry);
        } else {
            publishEntityEvent(EntityEventTypes.VM_OPERATION_FAILED, vmId, entry.id(),
                    Map.of("operation", "RESUME", "message", safeMessage(result)));
            failTask(entry, safeMessage(result));
        }
    }

    // ── VM Delete ─────────────────────────────────────────────────────────────

    @Transactional
    protected void processVmDeleteCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = requireVm(vmId, entry);
        if (vm == null) return;

        VmProvider provider = resolveProvider(vm, entry);
        if (provider == null) return;

        VmDeletionRequest delRequest = VmDeletionRequest.builder()
                .vmId(vmId).correlationId(correlationId(entry)).build();
        VmDeletionResult result = provider.deleteVm(delRequest).join();

        if (result.resultType() == VmDeletionResult.ResultType.SUCCESS) {
            publishEntityEvent(EntityEventTypes.VM_DELETED, vmId, entry.id(), Map.of());
            completeTask(entry);
        } else {
            String msg = result.message() != null ? result.message() : "VM deletion failed";
            publishEntityEvent(EntityEventTypes.VM_DELETION_FAILED, vmId, entry.id(),
                    Map.of("error_code", "PROVIDER_ERROR", "message", msg));
            failTask(entry, msg);
        }
    }

    // ── VM Migrate ────────────────────────────────────────────────────────────

    @Transactional
    protected void processVmMigrateCommand(CommandMessage entry) {
        // Placeholder — migration requires provider-level support (e.g. live migration).
        // Emit operation.failed for now so the job status reflects correctly.
        UUID vmId = entry.entityId();
        publishEntityEvent(EntityEventTypes.VM_OPERATION_FAILED, vmId, entry.id(),
                Map.of("operation", "MIGRATE", "message", "Migration not yet implemented by provider"));
        failTask(entry, "Migration not yet implemented");
    }

    // ── Attach / Detach ISO ───────────────────────────────────────────────────

    @Transactional
    protected void processVmAttachIsoCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = requireVm(vmId, entry);
        if (vm == null) return;

        Object rawIsoId = entry.payload() != null ? entry.payload().get("isoContentItemId") : null;
        if (rawIsoId == null) { failTask(entry, "Missing isoContentItemId"); return; }
        ContentItemEntity isoItem = contentItemRepository.findById(UUID.fromString(rawIsoId.toString())).orElse(null);
        if (isoItem == null) { failTask(entry, "ISO content item not found"); return; }
        try { ContentItemResolution.assertAvailableIso(isoItem); }
        catch (Exception e) { failTask(entry, e.getMessage()); return; }

        Optional<ProviderContext> contextOpt = providerRegistry.createContextForTenantDatacenter(vm.getTenantDatacenterGrantId());
        if (contextOpt.isEmpty()) { failTask(entry, "Failed to create provider context"); return; }
        VmProvider provider = resolveProvider(vm, entry);
        if (provider == null) return;

        IsoAttachment att = ContentItemResolution.toIsoAttachment(isoItem);
        VmIsoAttachProviderRequest req = new VmIsoAttachProviderRequest(
                vmId, vm.getExternalId(), att.isoPath(), att.deviceName(), att.bootable(),
                contextOpt.get(), correlationId(entry));
        VmOperationResult result = provider.attachIso(req).join();
        if (result.resultType() == VmOperationResult.ResultType.SUCCESS) {
            completeTask(entry);
        } else {
            failTask(entry, result.message() != null ? result.message() : "attachIso failed");
        }
    }

    @Transactional
    protected void processVmDetachIsoCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = requireVm(vmId, entry);
        if (vm == null) return;

        Object rawDevice = entry.payload() != null ? entry.payload().get("deviceName") : null;
        if (rawDevice == null || rawDevice.toString().isBlank()) { failTask(entry, "Missing deviceName"); return; }

        Optional<ProviderContext> contextOpt = providerRegistry.createContextForTenantDatacenter(vm.getTenantDatacenterGrantId());
        if (contextOpt.isEmpty()) { failTask(entry, "Failed to create provider context"); return; }
        VmProvider provider = resolveProvider(vm, entry);
        if (provider == null) return;

        VmIsoDetachProviderRequest req = new VmIsoDetachProviderRequest(
                vmId, vm.getExternalId(), rawDevice.toString(), contextOpt.get(), correlationId(entry));
        VmOperationResult result = provider.detachIso(req).join();
        if (result.resultType() == VmOperationResult.ResultType.SUCCESS) {
            completeTask(entry);
        } else {
            failTask(entry, result.message() != null ? result.message() : "detachIso failed");
        }
    }

    // ── Publish VM Template ───────────────────────────────────────────────────

    @Transactional
    protected void processVmPublishTemplateCommand(CommandMessage entry) {
        UUID vmId = entry.entityId();
        VmEntity vm = requireVm(vmId, entry);
        if (vm == null) return;

        Object rawCi = entry.payload() != null ? entry.payload().get("contentItemId") : null;
        if (rawCi == null) { failTask(entry, "Missing contentItemId"); return; }
        ContentItemEntity item = contentItemRepository.findById(UUID.fromString(rawCi.toString())).orElse(null);
        if (item == null) { failTask(entry, "Content item not found"); return; }
        if (item.getProviderRelativePath() == null || item.getProviderRelativePath().isBlank()) {
            failTask(entry, "Content item has no provider path"); return;
        }

        Optional<ProviderContext> contextOpt = providerRegistry.createContextForTenantDatacenter(vm.getTenantDatacenterGrantId());
        if (contextOpt.isEmpty()) { failTask(entry, "Failed to create provider context"); return; }
        VmProvider provider = resolveProvider(vm, entry);
        if (provider == null) return;

        VmTemplateExportRequest exportReq = new VmTemplateExportRequest(
                vm.getExternalId(), item.getProviderRelativePath(), item.getName(),
                contextOpt.get(), correlationId(entry));
        VmTemplateExportResult result = provider.cloneVmAsTemplate(exportReq).join();

        if (result.resultType() == VmTemplateExportResult.ResultType.SUCCESS) {
            // Content item status update — still via direct write (content entity events are a future iteration).
            item.setContentStatus("available");
            if (result.sizeBytes() != null) item.setSizeBytes(result.sizeBytes());
            contentItemRepository.save(item);
            completeTask(entry);
        } else {
            item.setContentStatus("failed");
            contentItemRepository.save(item);
            String msg = result.error() != null && result.error().message() != null
                    ? result.error().message() : "Template export failed";
            failTask(entry, msg);
        }
    }

    // ── Console Resolve (special: writes result back into command payload) ────

    @Transactional
    protected void processVmConsoleResolveCommand(CommandMessage entry) {
        Map<String, Object> payload = entry.payload();
        if (payload == null || payload.isEmpty()) { failTask(entry, "Missing console payload"); return; }

        Object grantObj = payload.get(VmConsoleResolvePayloadKeys.TENANT_DATACENTER_GRANT_ID);
        if (grantObj == null) { failTask(entry, "Missing tenantDatacenterGrantId"); return; }
        UUID grantId;
        try { grantId = UUID.fromString(grantObj.toString()); }
        catch (IllegalArgumentException e) { failTask(entry, "Invalid tenantDatacenterGrantId"); return; }

        UUID vmId = entry.entityId();
        String externalId = Optional.ofNullable(payload.get(VmConsoleResolvePayloadKeys.EXTERNAL_ID))
                .map(Object::toString).orElse("");
        UUID nodeId = null;
        Object rawNode = payload.get(VmConsoleResolvePayloadKeys.NODE_ID);
        if (rawNode != null && !rawNode.toString().isBlank()) {
            try { nodeId = UUID.fromString(rawNode.toString()); }
            catch (IllegalArgumentException e) { failTask(entry, "Invalid nodeId"); return; }
        }

        VmProvider provider = providerRegistry.resolveProviderForTenantDatacenter(grantId).orElse(null);
        if (provider == null) { failTask(entry, "No provider for this VM"); return; }

        VmConsoleConnectionInfo info;
        try {
            info = provider.getConsoleConnection(new VmConsoleRequest(vmId, grantId, externalId, nodeId)).join();
        } catch (Exception e) {
            Throwable c = e.getCause() != null ? e.getCause() : e;
            failTask(entry, c.getMessage() != null ? c.getMessage() : "Provider error");
            return;
        }

        // Write resolved connection info back into the queue entry payload so core-services can read it.
        Map<String, Object> result = new HashMap<>(payload);
        result.put(VmConsoleResolvePayloadKeys.RESOLVED, true);
        result.put(VmConsoleResolvePayloadKeys.CONSOLE_TYPE, info.consoleType().name());
        result.put(VmConsoleResolvePayloadKeys.HOST, info.host());
        result.put(VmConsoleResolvePayloadKeys.PORT, info.port());
        result.put(VmConsoleResolvePayloadKeys.TLS, info.tls());
        if (info.password() != null) result.put(VmConsoleResolvePayloadKeys.PASSWORD, info.password());

        queueEntryRepository.findById(entry.id()).ifPresent(qe -> {
            qe.setPayload(result);
            qe.setUpdatedAt(Instant.now());
            queueEntryRepository.save(qe);
        });
        commandQueue.markCompleted(entry.id());
        // No task event for console resolve — it is a synchronous polling flow handled in VmsService.
    }

    // ── Task lifecycle helpers ─────────────────────────────────────────────────

    private void publishTaskStarted(CommandMessage entry) {
        try {
            taskEventQueue.publishTaskEvent(entry.id(), TaskEventTypes.STARTED,
                    buildTaskPayload(entry, Map.of()));
        } catch (Exception e) {
            log.warn("Failed to publish task.started for {}: {}", entry.id(), e.getMessage());
        }
    }

    private void completeTask(CommandMessage entry) {
        commandQueue.markCompleted(entry.id());
        taskEventQueue.publishTaskEvent(entry.id(), TaskEventTypes.COMPLETED,
                buildTaskPayload(entry, Map.of()));
    }

    private void failTask(CommandMessage entry, String errorMessage) {
        commandQueue.markFailed(entry.id(), errorMessage);
        taskEventQueue.publishTaskEvent(entry.id(), TaskEventTypes.FAILED,
                buildTaskPayload(entry, Map.of("errorMessage", errorMessage != null ? errorMessage : "unknown")));
    }

    private void publishEntityEvent(String eventType, UUID entityId, UUID taskId, Map<String, Object> payload) {
        try {
            entityEventQueue.publishEntityEvent(EntityType.VM, entityId, eventType, taskId, payload);
        } catch (Exception e) {
            log.error("Failed to publish entity event {} for VM {}: {}", eventType, entityId, e.getMessage());
        }
    }

    private Map<String, Object> buildTaskPayload(CommandMessage entry, Map<String, Object> extra) {
        Map<String, Object> p = new HashMap<>(extra);
        Object jobId = entry.payload() != null ? entry.payload().get("jobId") : null;
        if (jobId != null) p.put("jobId", jobId);
        return p;
    }

    // ── Entity / provider resolution helpers ──────────────────────────────────

    private VmEntity requireVm(UUID vmId, CommandMessage entry) {
        VmEntity vm = vmRepository.findById(vmId).orElse(null);
        if (vm == null) {
            failTask(entry, "VM not found: " + vmId);
        }
        return vm;
    }

    private VmProvider resolveProvider(VmEntity vm, CommandMessage entry) {
        VmProvider provider = providerRegistry.resolveProviderForTenantDatacenter(vm.getTenantDatacenterGrantId()).orElse(null);
        if (provider == null) { failTask(entry, "No provider available"); }
        return provider;
    }

    private String correlationId(CommandMessage entry) {
        if (entry.metadata() != null && entry.metadata().get("requestId") != null) {
            return entry.metadata().get("requestId");
        }
        return entry.correlationId() != null ? entry.correlationId() : entry.id().toString();
    }

    private String safeMessage(VmOperationResult result) {
        return result.message() != null ? result.message() : "Operation failed";
    }
}
