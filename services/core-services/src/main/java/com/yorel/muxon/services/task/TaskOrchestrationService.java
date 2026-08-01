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
package com.yorel.muxon.services.task;

import com.yorel.muxon.api.dto.TaskCreateRequest;
import com.yorel.muxon.api.dto.TaskLogResponse;
import com.yorel.muxon.api.dto.TaskProgressResponse;
import com.yorel.muxon.api.dto.TaskResponse;
import com.yorel.muxon.api.dto.TaskStepResponse;
import com.yorel.muxon.api.dto.TaskTargetEntityResponse;
import com.yorel.muxon.api.dto.TaskTimingResponse;
import com.yorel.muxon.api.enums.EntityType;
import com.yorel.muxon.api.enums.JobStatus;
import com.yorel.muxon.api.enums.JobType;
import com.yorel.muxon.db.model.JobEntity;
import com.yorel.muxon.db.model.TaskLogEntity;
import com.yorel.muxon.db.model.TaskStepEntity;
import com.yorel.muxon.db.model.VmEntity;
import com.yorel.muxon.db.repository.JobRepository;
import com.yorel.muxon.db.repository.TaskLogRepository;
import com.yorel.muxon.db.repository.TaskStepRepository;
import com.yorel.muxon.db.repository.VmRepository;
import com.yorel.muxon.services.statemachine.EntityStateMachine;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class TaskOrchestrationService {

  private static final Logger logger = LoggerFactory.getLogger(TaskOrchestrationService.class);

  @Autowired private JobRepository jobRepository;

  @Autowired private TaskStepRepository taskStepRepository;

  @Autowired private TaskLogRepository taskLogRepository;

  @Autowired private VmRepository vmRepository;

  @Autowired private EntityStateMachine entityStateMachine;

  @Autowired private ObjectMapper objectMapper;

  @Value("${muxon.tasks.default-timeout:3600}")
  private int defaultTimeoutSeconds;

  @Value("${muxon.tasks.max-logs-per-task:1000}")
  private int maxLogsPerTask;

  @Transactional
  public JobEntity createTask(TaskCreateRequest request) {
    JobEntity task = new JobEntity();
    task.setJobType(request.getOperation());
    task.setStatus(JobStatus.PENDING);
    task.setTargetEntityType(request.getEntityType());
    task.setTargetEntityId(request.getEntityId());
    task.setProgressPercentage(0);
    task.setRetryCount(0);
    task.setMaxRetries(3);
    task.setCancellationRequested(false);

    if (request.getParameters() != null) {
      try {
        task.setParameters(objectMapper.writeValueAsString(request.getParameters()));
      } catch (JacksonException e) {
        logger.error("Failed to serialize task parameters", e);
      }
    }

    if (request.getMetadata() != null) {
      try {
        task.setMetadata(objectMapper.writeValueAsString(request.getMetadata()));
      } catch (JacksonException e) {
        logger.error("Failed to serialize task metadata", e);
      }
    }

    int timeoutSeconds =
        request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : defaultTimeoutSeconds;
    task.setTimeoutAt(Instant.now().plusSeconds(timeoutSeconds));

    entityStateMachine.validateStateForTask(request.getEntityId(), task);

    JobEntity savedTask = jobRepository.save(task);

    logTaskEvent(savedTask.getId(), null, "INFO", "Task created: " + request.getOperation());

    return savedTask;
  }

  @Transactional
  public JobEntity startTask(UUID taskId) {
    JobEntity task =
        jobRepository
            .findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

    if (task.getStatus() != JobStatus.PENDING) {
      throw new IllegalStateException("Task is not in PENDING state: " + task.getStatus());
    }

    task.setStatus(JobStatus.RUNNING);
    task.setStartedAt(Instant.now());
    task.setLastHeartbeatAt(Instant.now());

    JobEntity savedTask = jobRepository.save(task);

    entityStateMachine.updateStateForTaskStatus(task.getTargetEntityId(), task, JobStatus.RUNNING);

    logTaskEvent(taskId, null, "INFO", "Task started");

    return savedTask;
  }

  @Transactional
  public JobEntity updateProgress(UUID taskId, int percentage, String currentStep) {
    JobEntity task =
        jobRepository
            .findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

    task.setProgressPercentage(Math.min(100, Math.max(0, percentage)));
    task.setCurrentStep(currentStep);
    task.setLastHeartbeatAt(Instant.now());

    return jobRepository.save(task);
  }

  @Transactional
  public TaskStepEntity createStep(
      UUID taskId, int stepNumber, String stepName, String stepDescription) {
    JobEntity task =
        jobRepository
            .findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

    TaskStepEntity step = new TaskStepEntity();
    step.setTaskId(taskId);
    step.setStepNumber(stepNumber);
    step.setStepName(stepName);
    step.setStepDescription(stepDescription);
    step.setStatus(JobStatus.PENDING);

    TaskStepEntity savedStep = taskStepRepository.save(step);

    if (task.getTotalSteps() == null || task.getTotalSteps() < stepNumber) {
      task.setTotalSteps(stepNumber);
      jobRepository.save(task);
    }

    return savedStep;
  }

  @Transactional
  public TaskStepEntity startStep(UUID taskId, int stepNumber) {
    TaskStepEntity step =
        taskStepRepository
            .findByTaskIdAndStepNumber(taskId, stepNumber)
            .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepNumber));

    step.setStatus(JobStatus.RUNNING);
    step.setStartedAt(Instant.now());

    TaskStepEntity savedStep = taskStepRepository.save(step);

    JobEntity task = jobRepository.findById(taskId).orElse(null);
    if (task != null) {
      task.setCurrentStep(step.getStepDescription());
      task.setLastHeartbeatAt(Instant.now());
      jobRepository.save(task);
    }

    logTaskEvent(
        taskId, step.getId(), "INFO", "Step " + stepNumber + " started: " + step.getStepName());

    return savedStep;
  }

  @Transactional
  public TaskStepEntity completeStep(UUID taskId, int stepNumber, Object output) {
    TaskStepEntity step =
        taskStepRepository
            .findByTaskIdAndStepNumber(taskId, stepNumber)
            .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepNumber));

    step.setStatus(JobStatus.COMPLETED);
    step.setCompletedAt(Instant.now());

    if (output != null) {
      try {
        step.setOutput(objectMapper.writeValueAsString(output));
      } catch (JacksonException e) {
        logger.error("Failed to serialize step output", e);
      }
    }

    TaskStepEntity savedStep = taskStepRepository.save(step);

    long completedSteps = taskStepRepository.countCompletedSteps(taskId);
    long totalSteps = taskStepRepository.countTotalSteps(taskId);

    if (totalSteps > 0) {
      int percentage = (int) ((completedSteps * 100) / totalSteps);
      updateProgress(taskId, percentage, step.getStepDescription());
    }

    logTaskEvent(
        taskId, step.getId(), "INFO", "Step " + stepNumber + " completed: " + step.getStepName());

    return savedStep;
  }

  @Transactional
  public TaskStepEntity failStep(UUID taskId, int stepNumber, String errorMessage) {
    TaskStepEntity step =
        taskStepRepository
            .findByTaskIdAndStepNumber(taskId, stepNumber)
            .orElseThrow(() -> new IllegalArgumentException("Step not found: " + stepNumber));

    step.setStatus(JobStatus.FAILED);
    step.setCompletedAt(Instant.now());
    step.setErrorMessage(errorMessage);

    TaskStepEntity savedStep = taskStepRepository.save(step);

    logTaskEvent(taskId, step.getId(), "ERROR", "Step " + stepNumber + " failed: " + errorMessage);

    return savedStep;
  }

  @Transactional
  public JobEntity completeTask(UUID taskId, Object result) {
    JobEntity task =
        jobRepository
            .findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

    task.setStatus(JobStatus.COMPLETED);
    task.setCompletedAt(Instant.now());
    task.setProgressPercentage(100);

    if (result != null) {
      try {
        task.setResult(objectMapper.writeValueAsString(result));
      } catch (JacksonException e) {
        logger.error("Failed to serialize task result", e);
      }
    }

    JobEntity savedTask = jobRepository.save(task);

    entityStateMachine.updateStateForTaskStatus(
        task.getTargetEntityId(), task, JobStatus.COMPLETED);

    logTaskEvent(taskId, null, "INFO", "Task completed successfully");

    return savedTask;
  }

  @Transactional
  public JobEntity failTask(UUID taskId, String errorMessage) {
    JobEntity task =
        jobRepository
            .findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

    task.setStatus(JobStatus.FAILED);
    task.setCompletedAt(Instant.now());
    task.setErrorMessage(errorMessage);

    JobEntity savedTask = jobRepository.save(task);

    entityStateMachine.updateStateForTaskStatus(task.getTargetEntityId(), task, JobStatus.FAILED);

    logTaskEvent(taskId, null, "ERROR", "Task failed: " + errorMessage);

    return savedTask;
  }

  @Transactional
  public JobEntity cancelTask(UUID taskId, String reason) {
    JobEntity task =
        jobRepository
            .findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

    if (task.getStatus() == JobStatus.COMPLETED || task.getStatus() == JobStatus.FAILED) {
      throw new IllegalStateException("Cannot cancel task in state: " + task.getStatus());
    }

    task.setCancellationRequested(true);
    task.setCancellationReason(reason);

    if (task.getStatus() == JobStatus.PENDING) {
      task.setStatus(JobStatus.CANCELLED);
      task.setCompletedAt(Instant.now());
      entityStateMachine.updateStateForTaskStatus(
          task.getTargetEntityId(), task, JobStatus.CANCELLED);
    }

    JobEntity savedTask = jobRepository.save(task);

    logTaskEvent(taskId, null, "WARN", "Task cancellation requested: " + reason);

    return savedTask;
  }

  @Transactional
  public void heartbeat(UUID taskId) {
    JobEntity task = jobRepository.findById(taskId).orElse(null);
    if (task != null && task.getStatus() == JobStatus.RUNNING) {
      task.setLastHeartbeatAt(Instant.now());
      jobRepository.save(task);
    }
  }

  @Transactional
  public void logTaskEvent(UUID taskId, UUID stepId, String level, String message) {
    long logCount = taskLogRepository.countByTaskId(taskId);
    if (logCount >= maxLogsPerTask) {
      logger.warn("Task {} has reached max log limit ({}), skipping log", taskId, maxLogsPerTask);
      return;
    }

    TaskLogEntity log = new TaskLogEntity();
    log.setTaskId(taskId);
    log.setStepId(stepId);
    log.setLogLevel(level);
    log.setMessage(message);
    taskLogRepository.save(log);
  }

  public TaskResponse getTaskDetails(UUID taskId) {
    JobEntity task =
        jobRepository
            .findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

    return buildTaskResponse(task, true, true);
  }

  public Page<TaskResponse> listTasks(
      EntityType entityType,
      UUID entityId,
      JobStatus status,
      JobType operation,
      Pageable pageable) {
    Page<JobEntity> tasks;

    if (entityType != null && entityId != null) {
      if (status != null) {
        tasks =
            jobRepository.findByTargetEntityTypeAndTargetEntityIdAndStatus(
                entityType, entityId, status, pageable);
      } else {
        tasks =
            jobRepository.findByTargetEntityTypeAndTargetEntityId(entityType, entityId, pageable);
      }
    } else if (status != null) {
      tasks = jobRepository.findByStatus(status, pageable);
    } else if (operation != null) {
      tasks = jobRepository.findByJobType(operation, pageable);
    } else {
      tasks = jobRepository.findAll(pageable);
    }

    return tasks.map(task -> buildTaskResponse(task, false, false));
  }

  private TaskResponse buildTaskResponse(
      JobEntity task, boolean includeSteps, boolean includeLogs) {
    TaskResponse response = new TaskResponse();
    response.setTaskId(task.getId());
    response.setStatus(task.getStatus());
    response.setOperation(task.getJobType());
    response.setCancellationRequested(task.getCancellationRequested());
    response.setCancellationReason(task.getCancellationReason());
    response.setErrorMessage(task.getErrorMessage());

    TaskTargetEntityResponse targetEntity = new TaskTargetEntityResponse();
    targetEntity.setType(task.getTargetEntityType());
    targetEntity.setId(task.getTargetEntityId());

    if (task.getTargetEntityType() == EntityType.VM) {
      VmEntity vm = vmRepository.findById(task.getTargetEntityId()).orElse(null);
      if (vm != null) {
        targetEntity.setName(vm.getName());
      }
    }
    response.setTargetEntity(targetEntity);

    TaskProgressResponse progress = new TaskProgressResponse();
    progress.setPercentage(task.getProgressPercentage());
    progress.setCurrentStep(task.getCurrentStep());
    progress.setTotalSteps(task.getTotalSteps());

    if (includeSteps) {
      List<TaskStepEntity> steps =
          taskStepRepository.findByTaskIdOrderByStepNumberAsc(task.getId());
      Optional<TaskStepEntity> runningStep =
          steps.stream().filter(s -> s.getStatus() == JobStatus.RUNNING).findFirst();
      runningStep.ifPresent(step -> progress.setCurrentStepNumber(step.getStepNumber()));
    }
    response.setProgress(progress);

    TaskTimingResponse timing = new TaskTimingResponse();
    timing.setCreatedAt(task.getCreatedAt());
    timing.setStartedAt(task.getStartedAt());
    timing.setCompletedAt(task.getCompletedAt());
    timing.setTimeoutAt(task.getTimeoutAt());

    if (task.getStartedAt() != null && task.getCompletedAt() != null) {
      timing.setDurationSeconds(
          Duration.between(task.getStartedAt(), task.getCompletedAt()).toMillis() / 1000.0);
    } else if (task.getStartedAt() != null) {
      timing.setDurationSeconds(
          Duration.between(task.getStartedAt(), Instant.now()).toMillis() / 1000.0);
    }
    response.setTiming(timing);

    if (includeSteps) {
      List<TaskStepEntity> steps =
          taskStepRepository.findByTaskIdOrderByStepNumberAsc(task.getId());
      response.setSteps(steps.stream().map(this::buildStepResponse).collect(Collectors.toList()));
    }

    if (includeLogs) {
      Page<TaskLogEntity> logsPage =
          taskLogRepository.findByTaskIdOrderByCreatedAtDesc(task.getId(), PageRequest.of(0, 100));
      response.setLogs(
          logsPage.getContent().stream().map(this::buildLogResponse).collect(Collectors.toList()));
    }

    if (task.getResult() != null) {
      try {
        response.setResult(objectMapper.readValue(task.getResult(), Object.class));
      } catch (JacksonException e) {
        logger.error("Failed to deserialize task result", e);
      }
    }

    if (task.getMetadata() != null) {
      try {
        response.setMetadata(objectMapper.readValue(task.getMetadata(), Object.class));
      } catch (JacksonException e) {
        logger.error("Failed to deserialize task metadata", e);
      }
    }

    return response;
  }

  private TaskStepResponse buildStepResponse(TaskStepEntity step) {
    TaskStepResponse response = new TaskStepResponse();
    response.setId(step.getId());
    response.setStepNumber(step.getStepNumber());
    response.setStepName(step.getStepName());
    response.setStepDescription(step.getStepDescription());
    response.setStatus(step.getStatus());
    response.setStartedAt(step.getStartedAt());
    response.setCompletedAt(step.getCompletedAt());
    response.setErrorMessage(step.getErrorMessage());

    if (step.getStartedAt() != null && step.getCompletedAt() != null) {
      response.setDurationSeconds(
          Duration.between(step.getStartedAt(), step.getCompletedAt()).toMillis() / 1000.0);
    } else if (step.getStartedAt() != null) {
      response.setDurationSeconds(
          Duration.between(step.getStartedAt(), Instant.now()).toMillis() / 1000.0);
    }

    if (step.getOutput() != null) {
      try {
        response.setOutput(objectMapper.readValue(step.getOutput(), Object.class));
      } catch (JacksonException e) {
        logger.error("Failed to deserialize step output", e);
      }
    }

    return response;
  }

  private TaskLogResponse buildLogResponse(TaskLogEntity log) {
    TaskLogResponse response = new TaskLogResponse();
    response.setId(log.getId());
    response.setTimestamp(log.getCreatedAt());
    response.setLevel(log.getLogLevel());
    response.setMessage(log.getMessage());

    if (log.getStepId() != null) {
      TaskStepEntity step = taskStepRepository.findById(log.getStepId()).orElse(null);
      if (step != null) {
        response.setStepNumber(step.getStepNumber());
      }
    }

    if (log.getDetails() != null) {
      try {
        response.setDetails(objectMapper.readValue(log.getDetails(), Object.class));
      } catch (JacksonException e) {
        logger.error("Failed to deserialize log details", e);
      }
    }

    return response;
  }
}
