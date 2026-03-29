package com.onetattva.infron.api.enums;

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
    PROVIDER_SYNC,
    /** Provider storage inventory sync (orchestrator + storage discovery providers). */
    PROVIDER_STORAGE_SYNC
}
