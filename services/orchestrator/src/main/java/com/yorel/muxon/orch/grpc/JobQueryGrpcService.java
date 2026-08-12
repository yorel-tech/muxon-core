/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.orch.grpc;

import com.yorel.muxon.api.enums.EntityType;
import com.yorel.muxon.api.enums.JobStatus;
import com.yorel.muxon.db.model.JobEntity;
import com.yorel.muxon.grpc.workflow.v1.CancelJobRequest;
import com.yorel.muxon.grpc.workflow.v1.CancelJobResponse;
import com.yorel.muxon.grpc.workflow.v1.GetJobRequest;
import com.yorel.muxon.grpc.workflow.v1.JobQueryServiceGrpc;
import com.yorel.muxon.grpc.workflow.v1.JobResponse;
import com.yorel.muxon.grpc.workflow.v1.ListJobsRequest;
import com.yorel.muxon.grpc.workflow.v1.ListJobsResponse;
import com.yorel.muxon.orch.orch.JobService;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * gRPC implementation of {@link JobQueryServiceGrpc.JobQueryServiceImplBase}. Exposes job read +
 * cancel operations to core-services for the REST /api/v1/jobs endpoints.
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
      EntityType entityType =
          request.getEntityType().isBlank()
              ? null
              : EntityType.valueOf(request.getEntityType().toUpperCase());
      UUID entityId =
          request.getEntityId().isBlank() ? null : UUID.fromString(request.getEntityId());
      JobStatus status =
          request.getStatus().isBlank()
              ? null
              : JobStatus.valueOf(request.getStatus().toUpperCase());

      int page = Math.max(0, request.getPage());
      int size = request.getSize() > 0 ? request.getSize() : 20;

      Page<JobEntity> jobPage =
          jobService.listJobs(
              entityType,
              entityId,
              status,
              PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

      ListJobsResponse.Builder builder =
          ListJobsResponse.newBuilder().setTotal((int) jobPage.getTotalElements());
      jobPage.getContent().forEach(job -> builder.addJobs(JobService.toResponse(job)));

      responseObserver.onNext(builder.build());
      responseObserver.onCompleted();
    } catch (IllegalArgumentException e) {
      responseObserver.onError(
          io.grpc.Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void cancelJob(
      CancelJobRequest request, StreamObserver<CancelJobResponse> responseObserver) {
    try {
      jobService.cancelJob(UUID.fromString(request.getJobId()), request.getReason());
      responseObserver.onNext(CancelJobResponse.newBuilder().setAccepted(true).build());
      responseObserver.onCompleted();
    } catch (IllegalArgumentException e) {
      responseObserver.onNext(
          CancelJobResponse.newBuilder()
              .setAccepted(false)
              .setErrorMessage(e.getMessage())
              .build());
      responseObserver.onCompleted();
    } catch (IllegalStateException e) {
      responseObserver.onNext(
          CancelJobResponse.newBuilder()
              .setAccepted(false)
              .setErrorMessage(e.getMessage())
              .build());
      responseObserver.onCompleted();
    }
  }
}
