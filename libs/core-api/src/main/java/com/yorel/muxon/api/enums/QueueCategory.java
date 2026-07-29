package com.yorel.muxon.api.enums;

/**
 * Queue category enum
 */
public enum QueueCategory {
    COMMAND,
    STATUS,
    AUDIT,
    /** Task status events published by workers, consumed by the orchestrator TaskEventProcessor. */
    TASK_EVENT,
    /** Entity state events published by workers, consumed by core-services EntityEventProcessor. */
    ENTITY_EVENT
}
