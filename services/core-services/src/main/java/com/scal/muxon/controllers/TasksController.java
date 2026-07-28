package com.scal.muxon.controllers;

import com.scal.muxon.api.dto.*;
import com.scal.muxon.api.enums.EntityType;
import com.scal.muxon.api.enums.JobStatus;
import com.scal.muxon.api.enums.JobType;
import com.scal.muxon.services.task.TaskMonitoringService;
import com.scal.muxon.services.task.TaskOrchestrationService;
import com.scal.muxon.db.model.JobEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
public class TasksController extends BaseController {

    @Autowired
    private TaskOrchestrationService taskOrchestrationService;

    @Autowired
    private TaskMonitoringService taskMonitoringService;

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
        Page<TaskResponse> tasks = taskOrchestrationService.listTasks(
            entityType, entityId, status, operation, pageRequest);
        
        return ResponseEntity.ok(tasks);
    }

    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<TaskResponse> cancelTask(
            @PathVariable UUID taskId,
            @RequestBody TaskCancelRequest request) {
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
