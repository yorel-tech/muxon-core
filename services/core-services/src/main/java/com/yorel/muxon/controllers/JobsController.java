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
package com.yorel.muxon.controllers;

import com.yorel.muxon.auth.Permission;
import com.yorel.muxon.auth.RequiresPermission;
import com.yorel.muxon.grpc.workflow.v1.CancelJobRequest;
import com.yorel.muxon.grpc.workflow.v1.CancelJobResponse;
import com.yorel.muxon.grpc.workflow.v1.GetJobRequest;
import com.yorel.muxon.grpc.workflow.v1.JobQueryServiceGrpc;
import com.yorel.muxon.grpc.workflow.v1.JobResponse;
import com.yorel.muxon.grpc.workflow.v1.ListJobsRequest;
import com.yorel.muxon.grpc.workflow.v1.ListJobsResponse;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the /api/v1/jobs endpoints.
 *
 * <p>Read-only + cancel. Jobs are never created directly; they are always created as side effects
 * of workflow API calls (e.g. POST /api/v1/vms).
 *
 * <p>All requests are forwarded to the orchestrator's {@link JobQueryServiceGrpc} via gRPC.
 */
@RestController
@RequestMapping("/api/v1/jobs")
public class JobsController {

  private static final Logger log = LoggerFactory.getLogger(JobsController.class);

  @Autowired private JobQueryServiceGrpc.JobQueryServiceBlockingStub jobQueryStub;

  /**
   * List jobs, optionally filtered by entity.
   *
   * @param entityId required in practice — scope results to a specific entity
   * @param entityType VM | NODE | CONTENT_LIBRARY etc. (optional)
   * @param status PENDING | RUNNING | COMPLETED | FAILED | CANCELLED (optional)
   * @param page 0-based page index (default 0)
   * @param size page size (default 20)
   */
  @GetMapping
  @RequiresPermission(Permission.VM_READ)
  public ResponseEntity<?> listJobs(
      @RequestParam(required = false) UUID entityId,
      @RequestParam(required = false) String entityType,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    try {
      ListJobsRequest.Builder req = ListJobsRequest.newBuilder().setPage(page).setSize(size);
      if (entityId != null) req.setEntityId(entityId.toString());
      if (entityType != null) req.setEntityType(entityType);
      if (status != null) req.setStatus(status);

      ListJobsResponse response = jobQueryStub.listJobs(req.build());
      return ResponseEntity.ok(toRestResponse(response));
    } catch (StatusRuntimeException e) {
      return handleGrpcError(e);
    }
  }

  /** Get a single job by ID. */
  @GetMapping("/{jobId}")
  @RequiresPermission(Permission.VM_READ)
  public ResponseEntity<?> getJob(@PathVariable UUID jobId) {
    try {
      JobResponse response =
          jobQueryStub.getJob(GetJobRequest.newBuilder().setJobId(jobId.toString()).build());
      return ResponseEntity.ok(toJobDto(response));
    } catch (StatusRuntimeException e) {
      return handleGrpcError(e);
    }
  }

  /** Cancel a job. */
  @PostMapping("/{jobId}/cancel")
  @RequiresPermission(Permission.VM_MANAGE)
  public ResponseEntity<?> cancelJob(
      @PathVariable UUID jobId, @RequestBody(required = false) Map<String, String> body) {
    try {
      String reason =
          body != null
              ? body.getOrDefault("reason", "User requested cancellation")
              : "User requested cancellation";
      CancelJobResponse response =
          jobQueryStub.cancelJob(
              CancelJobRequest.newBuilder().setJobId(jobId.toString()).setReason(reason).build());
      if (response.getAccepted()) {
        return ResponseEntity.ok(Map.of("accepted", true, "jobId", jobId.toString()));
      } else {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("accepted", false, "error", response.getErrorMessage()));
      }
    } catch (StatusRuntimeException e) {
      return handleGrpcError(e);
    }
  }

  // ── mapping helpers ────────────────────────────────────────────────────────

  private Map<String, Object> toJobDto(JobResponse r) {
    return Map.of(
        "id", r.getJobId(),
        "status", r.getStatus(),
        "entityId", r.getEntityId(),
        "entityType", r.getEntityType(),
        "taskCount", r.getTaskCount(),
        "tasksDone", r.getTasksDone(),
        "createdAt", r.getCreatedAt(),
        "errorMessage", r.getErrorMessage());
  }

  private Map<String, Object> toRestResponse(ListJobsResponse response) {
    List<Map<String, Object>> items = response.getJobsList().stream().map(this::toJobDto).toList();
    return Map.of("jobs", items, "total", response.getTotal());
  }

  private ResponseEntity<?> handleGrpcError(StatusRuntimeException e) {
    return switch (e.getStatus().getCode()) {
      case NOT_FOUND -> ResponseEntity.notFound().build();
      case INVALID_ARGUMENT ->
          ResponseEntity.badRequest().body(Map.of("error", e.getStatus().getDescription()));
      default -> {
        log.error("gRPC error from orchestrator: {}", e.getMessage());
        yield ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of("error", "Orchestrator unavailable: " + e.getStatus().getDescription()));
      }
    };
  }
}
