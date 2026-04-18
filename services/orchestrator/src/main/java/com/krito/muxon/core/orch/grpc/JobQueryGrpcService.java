package com.krito.muxon.core.orch.grpc;

import com.krito.muxon.api.enums.EntityType;
import com.krito.muxon.api.enums.JobStatus;
import com.krito.muxon.core.orch.orch.JobService;
import com.krito.muxon.db.model.JobEntity;
import com.krito.muxon.grpc.workflow.v1.*;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.UUID;

/**
 * gRPC implementation of {@link JobQueryServiceGrpc.JobQueryServiceImplBase}.
 * Exposes job read + cancel operations to core-services for the REST /api/v1/jobs endpoints.
 */
@Service
public class JobQueryGrpcService extends JobQueryServiceGrpc.JobQueryServiceImplBase {

    private final JobService jobService;

    public JobQueryGrpcService(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void getJob(GetJobRequest request, StreamObserver<JobResponse> responseObserver) {
        try {
            JobResponse response = jobService.getJob(UUID.fromString(request.getJobId()));
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    io.grpc.Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void listJobs(ListJobsRequest request, StreamObserver<ListJobsResponse> responseObserver) {
        try {
            EntityType entityType = request.getEntityType().isBlank()
                    ? null : EntityType.valueOf(request.getEntityType().toUpperCase());
            UUID entityId = request.getEntityId().isBlank()
                    ? null : UUID.fromString(request.getEntityId());
            JobStatus status = request.getStatus().isBlank()
                    ? null : JobStatus.valueOf(request.getStatus().toUpperCase());

            int page = Math.max(0, request.getPage());
            int size = request.getSize() > 0 ? request.getSize() : 20;

            Page<JobEntity> jobPage = jobService.listJobs(
                    entityType, entityId, status,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

            ListJobsResponse.Builder builder = ListJobsResponse.newBuilder()
                    .setTotal((int) jobPage.getTotalElements());
            jobPage.getContent().forEach(job -> builder.addJobs(JobService.toResponse(job)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(
                    io.grpc.Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void cancelJob(CancelJobRequest request, StreamObserver<CancelJobResponse> responseObserver) {
        try {
            jobService.cancelJob(UUID.fromString(request.getJobId()), request.getReason());
            responseObserver.onNext(CancelJobResponse.newBuilder().setAccepted(true).build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onNext(CancelJobResponse.newBuilder()
                    .setAccepted(false)
                    .setErrorMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        } catch (IllegalStateException e) {
            responseObserver.onNext(CancelJobResponse.newBuilder()
                    .setAccepted(false)
                    .setErrorMessage(e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }
}
