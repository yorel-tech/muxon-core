package com.krito.muxon.core.spi.queue;

/**
 * Queue command types for VM-scoped work ({@code EntityType.VM}).
 */
public final class VmQueueCommands {

    private VmQueueCommands() {
    }

    /**
     * Resolve provider console connection details for a running VM.
     * Produced by core-services; orchestrator fills {@code payload} result fields and marks the row completed.
     */
    public static final String CONSOLE_RESOLVE = "VM_CONSOLE_RESOLVE_COMMAND";
}
