package com.scal.muxon.services.statemachine;

import com.scal.muxon.api.enums.EntityType;
import com.scal.muxon.api.enums.JobStatus;
import com.scal.muxon.api.enums.VmOperation;
import com.scal.muxon.api.enums.VmStatus;
import com.scal.muxon.db.model.JobEntity;
import com.scal.muxon.db.model.VmEntity;
import com.scal.muxon.db.repository.VmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EntityStateMachineImpl implements EntityStateMachine {

    private static final Logger logger = LoggerFactory.getLogger(EntityStateMachineImpl.class);

    @Autowired
    private VmRepository vmRepository;

    @Override
    public boolean canTransitionToState(EntityType entityType, UUID entityId, Object targetState) {
        if (entityType == EntityType.VM) {
            VmEntity vm = vmRepository.findById(entityId).orElse(null);
            if (vm == null) {
                return false;
            }
            return VmStateMachine.canTransition(vm.getStatus(), (VmStatus) targetState);
        }
        return false;
    }

    @Override
    public boolean isOperationAllowed(EntityType entityType, UUID entityId, String operation) {
        if (entityType == EntityType.VM) {
            VmEntity vm = vmRepository.findById(entityId).orElse(null);
            if (vm == null) {
                return false;
            }
            try {
                VmOperation vmOp = VmOperation.valueOf(operation);
                return VmStateMachine.isOperationAllowed(vm.getStatus(), vmOp);
            } catch (IllegalArgumentException e) {
                logger.warn("Unknown VM operation: {}", operation);
                return false;
            }
        }
        return false;
    }

    @Override
    public Set<String> getBlockedOperations(EntityType entityType, UUID entityId) {
        if (entityType == EntityType.VM) {
            VmEntity vm = vmRepository.findById(entityId).orElse(null);
            if (vm == null) {
                return Set.of();
            }
            return VmStateMachine.getBlockedOperations(vm.getStatus())
                    .stream()
                    .map(Enum::name)
                    .collect(Collectors.toSet());
        }
        return Set.of();
    }

    @Override
    public void validateStateForTask(UUID entityId, JobEntity task) {
        if (task.getTargetEntityType() == EntityType.PROVIDER) {
            return;
        }
        if (task.getTargetEntityType() == EntityType.VM) {
            VmEntity vm = vmRepository.findById(entityId).orElse(null);
            if (vm == null) {
                throw new IllegalStateException("VM not found: " + entityId);
            }
            
            VmStatus targetState = VmStateMachine.getTargetStateForTask(task.getJobType(), JobStatus.RUNNING);
            if (targetState != null && !VmStateMachine.canTransition(vm.getStatus(), targetState)) {
                throw new IllegalStateException(
                    String.format("Cannot transition VM from %s to %s for task %s", 
                        vm.getStatus(), targetState, task.getJobType())
                );
            }
        }
    }

    @Override
    public Object getCurrentState(EntityType entityType, UUID entityId) {
        if (entityType == EntityType.VM) {
            VmEntity vm = vmRepository.findById(entityId).orElse(null);
            return vm != null ? vm.getStatus() : null;
        }
        return null;
    }

    @Override
    public void updateStateForTaskStatus(UUID entityId, JobEntity task, JobStatus newStatus) {
        if (task.getTargetEntityType() == EntityType.PROVIDER) {
            return;
        }
        if (task.getTargetEntityType() == EntityType.VM) {
            VmEntity vm = vmRepository.findById(entityId).orElse(null);
            if (vm == null) {
                logger.warn("VM not found for state update: {}", entityId);
                return;
            }

            VmStatus targetState = VmStateMachine.getTargetStateForTask(task.getJobType(), newStatus);
            if (targetState != null) {
                logger.info("Updating VM {} state from {} to {} based on task {} status {}", 
                    entityId, vm.getStatus(), targetState, task.getId(), newStatus);
                vm.setStatus(targetState);
                vm.setUpdatedAt(Instant.now());
                vmRepository.save(vm);
            }
        }
    }
}
