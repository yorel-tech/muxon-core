package com.onetattva.infron.core.spi.queue;

/**
 * Metadata keys on {@link CommandMessage} for provider-scoped queue commands.
 */
public final class ProviderQueueMetadataKeys {

    private ProviderQueueMetadataKeys() {
    }

    /** {@link java.util.UUID} of {@code job} row; orchestrator updates job status when work finishes. */
    public static final String JOB_ID = "jobId";

    /** Max seconds for provider-side discovery work (orchestrator enforces). */
    public static final String EXECUTION_TIMEOUT_SECONDS = "executionTimeoutSeconds";
}
