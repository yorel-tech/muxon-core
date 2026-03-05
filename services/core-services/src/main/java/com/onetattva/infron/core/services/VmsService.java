package com.onetattva.infron.core.services;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.common.EntityNotFoundException;
import com.onetattva.infron.api.enums.EntityType;
import com.onetattva.infron.api.enums.QueueCategory;
import com.onetattva.infron.api.enums.QueueStatus;
import com.onetattva.infron.db.model.QueueEntry;
import com.onetattva.infron.db.model.TenantDatacenterGrantEntity;
import com.onetattva.infron.db.model.VmEntity;
import com.onetattva.infron.db.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private QueueEntryRepository queueEntryRepository;
    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public VmCreateResponse createVm(VmCreateRequest request) {
        // Validate tenant datacenter grant
        TenantDatacenterGrantEntity grant = tenantDatacenterGrantRepository.findById(request.getTenantDatacenterGrantId())
                .orElseThrow(() -> new EntityNotFoundException("Tenant datacenter grant not found"));

        // Create VM entity
        VmEntity vm = new VmEntity();
        vm.setId(UUID.randomUUID());
        vm.setTenantDatacenterGrantId(grant.getId());
        vm.setName(request.getName());
        vm.setSpec(vmSpecToJson(request.getSpec()));
        vm.setStatus(com.onetattva.infron.api.enums.VmStatus.PENDING);
        vm.setPowerState(com.onetattva.infron.api.enums.VmPowerState.UNKNOWN);
        vm.setCreatedAt(Instant.now());
        vm.setUpdatedAt(Instant.now());
        vm.setMetadata(request.getMetadata() != null ? request.getMetadata().toString() : null);
        vm.setTags(request.getTags());

        // Save VM
        VmEntity saved = vmRepository.save(vm);

        // Insert command into database queue
        QueueEntry commandEntry = buildCreateCommand(saved, request);
        queueEntryRepository.save(commandEntry);

        VmCreateResponse response = new VmCreateResponse();
        response.setId(saved.getId());
        response.setName(saved.getName());
        response.setStatus(VmStatus.valueOf(saved.getStatus().name()));
        response.setMessage("VM creation initiated");
        response.setCreatedAt(saved.getCreatedAt().atOffset(ZoneOffset.UTC));
        return response;
    }

    public VmListResponse listVms(Integer page, Integer perPage, VmStatus status,
                                UUID tenantDatacenterGrantId, String tags,
                                String sort) {
        Pageable pageable = PageRequest.of(page - 1, perPage);
        Page<VmEntity> entityPage = vmRepository.findAll(pageable);

        // Apply filters - simplified, only basic filtering
        if (status != null) {
            // entityPage = vmRepository.findByStatus(VmEnums.VmStatus.valueOf(status), pageable);
        }
        // Note: Additional filters not implemented yet

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

    public Vm getVm(UUID vmId) {
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found"));

        return mapEntityToApi(vm);
    }

    public Vm patchVm(UUID vmId, VmUpdateRequest request) {
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found"));

        if (request.getDescription() != null) {
            vm.setDescription(request.getDescription());
        }
        if (request.getMetadata() != null) {
            vm.setMetadata(request.getMetadata().toString());
        }
        if (request.getTags() != null) {
            vm.setTags(request.getTags());
        }

        vm.setUpdatedAt(Instant.now());
        VmEntity saved = vmRepository.save(vm);

        return mapEntityToApi(saved);
    }

    public VmOperationResponse startVm(UUID vmId) {
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found"));

        // Validate state transition
        if (vm.getStatus() != com.onetattva.infron.api.enums.VmStatus.STOPPED) {
            throw new IllegalStateException("VM must be stopped to start");
        }

        // Insert command into database queue
        QueueEntry commandEntry = buildStartCommand(vm);
        queueEntryRepository.save(commandEntry);

        return buildOperationResponse(vmId, "VM start initiated");
    }

    public VmOperationResponse stopVm(UUID vmId) {
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found"));

        // Validate state transition
        if (vm.getStatus() != com.onetattva.infron.api.enums.VmStatus.ACTIVE) {
            throw new IllegalStateException("VM must be running to stop");
        }

        // Insert command into database queue
        QueueEntry commandEntry = buildStopCommand(vm);
        queueEntryRepository.save(commandEntry);

        return buildOperationResponse(vmId, "VM stop initiated");
    }

    public VmOperationResponse restartVm(UUID vmId) {
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found"));

        // Insert command into database queue
        QueueEntry commandEntry = buildRestartCommand(vm);
        queueEntryRepository.save(commandEntry);

        return buildOperationResponse(vmId, "VM restart initiated");
    }

    public VmOperationResponse suspendVm(UUID vmId) {
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found"));

        // Validate state transition
        if (vm.getStatus() != com.onetattva.infron.api.enums.VmStatus.ACTIVE && vm.getStatus() != com.onetattva.infron.api.enums.VmStatus.SUSPENDED) {
            throw new IllegalStateException("VM must be running or suspended to suspend");
        }

        // Insert command into database queue
        QueueEntry commandEntry = buildSuspendCommand(vm);
        queueEntryRepository.save(commandEntry);

        return buildOperationResponse(vmId, "VM suspend initiated");
    }

    public VmOperationResponse resumeVm(UUID vmId) {
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found"));

        // Validate state transition
        if (vm.getStatus() != com.onetattva.infron.api.enums.VmStatus.SUSPENDED) {
            throw new IllegalStateException("VM must be suspended to resume");
        }

        // Insert command into database queue
        QueueEntry commandEntry = buildResumeCommand(vm);
        queueEntryRepository.save(commandEntry);

        return buildOperationResponse(vmId, "VM resume initiated");
    }

    public VmOperationResponse deleteVm(UUID vmId) {
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found"));

        // Update VM status
        vm.setStatus(com.onetattva.infron.api.enums.VmStatus.DELETING);
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);

        // Insert command into database queue
        QueueEntry commandEntry = buildDeleteCommand(vm);
        queueEntryRepository.save(commandEntry);

        return buildOperationResponse(vmId, "VM deletion initiated");
    }

    public VmConsoleResponse getVmConsole(UUID vmId) {
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found"));

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
        // Note: metadata is stored as JSON string in entity, would need proper deserialization
        vm.setMetadata(null);
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

    private QueueEntry buildCreateCommand(VmEntity vm, VmCreateRequest request) {
        QueueEntry entry = new QueueEntry();
        entry.setQueueType("VM_CREATE_COMMAND");
        entry.setEntityType(EntityType.VM);
        entry.setEntityId(vm.getId());
        entry.setQueueCategory(QueueCategory.COMMAND);
        entry.setStatus(QueueStatus.PENDING);
        entry.setActorType("USER");
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
        entry.setPayload(payload);
        entry.setMetadata(Map.of(
            "source", "api",
            "requestId", generateRequestId()
        ));
        entry.setSource("core-services");
        entry.setCreatedAt(Instant.now());
        return entry;
    }

    private QueueEntry buildStartCommand(VmEntity vm) {
        QueueEntry entry = new QueueEntry();
        entry.setQueueType("VM_START_COMMAND");
        entry.setEntityType(EntityType.VM);
        entry.setEntityId(vm.getId());
        entry.setQueueCategory(QueueCategory.COMMAND);
        entry.setStatus(QueueStatus.PENDING);
        entry.setActorType("USER");
        entry.setPayload(Map.of("vmId", vm.getId().toString()));
        entry.setMetadata(Map.of(
            "source", "api",
            "requestId", generateRequestId()
        ));
        entry.setSource("core-services");
        entry.setCreatedAt(Instant.now());
        return entry;
    }

    private QueueEntry buildStopCommand(VmEntity vm) {
        QueueEntry entry = new QueueEntry();
        entry.setQueueType("VM_STOP_COMMAND");
        entry.setEntityType(EntityType.VM);
        entry.setEntityId(vm.getId());
        entry.setQueueCategory(QueueCategory.COMMAND);
        entry.setStatus(QueueStatus.PENDING);
        entry.setActorType("USER");
        entry.setPayload(Map.of("vmId", vm.getId().toString()));
        entry.setMetadata(Map.of(
            "source", "api",
            "requestId", generateRequestId()
        ));
        entry.setSource("core-services");
        entry.setCreatedAt(Instant.now());
        return entry;
    }

    private QueueEntry buildRestartCommand(VmEntity vm) {
        QueueEntry entry = new QueueEntry();
        entry.setQueueType("VM_RESTART_COMMAND");
        entry.setEntityType(EntityType.VM);
        entry.setEntityId(vm.getId());
        entry.setQueueCategory(QueueCategory.COMMAND);
        entry.setStatus(QueueStatus.PENDING);
        entry.setActorType("USER");
        entry.setPayload(Map.of("vmId", vm.getId().toString()));
        entry.setMetadata(Map.of(
            "source", "api",
            "requestId", generateRequestId()
        ));
        entry.setSource("core-services");
        entry.setCreatedAt(Instant.now());
        return entry;
    }

    private QueueEntry buildSuspendCommand(VmEntity vm) {
        QueueEntry entry = new QueueEntry();
        entry.setQueueType("VM_SUSPEND_COMMAND");
        entry.setEntityType(EntityType.VM);
        entry.setEntityId(vm.getId());
        entry.setQueueCategory(QueueCategory.COMMAND);
        entry.setStatus(QueueStatus.PENDING);
        entry.setActorType("USER");
        entry.setPayload(Map.of("vmId", vm.getId().toString()));
        entry.setMetadata(Map.of(
            "source", "api",
            "requestId", generateRequestId()
        ));
        entry.setSource("core-services");
        entry.setCreatedAt(Instant.now());
        return entry;
    }

    private QueueEntry buildResumeCommand(VmEntity vm) {
        QueueEntry entry = new QueueEntry();
        entry.setQueueType("VM_RESUME_COMMAND");
        entry.setEntityType(EntityType.VM);
        entry.setEntityId(vm.getId());
        entry.setQueueCategory(QueueCategory.COMMAND);
        entry.setStatus(QueueStatus.PENDING);
        entry.setActorType("USER");
        entry.setPayload(Map.of("vmId", vm.getId().toString()));
        entry.setMetadata(Map.of(
            "source", "api",
            "requestId", generateRequestId()
        ));
        entry.setSource("core-services");
        entry.setCreatedAt(Instant.now());
        return entry;
    }

    private QueueEntry buildDeleteCommand(VmEntity vm) {
        QueueEntry entry = new QueueEntry();
        entry.setQueueType("VM_DELETE_COMMAND");
        entry.setEntityType(EntityType.VM);
        entry.setEntityId(vm.getId());
        entry.setQueueCategory(QueueCategory.COMMAND);
        entry.setStatus(QueueStatus.PENDING);
        entry.setActorType("USER");
        entry.setPayload(Map.of("vmId", vm.getId().toString()));
        entry.setMetadata(Map.of(
            "source", "api",
            "requestId", generateRequestId()
        ));
        entry.setSource("core-services");
        entry.setCreatedAt(Instant.now());
        return entry;
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
