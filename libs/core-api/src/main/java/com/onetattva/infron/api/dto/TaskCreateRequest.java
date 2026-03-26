package com.onetattva.infron.api.dto;

import com.onetattva.infron.api.enums.EntityType;
import com.onetattva.infron.api.enums.JobType;

import java.util.Map;
import java.util.UUID;

public class TaskCreateRequest {
    private JobType operation;
    private EntityType entityType;
    private UUID entityId;
    private Map<String, Object> parameters;
    private Integer timeoutSeconds;
    private Map<String, Object> metadata;

    public JobType getOperation() {
        return operation;
    }

    public void setOperation(JobType operation) {
        this.operation = operation;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
