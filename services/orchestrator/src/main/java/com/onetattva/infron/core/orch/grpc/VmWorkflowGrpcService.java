package com.onetattva.infron.core.orch.grpc;

import com.onetattva.infron.api.enums.EntityType;
import com.onetattva.infron.api.enums.JobType;
import com.onetattva.infron.core.orch.orch.JobService;
import com.onetattva.infron.core.spi.queue.VmConsoleResolvePayloadKeys;
import com.onetattva.infron.core.spi.queue.VmQueueCommands;
import com.onetattva.infron.grpc.workflow.v1.*;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * gRPC implementation of {@link VMWorkflowServiceGrpc.VMWorkflowServiceImplBase}.
 * Each method creates a Job + enqueues a task via {@link JobService} and returns a {@link JobResponse}.
 */
@Service
public class VmWorkflowGrpcService extends VMWorkflowServiceGrpc.VMWorkflowServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(VmWorkflowGrpcService.class);

    private final JobService jobService;

    public VmWorkflowGrpcService(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void createVM(CreateVMRequest request, StreamObserver<JobResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            Map<String, Object> payload = new HashMap<>();
            payload.put("specJson", request.getSpecJson());
            payload.put("tenantDatacenterGrantId", request.getTenantDatacenterGrantId());
            if (!request.getSourceImagePath().isBlank()) {
                payload.put("sourceImagePath", request.getSourceImagePath());
            }
            if (!request.getIsoContentIdsList().isEmpty()) {
                payload.put("isoContentItemIds", request.getIsoContentIdsList());
            }
            log.debug(
                    "VM workflow gRPC createVM: vmId={}, correlationId={}, grantId={}, specJsonChars={}, "
                            + "sourceImagePathSet={}, isoContentIdCount={}",
                    request.getVmId(),
                    request.getCorrelationId(),
                    request.getTenantDatacenterGrantId(),
                    request.getSpecJson() != null ? request.getSpecJson().length() : 0,
                    !request.getSourceImagePath().isBlank(),
                    request.getIsoContentIdsList().size());
            return jobService.createJob(
                    JobType.VM_CREATE, EntityType.VM,
                    UUID.fromString(request.getVmId()),
                    request.getCorrelationId(),
                    "VM_CREATE_COMMAND", payload);
        });
    }

    @Override
    public void deleteVM(DeleteVMRequest request, StreamObserver<JobResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            log.debug(
                    "VM workflow gRPC deleteVM: vmId={}, correlationId={}, externalIdSet={}, grantIdSet={}",
                    request.getVmId(),
                    request.getCorrelationId(),
                    !request.getExternalId().isBlank(),
                    !request.getGrantId().isBlank());
            Map<String, Object> payload = simpleVmPayload(request.getExternalId(), request.getGrantId());
            return jobService.createJob(
                    JobType.VM_DELETE, EntityType.VM,
                    UUID.fromString(request.getVmId()),
                    request.getCorrelationId(),
                    "VM_DELETE_COMMAND", payload);
        });
    }

    @Override
    public void powerOnVM(PowerOnVMRequest request, StreamObserver<JobResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            log.debug(
                    "VM workflow gRPC powerOnVM: vmId={}, correlationId={}, grantIdSet={}, externalIdSet={}",
                    request.getVmId(),
                    request.getCorrelationId(),
                    !request.getGrantId().isBlank(),
                    !request.getExternalId().isBlank());
            Map<String, Object> payload = simpleVmPayload(request.getExternalId(), request.getGrantId());
            return jobService.createJob(
                    JobType.VM_START, EntityType.VM,
                    UUID.fromString(request.getVmId()),
                    request.getCorrelationId(),
                    "VM_START_COMMAND", payload);
        });
    }

    @Override
    public void powerOffVM(PowerOffVMRequest request, StreamObserver<JobResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            log.debug(
                    "VM workflow gRPC powerOffVM: vmId={}, correlationId={}, grantIdSet={}, externalIdSet={}",
                    request.getVmId(),
                    request.getCorrelationId(),
                    !request.getGrantId().isBlank(),
                    !request.getExternalId().isBlank());
            Map<String, Object> payload = simpleVmPayload(request.getExternalId(), request.getGrantId());
            return jobService.createJob(
                    JobType.VM_STOP, EntityType.VM,
                    UUID.fromString(request.getVmId()),
                    request.getCorrelationId(),
                    "VM_STOP_COMMAND", payload);
        });
    }

    @Override
    public void restartVM(RestartVMRequest request, StreamObserver<JobResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            log.debug(
                    "VM workflow gRPC restartVM: vmId={}, correlationId={}, grantIdSet={}, externalIdSet={}",
                    request.getVmId(),
                    request.getCorrelationId(),
                    !request.getGrantId().isBlank(),
                    !request.getExternalId().isBlank());
            Map<String, Object> payload = simpleVmPayload(request.getExternalId(), request.getGrantId());
            return jobService.createJob(
                    JobType.VM_RESTART, EntityType.VM,
                    UUID.fromString(request.getVmId()),
                    request.getCorrelationId(),
                    "VM_RESTART_COMMAND", payload);
        });
    }

    @Override
    public void suspendVM(SuspendVMRequest request, StreamObserver<JobResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            log.debug(
                    "VM workflow gRPC suspendVM: vmId={}, correlationId={}, grantIdSet={}, externalIdSet={}",
                    request.getVmId(),
                    request.getCorrelationId(),
                    !request.getGrantId().isBlank(),
                    !request.getExternalId().isBlank());
            Map<String, Object> payload = simpleVmPayload(request.getExternalId(), request.getGrantId());
            return jobService.createJob(
                    JobType.VM_SUSPEND, EntityType.VM,
                    UUID.fromString(request.getVmId()),
                    request.getCorrelationId(),
                    "VM_SUSPEND_COMMAND", payload);
        });
    }

    @Override
    public void resumeVM(ResumeVMRequest request, StreamObserver<JobResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            log.debug(
                    "VM workflow gRPC resumeVM: vmId={}, correlationId={}, grantIdSet={}, externalIdSet={}",
                    request.getVmId(),
                    request.getCorrelationId(),
                    !request.getGrantId().isBlank(),
                    !request.getExternalId().isBlank());
            Map<String, Object> payload = simpleVmPayload(request.getExternalId(), request.getGrantId());
            return jobService.createJob(
                    JobType.VM_RESUME, EntityType.VM,
                    UUID.fromString(request.getVmId()),
                    request.getCorrelationId(),
                    "VM_RESUME_COMMAND", payload);
        });
    }

    @Override
    public void migrateVM(MigrateVMRequest request, StreamObserver<JobResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            log.debug(
                    "VM workflow gRPC migrateVM: vmId={}, correlationId={}, targetNodeIdSet={}, grantIdSet={}",
                    request.getVmId(),
                    request.getCorrelationId(),
                    !request.getTargetNodeId().isBlank(),
                    !request.getGrantId().isBlank());
            Map<String, Object> payload = simpleVmPayload(request.getExternalId(), request.getGrantId());
            if (!request.getTargetNodeId().isBlank()) {
                payload.put("targetNodeId", request.getTargetNodeId());
            }
            return jobService.createJob(
                    JobType.VM_MIGRATE, EntityType.VM,
                    UUID.fromString(request.getVmId()),
                    request.getCorrelationId(),
                    "VM_MIGRATE_COMMAND", payload);
        });
    }

    @Override
    public void attachIso(AttachIsoVMRequest request, StreamObserver<JobResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            log.debug(
                    "VM workflow gRPC attachIso: vmId={}, correlationId={}, isoContentItemId={}, grantIdSet={}",
                    request.getVmId(),
                    request.getCorrelationId(),
                    request.getIsoContentId(),
                    !request.getGrantId().isBlank());
            Map<String, Object> payload = simpleVmPayload(request.getExternalId(), request.getGrantId());
            payload.put("isoContentItemId", request.getIsoContentId());
            return jobService.createJob(
                    JobType.VM_CREATE, EntityType.VM, // reuse nearest type; extend JobType if needed
                    UUID.fromString(request.getVmId()),
                    request.getCorrelationId(),
                    "VM_ATTACH_ISO_COMMAND", payload);
        });
    }

    @Override
    public void detachIso(DetachIsoVMRequest request, StreamObserver<JobResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            log.debug(
                    "VM workflow gRPC detachIso: vmId={}, correlationId={}, deviceName={}, grantIdSet={}",
                    request.getVmId(),
                    request.getCorrelationId(),
                    request.getDeviceName(),
                    !request.getGrantId().isBlank());
            Map<String, Object> payload = simpleVmPayload(request.getExternalId(), request.getGrantId());
            payload.put("deviceName", request.getDeviceName());
            return jobService.createJob(
                    JobType.VM_CREATE, EntityType.VM,
                    UUID.fromString(request.getVmId()),
                    request.getCorrelationId(),
                    "VM_DETACH_ISO_COMMAND", payload);
        });
    }

    @Override
    public void publishVMTemplate(PublishVMTemplateRequest request, StreamObserver<JobResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            log.debug(
                    "VM workflow gRPC publishVMTemplate: vmId={}, correlationId={}, contentItemId={}, grantIdSet={}",
                    request.getVmId(),
                    request.getCorrelationId(),
                    request.getContentItemId(),
                    !request.getGrantId().isBlank());
            Map<String, Object> payload = simpleVmPayload(request.getExternalId(), request.getGrantId());
            payload.put("contentItemId", request.getContentItemId());
            return jobService.createJob(
                    JobType.VM_CREATE, EntityType.VM,
                    UUID.fromString(request.getVmId()),
                    request.getCorrelationId(),
                    "VM_PUBLISH_TEMPLATE_COMMAND", payload);
        });
    }

    @Override
    public void resolveConsole(ResolveConsoleRequest request, StreamObserver<JobResponse> responseObserver) {
        handleRequest(responseObserver, () -> {
            log.debug(
                    "VM workflow gRPC resolveConsole: vmId={}, correlationId={}, grantIdSet={}, "
                            + "externalIdSet={}, nodeIdSet={}, providerIdSet={}",
                    request.getVmId(),
                    request.getCorrelationId(),
                    !request.getGrantId().isBlank(),
                    !request.getExternalId().isBlank(),
                    !request.getNodeId().isBlank(),
                    !request.getProviderId().isBlank());
            Map<String, Object> payload = new HashMap<>();
            payload.put("tenantDatacenterGrantId", request.getGrantId());
            payload.put("externalId", request.getExternalId());
            payload.put("nodeId", request.getNodeId());
            if (!request.getProviderId().isBlank()) {
                payload.put(VmConsoleResolvePayloadKeys.PROVIDER_ID, request.getProviderId());
            }
            return jobService.createJob(
                    JobType.VM_CREATE, EntityType.VM,
                    UUID.fromString(request.getVmId()),
                    request.getCorrelationId(),
                    VmQueueCommands.CONSOLE_RESOLVE, payload);
        });
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Map<String, Object> simpleVmPayload(String externalId, String grantId) {
        Map<String, Object> p = new HashMap<>();
        if (!externalId.isBlank()) p.put("externalId", externalId);
        if (!grantId.isBlank())    p.put("tenantDatacenterGrantId", grantId);
        return p;
    }

    @FunctionalInterface
    private interface JobSupplier {
        JobResponse get() throws Exception;
    }

    private void handleRequest(StreamObserver<JobResponse> obs, JobSupplier supplier) {
        try {
            obs.onNext(supplier.get());
            obs.onCompleted();
        } catch (IllegalArgumentException e) {
            obs.onError(io.grpc.Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC error in VmWorkflowGrpcService", e);
            obs.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
