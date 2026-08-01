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

import com.yorel.muxon.api.dto.TaskCancelRequest;
import com.yorel.muxon.api.dto.TaskCreateRequest;
import com.yorel.muxon.api.dto.TaskResponse;
import com.yorel.muxon.api.enums.EntityType;
import com.yorel.muxon.api.enums.JobStatus;
import com.yorel.muxon.api.enums.JobType;
import com.yorel.muxon.db.model.JobEntity;
import com.yorel.muxon.services.task.TaskMonitoringService;
import com.yorel.muxon.services.task.TaskOrchestrationService;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class TasksController extends BaseController {

  @Autowired private TaskOrchestrationService taskOrchestrationService;

  @Autowired private TaskMonitoringService taskMonitoringService;

  @PostMapping
  public ResponseEntity<TaskResponse> createTask(@RequestBody TaskCreateRequest request) {
    try {
      JobEntity task = taskOrchestrationService.createTask(request);
      TaskResponse response = taskOrchestrationService.getTaskDetails(task.getId());
      return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @GetMapping("/{taskId}")
  public ResponseEntity<TaskResponse> getTask(@PathVariable UUID taskId) {
    try {
      TaskResponse response = taskOrchestrationService.getTaskDetails(taskId);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping
  public ResponseEntity<Page<TaskResponse>> listTasks(
      @RequestParam(required = false) EntityType entityType,
      @RequestParam(required = false) UUID entityId,
      @RequestParam(required = false) JobStatus status,
      @RequestParam(required = false) JobType operation,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    PageRequest pageRequest = PageRequest.of(page, size);
    Page<TaskResponse> tasks =
        taskOrchestrationService.listTasks(entityType, entityId, status, operation, pageRequest);

    return ResponseEntity.ok(tasks);
  }

  @PostMapping("/{taskId}/cancel")
  public ResponseEntity<TaskResponse> cancelTask(
      @PathVariable UUID taskId, @RequestBody TaskCancelRequest request) {
    try {
      JobEntity task = taskOrchestrationService.cancelTask(taskId, request.getReason());
      TaskResponse response = taskOrchestrationService.getTaskDetails(task.getId());
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    } catch (IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  @GetMapping("/stats")
  public ResponseEntity<TaskMonitoringService.TaskMonitoringStats> getStats() {
    TaskMonitoringService.TaskMonitoringStats stats = taskMonitoringService.getMonitoringStats();
    return ResponseEntity.ok(stats);
  }

  @Override
  protected Map<String, String> getEntityMetadata(UUID id) {
    return Map.of();
  }

  @Override
  protected Map<String, String> updateEntityMetadata(UUID id, Map<String, String> metadata) {
    return Map.of();
  }
}
