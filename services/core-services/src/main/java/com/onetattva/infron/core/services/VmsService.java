package com.onetattva.infron.core.services;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.db.model.*;
import com.onetattva.infron.db.repository.*;
import com.onetattva.infron.core.auth.RequiresPermission;
import com.onetattva.infron.core.auth.Permission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
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

    public Vm createVm(VmCreateRequest request) {
        // Validate tenant datacenter grant
        TenantDatacenterGrantEntity grant = tenantDatacenterGrantRepository.findById(request.getTenantDatacenterGrantId())
                .orElseThrow(() -> new RuntimeException("Tenant datacenter grant not found")));

        // Create VM entity
        VmEntity vm = new VmEntity();
        vm.setId(UUID.randomUUID());
        vm.setTenantDatacenterGrantId(grant.getId());
        vm.setName(request.getName());
        vm.setDescription(request.getDescription());
        vm.setSpec(request.getSpec());
        vm.setStatus(VmStatus.PENDING);
        vm.setPowerState(VmPowerState.UNKNOWN);
        vm.setCreatedAt(Instant.now());
        vm.setUpdatedAt(Instant.now());
        vm.setMetadata(request.getMetadata());
        vm.setTags(request.getTags());

        // Save VM
        VmEntity saved = vmRepository.save(vm);

        // Insert command into database queue
        QueueEntry commandEntry = buildCreateCommand(saved, request);
        queueEntryRepository.save(commandEntry);

        return mapEntityToApi(saved);
    }

    public VmList listVms(Integer page, Integer perPage, String status, 
                                UUID tenantDatacenterGrantId, String tags, 
                                String sort) {
        Pageable pageable = PageRequest.of(page - 1, perPage);
        Page<VmEntity> entityPage = vmRepository.findAll(pageable);

        // Apply filters
        if (status != null) {
            entityPage = vmRepository.findByStatus(VmStatus.valueOf(status), pageable);
        }
        if (tenantDatacenterGrantId != null) {
            entityPage = vmRepository.findByTenantDatacenterGrantId(tenantDatacenterGrantId, pageable);
        }
        if (tags != null) {
            entityPage = vmRepository.findByTags(tags, pageable);
        }

        List<Vm> vms = entityPage.getContent().stream()
                .map(this::mapEntityToApi)
                .toList();

        VmList result = new VmList();
        result.setTotal((int) entityPage.getTotalElements());
        result.setPage(page);
        result.setPerPage(perPage);
        result.setTotalPages((int) Math.ceil((double) entityPage.getTotalElements() / perPage));
        result.setItems(vms);

        return result;
    }

    public Vm getVm(UUID vmId) {
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found"));

        return mapEntityToApi(vm);
    }

    public Vm updateVm(UUID vmId, VmUpdateRequest request) {
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found"));

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

    public VmOperationResponse startVm(UUID vmId) {
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found"));

        // Validate state transition
        if (vm.getStatus() != VmStatus.STOPPED) {
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
        if (vm.getStatus() != VmStatus.ACTIVE) {
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
        if (vm.getStatus() != VmStatus.ACTIVE && vm.getStatus() != VmStatus.SUSPENDED) {
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
        if (vm.getStatus() != VmStatus.SUSPENDED) {
            throw new IllegalStateException("VM must be suspended to resume");
        }

        // Insert command into database queue
        QueueEntry commandEntry = buildResumeCommand(vm);
        queueEntryRepository.save(commandEntry);

        return buildOperationResponse(vmId, "VM resume initiated");
    }

    public void deleteVm(UUID vmId) {
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found"));

        // Update VM status
        vm.setStatus(VmStatus.DELETING);
        vm.setUpdatedAt(Instant.now());
        vmRepository.save(vm);

        // Insert command into database queue
        QueueEntry commandEntry = buildDeleteCommand(vm);
        queueEntryRepository.save(commandEntry);
    }

    public VmConsoleResponse getVmConsole(UUID vmId) {
        VmEntity vm = vmRepository.findById(vmId)
                .orElseThrow(() -> new RuntimeException("VM not found"));

        // Validate VM is running
        if (vm.getStatus() != VmStatus.ACTIVE) {
            throw new IllegalStateException("VM must be running to access console");
        }

        // Build console response
        VmConsoleResponse response = new VmConsoleResponse();
        response.setUrl(generateConsoleUrl(vm));
        response.setToken(generateConsoleToken(vm));
        response.setExpiresAt(Instant.now().plusSeconds(300)); // 5 minutes

        return response;
    }

    private Vm mapEntityToApi(VmEntity entity) {
        Vm vm = new Vm();
        vm.setId(entity.getId());
        vm.setName(entity.getName());
        vm.setDescription(entity.getDescription());
        vm.setStatus(entity.getStatus());
        vm.setPowerState(entity.getPowerState());
        vm.setTenantDatacenterGrantId(entity.getTenantDatacenterGrantId());
        vm.setSpec(entity.getSpec());
        vm.setProviderId(entity.getProviderId());
        vm.setNodeId(entity.getNodeId());
        vm.setExternalId(entity.getExternalId());
        vm.setIpAddresses(entity.getIpAddresses());
        vm.setHostname(entity.getHostname());
        vm.setResourceUsage(entity.getResourceUsage());
        vm.setMetadata(entity.getMetadata());
        vm.setTags(entity.getTags());
        vm.setCreatedAt(entity.getCreatedAt());
        vm.setUpdatedAt(entity.getUpdatedAt());
        vm.setStartedAt(entity.getStartedAt());
        vm.setStoppedAt(entity.getStoppedAt());

        return vm;
    }

    private VmOperationResponse buildOperationResponse(UUID vmId, String message) {
        VmOperationResponse response = new VmOperationResponse();
        response.setMessage(message);
        response.setOperationId(UUID.randomUUID());
        return response;
    }

    private QueueEntry buildCreateCommand(VmEntity vm, VmCreateRequest request) {
        return QueueEntry.builder()
                .queueType("VM_CREATE_COMMAND")
                .entityType(EntityType.VM)
                .entityId(vm.getId())
                .queueCategory(QueueCategory.COMMAND)
                .status(QueueStatus.PENDING)
                .actor(buildActor())
                .payload(Map.of(
                    "spec", request.getSpec(),
                    "tenantDatacenterGrantId", request.getTenantDatacenterGrantId(),
                    "name", request.getName(),
                    "description", request.getDescription(),
                    "metadata", request.getMetadata(),
                    "tags", request.getTags()
                ))
                .metadata(Map.of(
                    "source", "api",
                    "requestId", generateRequestId()
                ))
                .build();
    }

    private QueueEntry buildStartCommand(VmEntity vm) {
        return QueueEntry.builder()
                .queueType("VM_START_COMMAND")
                .entityType(EntityType.VM)
                .entityId(vm.getId())
                .queueCategory(QueueCategory.COMMAND)
                .status(QueueStatus.PENDING)
                .actor(buildActor())
                .payload(Map.of("vmId", vm.getId().toString()))
                .metadata(Map.of(
                    "source", "api",
                    "requestId", generateRequestId()
                ))
                .build();
    }

    private QueueEntry buildStopCommand(VmEntity vm) {
        return QueueEntry.builder()
                .queueType("VM_STOP_COMMAND")
                .entityType(EntityType.VM)
                .entityId(vm.getId())
                .queueCategory(QueueCategory.COMMAND)
                .status(QueueStatus.PENDING)
                .actor(buildActor())
                .payload(Map.of("vmId", vm.getId().toString()))
                .metadata(Map.of(
                    "source", "api",
                    "requestId", generateRequestId()
                ))
                .build();
    }

    private QueueEntry buildRestartCommand(VmEntity vm) {
        return QueueEntry.builder()
                .queueType("VM_RESTART_COMMAND")
                .entityType(EntityType.VM)
                .entityId(vm.getId())
                .queueCategory(QueueCategory.COMMAND)
                .status(QueueStatus.PENDING)
                .actor(buildActor())
                .payload(Map.of("vmId", vm.getId().toString()))
                .metadata(Map.of(
                    "source", "api",
                    "requestId", generateRequestId()
                ))
                .build();
    }

    private QueueEntry buildSuspendCommand(VmEntity vm) {
        return QueueEntry.builder()
                .queueType("VM_SUSPEND_COMMAND")
                .entityType(EntityType.VM)
                .entityId(vm.getId())
                .queueCategory(QueueCategory.COMMAND)
                .status(QueueStatus.PENDING)
                .actor(buildActor())
                .payload(Map.of("vmId", vm.getId().toString()))
                .metadata(Map.of(
                    "source", "api",
                    "requestId", generateRequestId()
                ))
                .build();
    }

    private QueueEntry buildResumeCommand(VmEntity vm) {
        return QueueEntry.builder()
                .queueType("VM_RESUME_COMMAND")
                .entityType(EntityType.VM)
                .entityId(vm.getId())
                .queueCategory(QueueCategory.COMMAND)
                .status(QueueStatus.PENDING)
                .actor(buildActor())
                .payload(Map.of("vmId", vm.getId().toString()))
                .metadata(Map.of(
                    "source", "api",
                    "requestId", generateRequestId()
                ))
                .build();
    }

    private QueueEntry buildDeleteCommand(VmEntity vm) {
        return QueueEntry.builder()
                .queueType("VM_DELETE_COMMAND")
                .entityType(EntityType.VM)
                .entityId(vm.getId())
                .queueCategory(QueueCategory.COMMAND)
                .status(QueueStatus.PENDING)
                .actor(buildActor())
                .payload(Map.of("vmId", vm.getId().toString()))
                .metadata(Map.of(
                    "source", "api",
                    "requestId", generateRequestId()
                ))
                .build();
    }

    private Map<String, Object> buildActor() {
        return Map.of(
            "type", "USER",
            "userId", getCurrentUserId()
        );
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

    private String getCurrentUserId() {
        // TODO: Get from security context
        return UUID.randomUUID().toString(); // Placeholder
    }
}
