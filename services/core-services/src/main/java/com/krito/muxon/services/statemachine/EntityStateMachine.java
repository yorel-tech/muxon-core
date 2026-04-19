package com.krito.muxon.services.statemachine;

import com.krito.muxon.api.enums.EntityType;
import com.krito.muxon.api.enums.JobStatus;
import com.krito.muxon.db.model.JobEntity;

import java.util.Set;
import java.util.UUID;

public interface EntityStateMachine {

    boolean canTransitionToState(EntityType entityType, UUID entityId, Object targetState);

    boolean isOperationAllowed(EntityType entityType, UUID entityId, String operation);

    Set<String> getBlockedOperations(EntityType entityType, UUID entityId);

    void validateStateForTask(UUID entityId, JobEntity task);

    Object getCurrentState(EntityType entityType, UUID entityId);

    void updateStateForTaskStatus(UUID entityId, JobEntity task, JobStatus newStatus);
}
