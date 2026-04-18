package com.krito.muxon.core.services.statemachine;

import com.krito.muxon.api.enums.JobStatus;
import com.krito.muxon.api.enums.JobType;
import com.krito.muxon.api.enums.VmOperation;
import com.krito.muxon.api.enums.VmStatus;

import java.util.*;

public class VmStateMachine {

    private static final Map<VmStatus, Set<VmStatus>> VALID_TRANSITIONS = new HashMap<>();
    private static final Map<VmStatus, Set<VmOperation>> BLOCKED_OPERATIONS = new HashMap<>();

    static {
        VALID_TRANSITIONS.put(VmStatus.PENDING, Set.of(VmStatus.PLANNED, VmStatus.ERROR, VmStatus.DELETED));
        VALID_TRANSITIONS.put(VmStatus.PLANNED, Set.of(VmStatus.PROVISIONING, VmStatus.ERROR, VmStatus.DELETED));
        VALID_TRANSITIONS.put(VmStatus.PROVISIONING, Set.of(VmStatus.ACTIVE, VmStatus.ERROR, VmStatus.DELETING));
        VALID_TRANSITIONS.put(VmStatus.ACTIVE, Set.of(VmStatus.STOPPED, VmStatus.SUSPENDED, VmStatus.DELETING, VmStatus.MIGRATING, VmStatus.RESIZING, VmStatus.ERROR));
        VALID_TRANSITIONS.put(VmStatus.STOPPED, Set.of(VmStatus.ACTIVE, VmStatus.DELETING, VmStatus.ERROR));
        VALID_TRANSITIONS.put(VmStatus.SUSPENDED, Set.of(VmStatus.ACTIVE, VmStatus.DELETING, VmStatus.ERROR));
        VALID_TRANSITIONS.put(VmStatus.ERROR, Set.of(VmStatus.DELETING, VmStatus.ACTIVE, VmStatus.STOPPED));
        VALID_TRANSITIONS.put(VmStatus.DELETING, Set.of(VmStatus.DELETED, VmStatus.ERROR));
        VALID_TRANSITIONS.put(VmStatus.DELETED, Set.of());
        VALID_TRANSITIONS.put(VmStatus.MIGRATING, Set.of(VmStatus.ACTIVE, VmStatus.ERROR));
        VALID_TRANSITIONS.put(VmStatus.RESIZING, Set.of(VmStatus.ACTIVE, VmStatus.ERROR));

        BLOCKED_OPERATIONS.put(VmStatus.PENDING, Set.of(VmOperation.START, VmOperation.STOP, VmOperation.RESTART, VmOperation.SUSPEND, VmOperation.RESUME, VmOperation.MODIFY, VmOperation.MIGRATE, VmOperation.RESIZE));
        BLOCKED_OPERATIONS.put(VmStatus.PLANNED, Set.of(VmOperation.START, VmOperation.STOP, VmOperation.RESTART, VmOperation.SUSPEND, VmOperation.RESUME, VmOperation.MODIFY, VmOperation.MIGRATE, VmOperation.RESIZE));
        BLOCKED_OPERATIONS.put(VmStatus.PROVISIONING, Set.of(VmOperation.CREATE, VmOperation.START, VmOperation.STOP, VmOperation.RESTART, VmOperation.SUSPEND, VmOperation.RESUME, VmOperation.MODIFY, VmOperation.MIGRATE, VmOperation.RESIZE));
        BLOCKED_OPERATIONS.put(VmStatus.ACTIVE, Set.of(VmOperation.CREATE, VmOperation.START, VmOperation.RESUME));
        BLOCKED_OPERATIONS.put(VmStatus.STOPPED, Set.of(VmOperation.CREATE, VmOperation.STOP, VmOperation.SUSPEND, VmOperation.RESUME, VmOperation.RESTART));
        BLOCKED_OPERATIONS.put(VmStatus.SUSPENDED, Set.of(VmOperation.CREATE, VmOperation.START, VmOperation.STOP, VmOperation.SUSPEND, VmOperation.RESTART));
        BLOCKED_OPERATIONS.put(VmStatus.ERROR, Set.of(VmOperation.CREATE, VmOperation.START, VmOperation.STOP, VmOperation.RESTART, VmOperation.SUSPEND, VmOperation.RESUME, VmOperation.MODIFY, VmOperation.MIGRATE, VmOperation.RESIZE));
        BLOCKED_OPERATIONS.put(VmStatus.DELETING, Set.of(VmOperation.CREATE, VmOperation.DELETE, VmOperation.START, VmOperation.STOP, VmOperation.RESTART, VmOperation.SUSPEND, VmOperation.RESUME, VmOperation.MODIFY, VmOperation.MIGRATE, VmOperation.RESIZE, VmOperation.SNAPSHOT, VmOperation.BACKUP));
        BLOCKED_OPERATIONS.put(VmStatus.DELETED, Set.of(VmOperation.CREATE, VmOperation.DELETE, VmOperation.START, VmOperation.STOP, VmOperation.RESTART, VmOperation.SUSPEND, VmOperation.RESUME, VmOperation.MODIFY, VmOperation.MIGRATE, VmOperation.RESIZE, VmOperation.SNAPSHOT, VmOperation.BACKUP));
        BLOCKED_OPERATIONS.put(VmStatus.MIGRATING, Set.of(VmOperation.CREATE, VmOperation.DELETE, VmOperation.START, VmOperation.STOP, VmOperation.RESTART, VmOperation.SUSPEND, VmOperation.RESUME, VmOperation.MODIFY, VmOperation.MIGRATE, VmOperation.RESIZE));
        BLOCKED_OPERATIONS.put(VmStatus.RESIZING, Set.of(VmOperation.CREATE, VmOperation.DELETE, VmOperation.START, VmOperation.STOP, VmOperation.RESTART, VmOperation.SUSPEND, VmOperation.RESUME, VmOperation.MODIFY, VmOperation.MIGRATE, VmOperation.RESIZE));
    }

    public static boolean canTransition(VmStatus from, VmStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<VmStatus> allowedTransitions = VALID_TRANSITIONS.get(from);
        return allowedTransitions != null && allowedTransitions.contains(to);
    }

    public static boolean isOperationAllowed(VmStatus currentStatus, VmOperation operation) {
        if (currentStatus == null || operation == null) {
            return false;
        }
        Set<VmOperation> blockedOps = BLOCKED_OPERATIONS.get(currentStatus);
        return blockedOps == null || !blockedOps.contains(operation);
    }

    public static Set<VmOperation> getBlockedOperations(VmStatus currentStatus) {
        if (currentStatus == null) {
            return Set.of();
        }
        return BLOCKED_OPERATIONS.getOrDefault(currentStatus, Set.of());
    }

    public static VmStatus getTargetStateForTask(JobType jobType, JobStatus taskStatus) {
        if (taskStatus == JobStatus.RUNNING) {
            return switch (jobType) {
                case VM_CREATE -> VmStatus.PROVISIONING;
                case VM_DELETE -> VmStatus.DELETING;
                case VM_MIGRATE -> VmStatus.MIGRATING;
                case VM_RESIZE -> VmStatus.RESIZING;
                default -> null;
            };
        } else if (taskStatus == JobStatus.COMPLETED) {
            return switch (jobType) {
                case VM_CREATE -> VmStatus.ACTIVE;
                case VM_DELETE -> VmStatus.DELETED;
                case VM_START -> VmStatus.ACTIVE;
                case VM_STOP -> VmStatus.STOPPED;
                case VM_SUSPEND -> VmStatus.SUSPENDED;
                case VM_RESUME -> VmStatus.ACTIVE;
                case VM_MIGRATE -> VmStatus.ACTIVE;
                case VM_RESIZE -> VmStatus.ACTIVE;
                default -> null;
            };
        } else if (taskStatus == JobStatus.FAILED || taskStatus == JobStatus.CANCELLED) {
            return VmStatus.ERROR;
        }
        return null;
    }
}
