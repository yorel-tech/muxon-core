package com.onetattva.infron.core.services;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.common.EntityNotFoundException;
import com.onetattva.infron.api.model.EntityType;
import tools.jackson.databind.ObjectMapper;
import com.onetattva.infron.core.auth.UserPrincipal;
import com.onetattva.infron.core.spi.queue.CommandMessage;
import com.onetattva.infron.core.spi.queue.CommandQueue;
import com.onetattva.infron.db.model.TenantDatacenterGrantEntity;
import com.onetattva.infron.db.model.VmEntity;
import com.onetattva.infron.db.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VmsService {

    @Autowired
    private VmRepository vmRepository;
    @Autowired
    private ComputeProfileRepository computeProfileRepository;
    @Autowired
    private TenantDatacenterGrantRepository tenantDatacenterGrantRepository;
    @Autowired
    private CommandQueue commandQueue;
    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public VmCreateResponse createVm(UUID tenantId, VmCreateRequest request) {
        TenantDatacenterGrantEntity grant = tenantDatacenterGrantRepository
                .findByIdAndTenant_Id(request.getTenantDatacenterGrantId(), tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant datacenter grant not found"));

        // Create VM entity
        VmEntity vm = new VmEntity();
        vm.setTenantDatacenterGrantId(grant.getId());
        vm.setName(request.getName());
        vm.setSpec(vmSpecToJson(request.getSpec()));
        vm.setStatus(com.onetattva.infron.api.enums.VmStatus.PENDING);
        vm.setPowerState(com.onetattva.infron.api.enums.VmPowerState.UNKNOWN);
        vm.setCreatedAt(Instant.now());
        vm.setUpdatedAt(Instant.now());
        vm.setMetadata(request.getMetadata());
        vm.setTags(request.getTags());

        // Save VM
        VmEntity saved = vmRepository.save(vm);

        // Enqueue command via transport-agnostic port
        CommandMessage command = buildCreateCommand(saved, request);
        commandQueue.sendCommand(command);

        VmCreateResponse response = new VmCreateResponse();
        response.setId(saved.getId());
        response.setName(saved.getName());
        response.setStatus(VmStatus.valueOf(saved.getStatus().name()));
        response.setMessage("VM creation initiated");
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

        vm.setUpdatedAt(Instant.now());
        VmEntity saved = vmRepository.save(vm);

        return mapEntityToApi(saved);
    }

    public VmOperationResponse startVm(UUID tenantId, UUID vmId) {
        VmEntity vm = requireVmForTenant(tenantId, vmId);

        // Validate state transition
        if (vm.getStatus() != com.onetattva.infron.api.enums.VmStatus.STOPPED) {
            throw new IllegalStateException("VM must be stopped to start");
        }

        commandQueue.sendCommand(buildStartCommand(vm));

        return buildOperationResponse(vmId, "VM start initiated");
    }

    public VmOperationResponse stopVm(UUID tenantId, UUID vmId) {
        VmEntity vm = requireVmForTenant(tenantId, vmId);

        // Validate state transition
        if (vm.getStatus() != com.onetattva.infron.api.enums.VmStatus.ACTIVE) {
            throw new IllegalStateException("VM must be running to stop");
        }

        commandQueue.sendCommand(buildStopCommand(vm));

        return buildOperationResponse(vmId, "VM stop initiated");
    }

    public VmOperationResponse restartVm(UUID tenantId, UUID vmId) {
        VmEntity vm = requireVmForTenant(tenantId, vmId);

        commandQueue.sendCommand(buildRestartCommand(vm));

        return buildOperationResponse(vmId, "VM restart initiated");
    }

    public VmOperationResponse suspendVm(UUID tenantId, UUID vmId) {
        VmEntity vm = requireVmForTenant(tenantId, vmId);

        // Validate state transition
        if (vm.getStatus() != com.onetattva.infron.api.enums.VmStatus.ACTIVE && vm.getStatus() != com.onetattva.infron.api.enums.VmStatus.SUSPENDED) {
            throw new IllegalStateException("VM must be running or suspended to suspend");
        }

        commandQueue.sendCommand(buildSuspendCommand(vm));

        return buildOperationResponse(vmId, "VM suspend initiated");
    }

    public VmOperationResponse resumeVm(UUID tenantId, UUID vmId) {
        VmEntity vm = requireVmForTenant(tenantId, vmId);

        // Validate state transition
        if (vm.getStatus() != com.onetattva.infron.api.enums.VmStatus.SUSPENDED) {
            throw new IllegalStateException("VM must be suspended to resume");
        }

        commandQueue.sendCommand(buildResumeCommand(vm));

        return buildOperationResponse(vmId, "VM resume initiated");
    }

    public VmOperationResponse deleteVm(UUID tenantId, UUID vmId) {
        VmEntity vm = requireVmForTenant(tenantId, vmId);

        // Update VM status
        vm.setStatus(com.onetattva.infron.api.enums.VmStatus.DELETING);
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);

        commandQueue.sendCommand(buildDeleteCommand(vm));

        return buildOperationResponse(vmId, "VM deletion initiated");
    }

    public VmConsoleResponse getVmConsole(UUID tenantId, UUID vmId) {
        VmEntity vm = requireVmForTenant(tenantId, vmId);

        // Validate VM is running
        if (vm.getStatus() != com.onetattva.infron.api.enums.VmStatus.ACTIVE) {
            throw new IllegalStateException("VM must be running to access console");
        }

        // Build console response
        VmConsoleResponse response = new VmConsoleResponse();
        response.setUrl(URI.create(generateConsoleUrl(vm)));
        response.setToken(generateConsoleToken(vm));
        response.setExpiresAt(Instant.now().plusSeconds(300).atOffset(ZoneOffset.UTC)); // 5 minutes

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

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal user) {
            try {
                return UUID.fromString(user.id());
            } catch (IllegalArgumentException ignored) {
                // If the user ID is not a valid UUID, leave actorUserId as null
            }
        }
        return null;
    }

    private String generateRequestId() {
        return "req-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateConsoleUrl(VmEntity vm) {
        return String.format("wss://console.infron.local/vm/%s", vm.getId());
    }

    private String generateConsoleToken(VmEntity vm) {
        return UUID.randomUUID().toString();
    }
}
