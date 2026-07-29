package com.yorel.muxon.customization.model;

/**
 * Lifecycle phases for guest customization.
 * Stored as a string in {@code vms.customization_status} JSONB.
 */
public enum CustomizationPhase {
    /** No customization configured for this VM. */
    NONE,
    /** Customization spec persisted; seed ISO not yet built. */
    PENDING,
    /** Seed ISO written to disk; VM not yet started. */
    SEED_BUILT,
    /** VM is running; waiting for QEMU guest agent to come online. */
    WAITING_AGENT,
    /** QEMU guest agent is reachable; cloud-init or sysprep is actively running. */
    IN_PROGRESS,
    /** All stages done; IP/hostname confirmed and stored; seed ISO cleaned up. */
    COMPLETE,
    /** Timeout exceeded or guest-reported error. */
    FAILED
}
