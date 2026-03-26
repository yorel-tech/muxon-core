package com.onetattva.infron.api.dto;

import com.onetattva.infron.api.enums.JobStatus;
import com.onetattva.infron.api.enums.JobType;

import java.util.List;
import java.util.UUID;

public class TaskResponse {
    private UUID taskId;
    private JobStatus status;
    private JobType operation;
    private TaskTargetEntityResponse targetEntity;
    private TaskProgressResponse progress;
    private List<TaskStepResponse> steps;
    private List<TaskLogResponse> logs;
    private TaskTimingResponse timing;
    private String errorMessage;
    private Object result;
    private Object metadata;
    private Boolean cancellationRequested;
    private String cancellationReason;

    public UUID getTaskId() {
        return taskId;
    }

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public JobType getOperation() {
        return operation;
    }

    public void setOperation(JobType operation) {
        this.operation = operation;
    }

    public TaskTargetEntityResponse getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(TaskTargetEntityResponse targetEntity) {
        this.targetEntity = targetEntity;
    }

    public TaskProgressResponse getProgress() {
        return progress;
    }

    public void setProgress(TaskProgressResponse progress) {
        this.progress = progress;
    }

    public List<TaskStepResponse> getSteps() {
        return steps;
    }

    public void setSteps(List<TaskStepResponse> steps) {
        this.steps = steps;
    }

    public List<TaskLogResponse> getLogs() {
        return logs;
    }

    public void setLogs(List<TaskLogResponse> logs) {
        this.logs = logs;
    }

    public TaskTimingResponse getTiming() {
        return timing;
    }

    public void setTiming(TaskTimingResponse timing) {
        this.timing = timing;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }

    public Boolean getCancellationRequested() {
        return cancellationRequested;
    }

    public void setCancellationRequested(Boolean cancellationRequested) {
        this.cancellationRequested = cancellationRequested;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
