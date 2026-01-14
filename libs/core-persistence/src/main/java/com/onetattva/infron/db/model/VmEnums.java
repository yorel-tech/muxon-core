package com.onetattva.infron.db.model;

import jakarta.persistence.*;

/**
 * Enums for VM-related entities.
 */
public class VmEnums {

    /**
     * VM status enum
     */
    public enum VmStatus {
        PENDING,
        PLANNED,
        PROVISIONING,
        ACTIVE,
        STOPPED,
        SUSPENDED,
        ERROR,
        DELETING,
        DELETED,
        MIGRATING,
        RESIZING
    }

    /**
     * VM power state enum
     */
    public enum VmPowerState {
        UNKNOWN,
        ON,
        OFF,
        SUSPENDED
    }

    /**
     * Queue category enum
     */
    public enum QueueCategory {
        COMMAND,
        STATUS,
        AUDIT
    }

    /**
     * Queue status enum
     */
    public enum QueueStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    /**
     * Entity type enum
     */
    public enum EntityType {
        VM,
        NODE,
        DATACENTER,
        TENANT,
        PROVIDER,
        USER
    }

    /**
     * Job type enum
     */
    public enum JobType {
        VM_CREATE,
        VM_DELETE,
        VM_START,
        VM_STOP,
        VM_RESTART,
        VM_SUSPEND,
        VM_RESUME,
        VM_SNAPSHOT,
        VM_BACKUP,
        VM_MIGRATE,
        VM_RESIZE,
        NODE_PROVISION,
        NODE_DECOMMISSION,
        PROVIDER_SYNC
    }

    /**
     * Job status enum
     */
    public enum JobStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * Event type enum
     */
    public enum EventType {
        CREATED,
        UPDATED,
        DELETED,
        STARTED,
        STOPPED,
        SUSPENDED,
        RESUMED,
        RESTARTED,
        STATUS_CHANGED,
        OPERATION_COMPLETED,
        OPERATION_FAILED
    }

    /**
     * VM operation type enum
     */
    public enum VmOperationType {
        CREATE,
        DELETE,
        START,
        STOP,
        RESTART,
        SUSPEND,
        RESUME,
        SNAPSHOT,
        BACKUP,
        MIGRATE,
        RESIZE
    }
}
