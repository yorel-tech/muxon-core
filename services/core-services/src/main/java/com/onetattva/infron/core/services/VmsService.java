package com.onetattva.infron.core.services;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.common.EntityNotFoundException;
import com.onetattva.infron.api.model.EntityType;
import tools.jackson.databind.ObjectMapper;
import com.onetattva.infron.core.auth.AuthorizationService;
import com.onetattva.infron.core.auth.Permission;
import com.onetattva.infron.core.auth.UserPrincipal;
import com.onetattva.infron.core.common.Constants;
import com.onetattva.infron.core.config.InfronConsoleProperties;
import com.onetattva.infron.api.enums.QueueStatus;
import com.onetattva.infron.core.providers.VmConsoleType;
import com.onetattva.infron.core.spi.queue.CommandMessage;
import com.onetattva.infron.core.spi.queue.CommandQueue;
import com.onetattva.infron.core.spi.queue.VmConsoleResolvePayloadKeys;
import com.onetattva.infron.core.spi.queue.VmQueueCommands;
import com.onetattva.infron.grpc.workflow.v1.*;
import io.grpc.StatusRuntimeException;
import com.onetattva.infron.db.model.ConsoleSessionConsoleType;
import com.onetattva.infron.db.model.ConsoleSessionEntity;
import com.onetattva.infron.db.model.ConsoleSessionStatus;
import com.onetattva.infron.db.model.SystemSettingsEntity;
import com.onetattva.infron.db.model.TenantDatacenterGrantEntity;
import com.onetattva.infron.db.model.UserRoleBindingViewEntity;
import com.onetattva.infron.db.model.QueueEntryEntity;
import com.onetattva.infron.db.model.VmEntity;
import com.onetattva.infron.db.model.ContentItemEntity;
import com.onetattva.infron.db.model.ContentLibraryEntity;
import com.onetattva.infron.db.repository.*;
import com.onetattva.infron.core.services.content.ContentLibraryProviderPathBuilder;
import com.onetattva.infron.core.services.content.ContentLibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class VmsService {

    private static final Logger log = LoggerFactory.getLogger(VmsService.class);

    @Autowired
    private VmRepository vmRepository;
    @Autowired
    private ComputeProfileRepository computeProfileRepository;
    @Autowired
    private TenantDatacenterGrantRepository tenantDatacenterGrantRepository;
    @Autowired
    private CommandQueue commandQueue;   // kept for console-resolve polling flow
    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private VMWorkflowServiceGrpc.VMWorkflowServiceBlockingStub vmWorkflowStub;
    @Autowired
    private UserRoleBindingViewRepository userRoleBindingViewRepository;
    @Autowired
    private IdpUserRepository idpUserRepository;
    @Autowired
    private ContentItemRepository contentItemRepository;
    @Autowired
    private ContentLibraryRepository contentLibraryRepository;

    @Autowired
    private ContentLibraryProviderPathBuilder contentLibraryProviderPathBuilder;

    @Autowired
    private ContentLibraryService contentLibraryService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ConsoleSessionRepository consoleSessionRepository;

    @Autowired
    private SystemSettingsRepository systemSettingsRepository;

    @Autowired
    private InfronConsoleProperties infronConsoleProperties;

    public VmCreateResponse createVm(UUID tenantId, VmCreateRequest request) {
        TenantDatacenterGrantEntity grant = tenantDatacenterGrantRepository
                .findByIdAndTenant_Id(request.getTenantDatacenterGrantId(), tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant datacenter grant not found"));

        boolean usesCl = request.getContentItemId() != null
                || (request.getIsoContentItemIds() != null && !request.getIsoContentItemIds().isEmpty());
        if (log.isDebugEnabled()) {
            log.debug(
                    "VM create request: tenantId={}, grantId={}, name={}, usesContentLibrary={}, "
                            + "templateContentItemId={}, isoContentItemCount={}",
                    tenantId,
                    request.getTenantDatacenterGrantId(),
                    request.getName(),
                    usesCl,
                    request.getContentItemId(),
                    request.getIsoContentItemIds() != null ? request.getIsoContentItemIds().size() : 0);
        }
        if (usesCl) {
            requireContentLibraryRead(tenantId);
        }

        ContentItemEntity tpl = null;
        if (request.getContentItemId() != null) {
            tpl = contentItemRepository.findById(request.getContentItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Content item not found: " + request.getContentItemId()));
            assertContentItemReadableByTenant(tenantId, tpl);
            if (!"vm_template".equalsIgnoreCase(tpl.getContentType())) {
                throw new IllegalArgumentException("content_item_id must reference a vm_template item");
            }
            if (!"available".equalsIgnoreCase(tpl.getContentStatus())) {
                throw new IllegalArgumentException(
                        "Template content item must be available in the content store before VM create");
            }
        }

        List<UUID> isoIds = request.getIsoContentItemIds();
        if (isoIds != null) {
            for (UUID isoId : isoIds) {
                ContentItemEntity iso = contentItemRepository.findById(isoId)
                        .orElseThrow(() -> new EntityNotFoundException("Content item not found: " + isoId));
                assertContentItemReadableByTenant(tenantId, iso);
                if (!"iso".equalsIgnoreCase(iso.getContentType())) {
                    throw new IllegalArgumentException("iso_content_item_ids must reference iso items");
                }
                if (!"available".equalsIgnoreCase(iso.getContentStatus())) {
                    throw new IllegalArgumentException(
                            "ISO content item must be available in the content store before VM create");
                }
            }
        }

        // Create VM entity
        VmEntity vm = new VmEntity();
        vm.setTenantDatacenterGrantId(grant.getId());
        vm.setName(request.getName());
        VmSpec finalSpec = request.getSpec();
        if (tpl != null) {
            finalSpec = mergeTemplateIntoVmSpec(tpl, request.getSpec());
        }
        vm.setSpec(vmSpecToJson(finalSpec));
        vm.setStatus(com.onetattva.infron.api.enums.VmStatus.PENDING);
        vm.setPowerState(com.onetattva.infron.api.enums.VmPowerState.UNKNOWN);
        vm.setCreatedAt(Instant.now());
        vm.setUpdatedAt(Instant.now());
        vm.setMetadata(request.getMetadata());
        vm.setTags(request.getTags());
        vm.setContentItemId(request.getContentItemId());
        if (isoIds != null && !isoIds.isEmpty()) {
            vm.setAttachedIsoItemIds(new ArrayList<>(new LinkedHashSet<>(isoIds)));
        }

        // Save VM
        VmEntity saved = vmRepository.save(vm);

        // Dispatch VM creation workflow to orchestrator via gRPC
        CreateVMRequest grpcRequest = buildGrpcCreateRequest(saved, request);
        if (log.isDebugEnabled()) {
            log.debug(
                    "VM create dispatch gRPC: vmId={}, correlationId={}, grantId={}, specJsonChars={}, "
                            + "isoIdsInGrpc={}",
                    saved.getId(),
                    grpcRequest.getCorrelationId(),
                    grpcRequest.getTenantDatacenterGrantId(),
                    grpcRequest.getSpecJson() != null ? grpcRequest.getSpecJson().length() : 0,
                    grpcRequest.getIsoContentIdsList().size());
        }
        JobResponse jobResponse = vmWorkflowStub.createVM(grpcRequest);
        log.debug("VM create orchestrator accepted: vmId={}, jobId={}", saved.getId(), jobResponse.getJobId());

        VmCreateResponse response = new VmCreateResponse();
        response.setId(saved.getId());
        response.setName(saved.getName());
        response.setStatus(VmStatus.valueOf(saved.getStatus().name()));
        response.setMessage("VM creation initiated — job: " + jobResponse.getJobId());
        response.setCreatedAt(saved.getCreatedAt().atOffset(ZoneOffset.UTC));
        return response;
    }

    public VmListResponse listVms(UUID tenantId, Integer page, Integer perPage, VmStatus status,
                                UUID tenantDatacenterGrantId, String tags,
                                String sort, String order) {
        Sort sortSpec = vmListSort(sort, order);
        Pageable pageable = PageRequest.of(page - 1, perPage, sortSpec);
        Page<VmEntity> entityPage;
        if (tenantDatacenterGrantId != null) {
            tenantDatacenterGrantRepository.findByIdAndTenant_Id(tenantDatacenterGrantId, tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Tenant datacenter grant not found"));
            entityPage = vmRepository.findByTenantDatacenterGrantId(tenantDatacenterGrantId, pageable);
        } else {
            entityPage = vmRepository.findAllByTenantId(tenantId, pageable);
        }

        if (status != null) {
            // entityPage = vmRepository.findByStatus(...);
        }

        List<Vm> vms = entityPage.getContent().stream()
                .map(this::mapEntityToApi)
                .toList();

        // Calculate total pages
        int totalPages = (int) Math.ceil((double) entityPage.getTotalElements() / perPage);

        VmListResponse response = new VmListResponse();
        response.setTotal((int) entityPage.getTotalElements());
        response.setPage(page);
        response.setPerPage(perPage);
        response.setTotalPages(totalPages);
        response.setItems(vms);
        return response;
    }

    public Vm getVm(UUID tenantId, UUID vmId) {
        return mapEntityToApi(requireVmForTenant(tenantId, vmId));
    }

    public Vm patchVm(UUID tenantId, UUID vmId, VmUpdateRequest request) {
        VmEntity vm = requireVmForTenant(tenantId, vmId);

        if (request.getDescription() != null) {
            vm.setDescription(request.getDescription());
        }
        if (request.getMetadata() != null) {
            vm.setMetadata(request.getMetadata());
        }
        if (request.getTags() != null) {
            vm.setTags(request.getTags());
        }
        if (request.getContentItemId() != null) {
            vm.setContentItemId(request.getContentItemId());
        }

        vm.setUpdatedAt(Instant.now());
        VmEntity saved = vmRepository.save(vm);

        return mapEntityToApi(saved);
    }

    public VmOperationResponse startVm(UUID tenantId, UUID vmId) {
        VmEntity vm = requireVmForTenant(tenantId, vmId);
        if (vm.getStatus() != com.onetattva.infron.api.enums.VmStatus.STOPPED) {
            throw new IllegalStateException("VM must be stopped to start");
        }
        PowerOnVMRequest grpc = PowerOnVMRequest.newBuilder()
                .setVmId(vm.getId().toString())
                .setExternalId(vm.getExternalId() != null ? vm.getExternalId() : "")
                .setGrantId(vm.getTenantDatacenterGrantId().toString())
                .build();
        log.debug(
                "VM start gRPC: vmId={}, grantId={}, externalIdSet={}",
                vmId,
                vm.getTenantDatacenterGrantId(),
                vm.getExternalId() != null && !vm.getExternalId().isBlank());
        JobResponse job = vmWorkflowStub.powerOnVM(grpc);
        return buildOperationResponse(vmId, "VM start initiated — job: " + job.getJobId());
    }

    public VmOperationResponse stopVm(UUID tenantId, UUID vmId) {
        VmEntity vm = requireVmForTenant(tenantId, vmId);
        if (vm.getStatus() != com.onetattva.infron.api.enums.VmStatus.ACTIVE) {
            throw new IllegalStateException("VM must be running to stop");
        }
        log.debug(
                "VM stop gRPC: vmId={}, grantId={}, externalIdSet={}",
                vmId,
                vm.getTenantDatacenterGrantId(),
                vm.getExternalId() != null && !vm.getExternalId().isBlank());
        JobResponse job = vmWorkflowStub.powerOffVM(PowerOffVMRequest.newBuilder()
                .setVmId(vm.getId().toString())
                .setExternalId(vm.getExternalId() != null ? vm.getExternalId() : "")
                .setGrantId(vm.getTenantDatacenterGrantId().toString())
                .build());
        return buildOperationResponse(vmId, "VM stop initiated — job: " + job.getJobId());
    }

    public VmOperationResponse restartVm(UUID tenantId, UUID vmId) {
        VmEntity vm = requireVmForTenant(tenantId, vmId);
        JobResponse job = vmWorkflowStub.restartVM(RestartVMRequest.newBuilder()
                .setVmId(vm.getId().toString())
                .setExternalId(vm.getExternalId() != null ? vm.getExternalId() : "")
                .setGrantId(vm.getTenantDatacenterGrantId().toString())
                .build());
        return buildOperationResponse(vmId, "VM restart initiated — job: " + job.getJobId());
    }

    public VmOperationResponse suspendVm(UUID tenantId, UUID vmId) {
        VmEntity vm = requireVmForTenant(tenantId, vmId);
        if (vm.getStatus() != com.onetattva.infron.api.enums.VmStatus.ACTIVE) {
            throw new IllegalStateException("VM must be running to suspend");
        }
        JobResponse job = vmWorkflowStub.suspendVM(SuspendVMRequest.newBuilder()
                .setVmId(vm.getId().toString())
                .setExternalId(vm.getExternalId() != null ? vm.getExternalId() : "")
                .setGrantId(vm.getTenantDatacenterGrantId().toString())
                .build());
        return buildOperationResponse(vmId, "VM suspend initiated — job: " + job.getJobId());
    }

    public VmOperationResponse resumeVm(UUID tenantId, UUID vmId) {
        VmEntity vm = requireVmForTenant(tenantId, vmId);
        if (vm.getStatus() != com.onetattva.infron.api.enums.VmStatus.SUSPENDED) {
            throw new IllegalStateException("VM must be suspended to resume");
        }
        JobResponse job = vmWorkflowStub.resumeVM(ResumeVMRequest.newBuilder()
                .setVmId(vm.getId().toString())
                .setExternalId(vm.getExternalId() != null ? vm.getExternalId() : "")
                .setGrantId(vm.getTenantDatacenterGrantId().toString())
                .build());
        return buildOperationResponse(vmId, "VM resume initiated — job: " + job.getJobId());
    }

    public VmOperationResponse deleteVm(UUID tenantId, UUID vmId) {
        VmEntity vm = requireVmForTenant(tenantId, vmId);
        log.debug(
                "VM delete gRPC: vmId={}, grantId={}, externalIdSet={}",
                vmId,
                vm.getTenantDatacenterGrantId(),
                vm.getExternalId() != null && !vm.getExternalId().isBlank());
        JobResponse job = vmWorkflowStub.deleteVM(DeleteVMRequest.newBuilder()
                .setVmId(vm.getId().toString())
                .setExternalId(vm.getExternalId() != null ? vm.getExternalId() : "")
                .setGrantId(vm.getTenantDatacenterGrantId().toString())
                .build());
        return buildOperationResponse(vmId, "VM deletion initiated — job: " + job.getJobId());
    }

    public VmConsoleResponse getVmConsole(UUID tenantId, UUID vmId) {
        VmEntity vm = requireVmForTenant(tenantId, vmId);

        if (vm.getStatus() != com.onetattva.infron.api.enums.VmStatus.ACTIVE) {
            throw new IllegalStateException("VM must be running to access console");
        }
        UUID userId = getCurrentUserId();
        if (userId == null) {
            throw new AccessDeniedException("User identity is required to open a VM console");
        }
        long active = consoleSessionRepository.countByUserIdAndStatus(userId, ConsoleSessionStatus.ACTIVE);
        if (active >= infronConsoleProperties.getMaxSessionsPerUser()) {
            throw new IllegalStateException("Maximum number of active console sessions reached; close an existing session and retry");
        }

        TenantDatacenterGrantEntity grant = tenantDatacenterGrantRepository
                .findById(vm.getTenantDatacenterGrantId())
                .orElseThrow(() -> new EntityNotFoundException("Tenant datacenter grant not found"));
        UUID infronTenantId = grant.getTenant().getId();
        if (!infronTenantId.equals(tenantId)) {
            throw new AccessDeniedException("VM tenant mismatch");
        }

        Map<String, Object> resolvePayload = new HashMap<>();
        resolvePayload.put(VmConsoleResolvePayloadKeys.TENANT_DATACENTER_GRANT_ID, vm.getTenantDatacenterGrantId().toString());
        resolvePayload.put(VmConsoleResolvePayloadKeys.EXTERNAL_ID, vm.getExternalId() != null ? vm.getExternalId() : "");
        resolvePayload.put(VmConsoleResolvePayloadKeys.NODE_ID, vm.getNodeId() != null ? vm.getNodeId().toString() : "");

        CommandMessage consoleCommand = CommandMessage.builder()
                .queueType(VmQueueCommands.CONSOLE_RESOLVE)
                .entityType(EntityType.VM)
                .entityId(vm.getId())
                .payload(resolvePayload)
                .metadata(Map.of("source", "api", "requestId", generateRequestId()))
                .source("core-services")
                .actorType("USER")
                .actorUserId(userId)
                .actorService("api")
                .createdAt(Instant.now())
                .build();

        UUID commandId = commandQueue.sendCommand(consoleCommand);
        Map<String, Object> resolved = waitForConsoleResolve(commandId, infronConsoleProperties.getResolveTimeoutSeconds());

        if (!Boolean.TRUE.equals(resolved.get(VmConsoleResolvePayloadKeys.RESOLVED))) {
            throw new IllegalStateException("Console resolution did not complete successfully");
        }

        Object typeObj = resolved.get(VmConsoleResolvePayloadKeys.CONSOLE_TYPE);
        if (typeObj == null) {
            throw new IllegalStateException("Orchestrator returned no console type");
        }
        VmConsoleType consoleType;
        try {
            consoleType = VmConsoleType.valueOf(typeObj.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown console type: " + typeObj);
        }

        Object hostObj = resolved.get(VmConsoleResolvePayloadKeys.HOST);
        Object portObj = resolved.get(VmConsoleResolvePayloadKeys.PORT);
        if (hostObj == null || portObj == null) {
            throw new IllegalStateException("Orchestrator returned incomplete console endpoint");
        }
        String host = hostObj.toString();
        int port = payloadPort(portObj);
        boolean tls = Boolean.TRUE.equals(resolved.get(VmConsoleResolvePayloadKeys.TLS));

        Object pwObj = resolved.get(VmConsoleResolvePayloadKeys.PASSWORD);
        String hypervisorPassword = pwObj != null && !pwObj.toString().isEmpty() ? pwObj.toString() : null;

        String token = new BigInteger(130, new SecureRandom()).toString(32);
        int timeoutMinutes = resolveConsoleTimeoutMinutes(tenantId);
        Instant expiresAt = Instant.now().plus(timeoutMinutes, ChronoUnit.MINUTES);

        ConsoleSessionEntity session = new ConsoleSessionEntity();
        session.setVmId(vm.getId());
        session.setTenantId(tenantId);
        session.setUserId(userId);
        session.setToken(token);
        session.setConsoleType(mapPersistenceConsoleType(consoleType));
        session.setHypervisorHost(host);
        session.setHypervisorPort(port);
        session.setHypervisorPassword(hypervisorPassword);
        session.setTls(tls);
        session.setStatus(ConsoleSessionStatus.ACTIVE);
        session.setExpiresAt(expiresAt);
        session.setCreatedAt(Instant.now());
        session.setLastActivityAt(Instant.now());

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.executeWithoutResult(status -> consoleSessionRepository.save(session));

        VmConsoleResponse response = new VmConsoleResponse();
        response.setUrl(URI.create(buildConsoleProxyWsUrl(token)));
        response.setToken(token);
        response.setExpiresAt(expiresAt.atOffset(ZoneOffset.UTC));
        response.setConsoleType(mapApiConsoleType(consoleType));
        response.setRemotePassword(hypervisorPassword);
        return response;
    }

    private static int payloadPort(Object portObj) {
        if (portObj instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(portObj.toString());
    }

    private Map<String, Object> waitForConsoleResolve(UUID commandId, int timeoutSeconds) {
        Instant deadline = Instant.now().plusSeconds(timeoutSeconds);
        while (Instant.now().isBefore(deadline)) {
            Optional<QueueEntryEntity> opt = queueEntryRepository.findById(commandId);
            if (opt.isEmpty()) {
                throw new IllegalStateException("Console resolve command disappeared");
            }
            QueueEntryEntity row = opt.get();
            if (row.getStatus() == QueueStatus.COMPLETED) {
                Map<String, Object> p = row.getPayload();
                if (p == null) {
                    throw new IllegalStateException("Console resolve completed without payload");
                }
                return p;
            }
            if (row.getStatus() == QueueStatus.FAILED) {
                String msg = row.getErrorMessage();
                throw new IllegalStateException(
                        msg != null && !msg.isBlank() ? msg : "Console resolution failed");
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for console resolution", e);
            }
        }
        throw new IllegalStateException(
                "Console resolution timed out after " + timeoutSeconds + "s; ensure the orchestrator is running");
    }

    public VmOperationResponse attachVmIso(UUID tenantId, UUID vmId, VmIsoAttachRequest request) {
        requireContentLibraryRead(tenantId);
        VmEntity vm = requireVmForTenant(tenantId, vmId);
        if (vm.getStatus() != com.onetattva.infron.api.enums.VmStatus.ACTIVE) {
            throw new IllegalStateException("VM must be active to attach an ISO");
        }
        UUID isoId = request.getContentItemId();
        ContentItemEntity iso = contentItemRepository.findById(isoId)
                .orElseThrow(() -> new EntityNotFoundException("Content item not found: " + isoId));
        assertContentItemReadableByTenant(tenantId, iso);
        if (!"iso".equalsIgnoreCase(iso.getContentType())) {
            throw new IllegalArgumentException("content_item_id must reference an iso item");
        }
        if (!"available".equalsIgnoreCase(iso.getContentStatus())) {
            throw new IllegalArgumentException("ISO content item must be available in the content store");
        }
        List<UUID> attached = vm.getAttachedIsoItemIds();
        if (attached == null) {
            attached = new ArrayList<>();
        } else {
            attached = new ArrayList<>(attached);
        }
        if (!attached.contains(isoId)) {
            attached.add(isoId);
        }
        vm.setAttachedIsoItemIds(attached);
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);
        log.debug(
                "VM attachIso gRPC: vmId={}, isoContentItemId={}, grantId={}, externalIdSet={}",
                vmId,
                isoId,
                vm.getTenantDatacenterGrantId(),
                vm.getExternalId() != null && !vm.getExternalId().isBlank());
        JobResponse job = vmWorkflowStub.attachIso(AttachIsoVMRequest.newBuilder()
                .setVmId(vm.getId().toString())
                .setExternalId(vm.getExternalId() != null ? vm.getExternalId() : "")
                .setGrantId(vm.getTenantDatacenterGrantId().toString())
                .setIsoContentId(isoId.toString())
                .build());
        return buildOperationResponse(vmId, "ISO attach initiated — job: " + job.getJobId());
    }

    public VmOperationResponse detachVmIso(UUID tenantId, UUID vmId, VmIsoDetachRequest request) {
        VmEntity vm = requireVmForTenant(tenantId, vmId);
        String deviceName = request.getDeviceName();
        UUID toRemove = findAttachedIsoIdByDeviceName(vm, deviceName);
        if (toRemove != null) {
            List<UUID> attached = vm.getAttachedIsoItemIds();
            if (attached != null) {
                List<UUID> next = new ArrayList<>(attached);
                next.remove(toRemove);
                vm.setAttachedIsoItemIds(next.isEmpty() ? null : next);
                vm.setUpdatedAt(Instant.now());
                vmRepository.save(vm);
            }
        }
        log.debug(
                "VM detachIso gRPC: vmId={}, deviceName={}, grantId={}, externalIdSet={}",
                vmId,
                deviceName,
                vm.getTenantDatacenterGrantId(),
                vm.getExternalId() != null && !vm.getExternalId().isBlank());
        JobResponse detachJob = vmWorkflowStub.detachIso(DetachIsoVMRequest.newBuilder()
                .setVmId(vm.getId().toString())
                .setExternalId(vm.getExternalId() != null ? vm.getExternalId() : "")
                .setGrantId(vm.getTenantDatacenterGrantId().toString())
                .setDeviceName(deviceName)
                .build());
        return buildOperationResponse(vmId, "ISO detach initiated — job: " + detachJob.getJobId());
    }

    public VmPublishTemplateResponse publishVmAsTemplate(UUID tenantId, UUID vmId, VmPublishTemplateRequest request) {
        requirePermissionForTenant(tenantId, Permission.VM_READ);
        requirePermissionForTenant(tenantId, Permission.CONTENT_LIBRARY_PUBLISH_TEMPLATE);
        VmEntity vm = requireVmForTenant(tenantId, vmId);
        com.onetattva.infron.api.enums.VmStatus st = vm.getStatus();
        if (st != com.onetattva.infron.api.enums.VmStatus.ACTIVE
                && st != com.onetattva.infron.api.enums.VmStatus.STOPPED) {
            throw new IllegalStateException("VM must be active or stopped to publish as template");
        }
        contentLibraryService.requireWriteAccess(tenantId, request.getLibraryId());

        ContentItemEntity item = new ContentItemEntity();
        item.setLibraryId(request.getLibraryId());
        item.setName(request.getTemplateName());
        item.setDescription(request.getDescription());
        item.setContentType("vm_template");
        if (request.getVersionLabel() != null) {
            item.setVersionLabel(request.getVersionLabel());
        }
        item.setMetadata(request.getMetadata());
        item.setContentStatus("pending");
        ContentItemEntity saved = contentItemRepository.save(item);
        contentLibraryProviderPathBuilder.applyProviderPaths(saved);
        saved = contentItemRepository.save(saved);

        PublishVMTemplateRequest pubGrpc = PublishVMTemplateRequest.newBuilder()
                .setVmId(vm.getId().toString())
                .setExternalId(vm.getExternalId() != null ? vm.getExternalId() : "")
                .setGrantId(vm.getTenantDatacenterGrantId().toString())
                .setContentItemId(saved.getId().toString())
                .build();
        log.debug(
                "VM publish template gRPC: vmId={}, contentItemId={}, libraryId={}, grantId={}",
                vmId,
                saved.getId(),
                request.getLibraryId(),
                vm.getTenantDatacenterGrantId());
        JobResponse publishJob = vmWorkflowStub.publishVMTemplate(pubGrpc);

        VmPublishTemplateResponse response = new VmPublishTemplateResponse();
        response.setContentItemId(saved.getId());
        response.setMessage("Template publishing initiated — job: " + publishJob.getJobId());
        return response;
    }

    private VmEntity requireVmForTenant(UUID tenantId, UUID vmId) {
        return vmRepository.findByIdAndTenantId(vmId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("VM not found"));
    }

    private static Sort vmListSort(String sortField, String order) {
        String f = sortField != null ? sortField : "created_at";
        String property = switch (f) {
            case "name" -> "name";
            case "updated_at" -> "updatedAt";
            case "status" -> "status";
            default -> "createdAt";
        };
        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }

    private Vm mapEntityToApi(VmEntity entity) {
        Vm vm = new Vm();
        vm.setId(entity.getId());
        vm.setName(entity.getName());
        vm.setDescription(entity.getDescription());
        vm.setStatus(VmStatus.valueOf(entity.getStatus().name()));
        vm.setPowerState(VmPowerState.valueOf(entity.getPowerState().name()));
        vm.setTenantDatacenterGrantId(entity.getTenantDatacenterGrantId());
        // Note: spec is stored as JSON string in entity, would need proper deserialization
        vm.setSpec(null);
        vm.setProviderId(entity.getProviderId());
        vm.setNodeId(entity.getNodeId());
        vm.setExternalId(entity.getExternalId());
        vm.setContentItemId(entity.getContentItemId());
        if (entity.getAttachedIsoItemIds() != null && !entity.getAttachedIsoItemIds().isEmpty()) {
            vm.setAttachedIsoItemIds(new ArrayList<>(entity.getAttachedIsoItemIds()));
        }
        vm.setIpAddresses(entity.getIpAddresses());
        vm.setHostname(entity.getHostname());
        // Note: resourceUsage is stored as JSON string in entity, would need proper deserialization
        vm.setResourceUsage(null);
        vm.setMetadata(entity.getMetadata());
        vm.setTags(entity.getTags());
        vm.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        vm.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        vm.setStartedAt(entity.getStartedAt() != null ? entity.getStartedAt().atOffset(ZoneOffset.UTC) : null);
        vm.setStoppedAt(entity.getStoppedAt() != null ? entity.getStoppedAt().atOffset(ZoneOffset.UTC) : null);

        return vm;
    }

    private String vmSpecToJson(VmSpec spec) {
        if (spec == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize VM spec to JSON", e);
        }
    }

    private VmOperationResponse buildOperationResponse(UUID vmId, String message) {
        VmOperationResponse response = new VmOperationResponse();
        response.setMessage(message);
        response.setOperationId(UUID.randomUUID());
        return response;
    }

    private CommandMessage buildCreateCommand(VmEntity vm, VmCreateRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("spec", vm.getSpec() != null ? vm.getSpec() : "{}");
        payload.put("tenantDatacenterGrantId", request.getTenantDatacenterGrantId().toString());
        payload.put("name", request.getName());
        if (request.getMetadata() != null) {
            payload.put("metadata", request.getMetadata());
        }
        if (request.getTags() != null) {
            payload.put("tags", request.getTags());
        }
        if (request.getContentItemId() != null) {
            payload.put("contentItemId", request.getContentItemId().toString());
        }
        if (request.getIsoContentItemIds() != null && !request.getIsoContentItemIds().isEmpty()) {
            payload.put("isoContentItemIds", request.getIsoContentItemIds().stream().map(UUID::toString).toList());
        }
        return CommandMessage.builder()
            .queueType("VM_CREATE_COMMAND")
            .entityType(EntityType.VM)
            .entityId(vm.getId())
            .payload(payload)
            .metadata(Map.of("source", "api", "requestId", generateRequestId()))
            .source("core-services")
            .actorType("USER")
            .actorUserId(getCurrentUserId())
            .actorService("api")
            .createdAt(Instant.now())
            .build();
    }

    private CreateVMRequest buildGrpcCreateRequest(VmEntity vm, VmCreateRequest request) {
        CreateVMRequest.Builder b = CreateVMRequest.newBuilder()
                .setVmId(vm.getId().toString())
                .setTenantDatacenterGrantId(request.getTenantDatacenterGrantId().toString())
                .setSpecJson(vm.getSpec() != null ? vm.getSpec() : "{}")
                .setCorrelationId(generateRequestId());
        if (request.getIsoContentItemIds() != null) {
            request.getIsoContentItemIds().forEach(id -> b.addIsoContentIds(id.toString()));
        }
        return b.build();
    }

    /**
     * Merge a vm_template content item into the VM spec.
     *
     * - templateSpec.compute -> vmSpec.compute
     * - templateSpec.disks   -> vmSpec.storage.disks (by index)
     * - request spec can override by increasing resources (never decrease below template minimums)
     */
    private VmSpec mergeTemplateIntoVmSpec(ContentItemEntity templateItem, VmSpec requestSpec) {
        if (templateItem.getTemplateSpec() == null) {
            throw new IllegalStateException("Template content item is missing template_spec in database");
        }
        VmTemplateSpec tpl = objectMapper.convertValue(templateItem.getTemplateSpec(), VmTemplateSpec.class);
        if (tpl.getSpec() == null || tpl.getSpec().getCompute() == null) {
            throw new IllegalStateException("templateSpec.spec.compute is required");
        }
        List<VmTemplateDiskSpec> tplDisks = tpl.getSpec().getDisks();
        if (tplDisks == null || tplDisks.isEmpty()) {
            throw new IllegalStateException("templateSpec.spec.disks must have at least 1 disk");
        }

        VmSpec out = new VmSpec();

        // Compute
        ComputeSpec compute = new ComputeSpec();
        int tplCpus = tpl.getSpec().getCompute().getCpuCores();
        int tplMem = tpl.getSpec().getCompute().getMemoryMB();

        Integer reqCpusObj = requestSpec != null && requestSpec.getCompute() != null ? requestSpec.getCompute().getCpus() : null;
        Integer reqMemObj = requestSpec != null && requestSpec.getCompute() != null ? requestSpec.getCompute().getMemorySizeMb() : null;
        int reqCpus = reqCpusObj != null ? reqCpusObj : tplCpus;
        int reqMem = reqMemObj != null ? reqMemObj : tplMem;

        if (reqCpus < tplCpus) {
            throw new IllegalArgumentException("spec.compute.cpus cannot be less than template cpuCores (" + tplCpus + ")");
        }
        if (reqMem < tplMem) {
            throw new IllegalArgumentException("spec.compute.memorySizeMb cannot be less than template memoryMB (" + tplMem + ")");
        }
        compute.setCpus(reqCpus);
        compute.setMemorySizeMb(reqMem);
        out.setCompute(compute);

        // Storage disks
        StorageSpec storage = new StorageSpec();
        List<DiskSpec> reqDisks = requestSpec != null && requestSpec.getStorage() != null ? requestSpec.getStorage().getDisks() : null;
        java.util.ArrayList<DiskSpec> disks = new java.util.ArrayList<>();
        for (int i = 0; i < tplDisks.size(); i++) {
            VmTemplateDiskSpec td = tplDisks.get(i);
            long tplSizeMb = (td.getSizeBytes() + (1024L * 1024L - 1)) / (1024L * 1024L);
            Integer reqSizeMbObj = (reqDisks != null && i < reqDisks.size() && reqDisks.get(i) != null) ? reqDisks.get(i).getSizeMb() : null;
            long reqSizeMb = reqSizeMbObj != null ? reqSizeMbObj.longValue() : tplSizeMb;
            if (reqSizeMb < tplSizeMb) {
                throw new IllegalArgumentException("spec.storage.disks[" + i + "].sizeMb cannot be less than template disk size");
            }
            DiskSpec d = new DiskSpec();
            d.setSizeMb((int) reqSizeMb);
            if (reqDisks != null && i < reqDisks.size() && reqDisks.get(i) != null) {
                d.setStorageClass(reqDisks.get(i).getStorageClass());
            }
            disks.add(d);
        }
        storage.setDisks(disks);
        if (requestSpec != null && requestSpec.getStorage() != null) {
            storage.setVmStorageClass(requestSpec.getStorage().getVmStorageClass());
        }
        out.setStorage(storage);

        // Pass-through (network/os) from request if present
        if (requestSpec != null) {
            out.setNetwork(requestSpec.getNetwork());
            out.setOs(requestSpec.getOs());
        }
        return out;
    }

    private CommandMessage buildStartCommand(VmEntity vm) {
        return CommandMessage.builder()
            .queueType("VM_START_COMMAND")
            .entityType(EntityType.VM)
            .entityId(vm.getId())
            .payload(Map.of("vmId", vm.getId().toString()))
            .metadata(Map.of("source", "api", "requestId", generateRequestId()))
            .source("core-services")
            .actorType("USER")
            .actorUserId(getCurrentUserId())
            .actorService("api")
            .createdAt(Instant.now())
            .build();
    }

    private CommandMessage buildStopCommand(VmEntity vm) {
        return CommandMessage.builder()
            .queueType("VM_STOP_COMMAND")
            .entityType(EntityType.VM)
            .entityId(vm.getId())
            .payload(Map.of("vmId", vm.getId().toString()))
            .metadata(Map.of("source", "api", "requestId", generateRequestId()))
            .source("core-services")
            .actorType("USER")
            .actorUserId(getCurrentUserId())
            .actorService("api")
            .createdAt(Instant.now())
            .build();
    }

    private CommandMessage buildRestartCommand(VmEntity vm) {
        return CommandMessage.builder()
            .queueType("VM_RESTART_COMMAND")
            .entityType(EntityType.VM)
            .entityId(vm.getId())
            .payload(Map.of("vmId", vm.getId().toString()))
            .metadata(Map.of("source", "api", "requestId", generateRequestId()))
            .source("core-services")
            .actorType("USER")
            .actorUserId(getCurrentUserId())
            .actorService("api")
            .createdAt(Instant.now())
            .build();
    }

    private CommandMessage buildSuspendCommand(VmEntity vm) {
        return CommandMessage.builder()
            .queueType("VM_SUSPEND_COMMAND")
            .entityType(EntityType.VM)
            .entityId(vm.getId())
            .payload(Map.of("vmId", vm.getId().toString()))
            .metadata(Map.of("source", "api", "requestId", generateRequestId()))
            .source("core-services")
            .actorType("USER")
            .actorUserId(getCurrentUserId())
            .actorService("api")
            .createdAt(Instant.now())
            .build();
    }

    private CommandMessage buildResumeCommand(VmEntity vm) {
        return CommandMessage.builder()
            .queueType("VM_RESUME_COMMAND")
            .entityType(EntityType.VM)
            .entityId(vm.getId())
            .payload(Map.of("vmId", vm.getId().toString()))
            .metadata(Map.of("source", "api", "requestId", generateRequestId()))
            .source("core-services")
            .actorType("USER")
            .actorUserId(getCurrentUserId())
            .actorService("api")
            .createdAt(Instant.now())
            .build();
    }

    private CommandMessage buildDeleteCommand(VmEntity vm) {
        return CommandMessage.builder()
            .queueType("VM_DELETE_COMMAND")
            .entityType(EntityType.VM)
            .entityId(vm.getId())
            .payload(Map.of("vmId", vm.getId().toString()))
            .metadata(Map.of("source", "api", "requestId", generateRequestId()))
            .source("core-services")
            .actorType("USER")
            .actorUserId(getCurrentUserId())
            .actorService("api")
            .createdAt(Instant.now())
            .build();
    }

    private CommandMessage buildAttachIsoCommand(VmEntity vm, UUID contentItemId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("vmId", vm.getId().toString());
        payload.put("contentItemId", contentItemId.toString());
        return CommandMessage.builder()
                .queueType("VM_ATTACH_ISO_COMMAND")
                .entityType(EntityType.VM)
                .entityId(vm.getId())
                .payload(payload)
                .metadata(Map.of("source", "api", "requestId", generateRequestId()))
                .source("core-services")
                .actorType("USER")
                .actorUserId(getCurrentUserId())
                .actorService("api")
                .createdAt(Instant.now())
                .build();
    }

    private CommandMessage buildDetachIsoCommand(VmEntity vm, String deviceName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("vmId", vm.getId().toString());
        payload.put("deviceName", deviceName);
        return CommandMessage.builder()
                .queueType("VM_DETACH_ISO_COMMAND")
                .entityType(EntityType.VM)
                .entityId(vm.getId())
                .payload(payload)
                .metadata(Map.of("source", "api", "requestId", generateRequestId()))
                .source("core-services")
                .actorType("USER")
                .actorUserId(getCurrentUserId())
                .actorService("api")
                .createdAt(Instant.now())
                .build();
    }

    private CommandMessage buildPublishTemplateCommand(VmEntity vm, UUID contentItemId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("vmId", vm.getId().toString());
        payload.put("contentItemId", contentItemId.toString());
        return CommandMessage.builder()
                .queueType("VM_PUBLISH_TEMPLATE_COMMAND")
                .entityType(EntityType.VM)
                .entityId(vm.getId())
                .payload(payload)
                .metadata(Map.of("source", "api", "requestId", generateRequestId()))
                .source("core-services")
                .actorType("USER")
                .actorUserId(getCurrentUserId())
                .actorService("api")
                .createdAt(Instant.now())
                .build();
    }

    private void requireContentLibraryRead(UUID tenantId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal user)) {
            return;
        }
        if (!authorizationService.isAllowedForTenant(user, Permission.CONTENT_LIBRARY_READ.getAction(), tenantId.toString())) {
            throw new AccessDeniedException("content_library:read required");
        }
    }

    private void requirePermissionForTenant(UUID tenantId, Permission permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal user)) {
            return;
        }
        if (!authorizationService.isAllowedForTenant(user, permission.getAction(), tenantId.toString())) {
            throw new AccessDeniedException(permission.getAction() + " required");
        }
    }

    private void assertContentItemReadableByTenant(UUID tenantId, ContentItemEntity item) {
        ContentLibraryEntity lib = contentLibraryRepository.findById(item.getLibraryId())
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + item.getLibraryId()));
        boolean visible = lib.getTenantId().equals(tenantId)
                || lib.getTenantId().equals(ContentLibraryService.SYSTEM_TENANT_ID);
        if (!visible) {
            throw new EntityNotFoundException("Content item not found: " + item.getId());
        }
    }

    private UUID findAttachedIsoIdByDeviceName(VmEntity vm, String deviceName) {
        if (deviceName == null || deviceName.isBlank()) {
            return null;
        }
        List<UUID> ids = vm.getAttachedIsoItemIds();
        if (ids == null) {
            return null;
        }
        for (UUID id : ids) {
            ContentItemEntity e = contentItemRepository.findById(id).orElse(null);
            if (e == null) {
                continue;
            }
            Map<String, String> meta = e.getMetadata();
            String dn = meta != null ? meta.get("deviceName") : null;
            if (deviceName.equals(dn)) {
                return id;
            }
        }
        return null;
    }

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal user) {
            String subject = user.id();
            List<UserRoleBindingViewEntity> bindings = userRoleBindingViewRepository.findByExternalId(subject);
            if (!bindings.isEmpty() && bindings.get(0).getUserId() != null) {
                return bindings.get(0).getUserId();
            }

            // Backward-compatible fallback for environments where principal id is already idp_user.id.
            try {
                UUID candidateUserId = UUID.fromString(subject);
                if (idpUserRepository.existsById(candidateUserId)) {
                    return candidateUserId;
                }
            } catch (IllegalArgumentException ignored) {
                // If the principal id is not UUID, leave actorUserId as null.
            }
        }
        return null;
    }

    private String generateRequestId() {
        return "req-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String buildConsoleProxyWsUrl(String token) {
        String base = infronConsoleProperties.getProxyWsBaseUrl();
        String enc = URLEncoder.encode(token, StandardCharsets.UTF_8);
        if (base.contains("?")) {
            return base + "&token=" + enc;
        }
        return base + "?token=" + enc;
    }

    private int resolveConsoleTimeoutMinutes(UUID tenantId) {
        int fallback = 15;
        Optional<SystemSettingsEntity> tenantSettings = systemSettingsRepository.findByTenantId(tenantId);
        if (tenantSettings.isPresent()) {
            Integer m = tenantSettings.get().getConsoleSessionTimeoutMinutes();
            if (m != null && m > 0) {
                return m;
            }
        }
        Optional<SystemSettingsEntity> systemSettings =
                systemSettingsRepository.findByTenantId(UUID.fromString(Constants.SYSTEM_ID));
        return systemSettings
                .map(SystemSettingsEntity::getConsoleSessionTimeoutMinutes)
                .filter(m -> m != null && m > 0)
                .orElse(fallback);
    }

    private static ConsoleSessionConsoleType mapPersistenceConsoleType(VmConsoleType type) {
        return switch (type) {
            case VNC -> ConsoleSessionConsoleType.VNC;
            case SPICE -> ConsoleSessionConsoleType.SPICE;
            case SERIAL -> ConsoleSessionConsoleType.SERIAL;
        };
    }

    private static com.onetattva.infron.api.model.VmConsoleResponse.ConsoleTypeEnum mapApiConsoleType(VmConsoleType type) {
        return switch (type) {
            case VNC -> com.onetattva.infron.api.model.VmConsoleResponse.ConsoleTypeEnum.VNC;
            case SPICE -> com.onetattva.infron.api.model.VmConsoleResponse.ConsoleTypeEnum.SPICE;
            case SERIAL -> com.onetattva.infron.api.model.VmConsoleResponse.ConsoleTypeEnum.SERIAL;
        };
    }
}
