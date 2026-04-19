package com.krito.muxon.spi.queue;

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

    /** Absolute artifact root on the core-services host (resolved before enqueue). */
    public static final String ARTIFACT_ROOT = "artifactRoot";

    /**
     * Comma-separated Proxmox storage names ({@code ProviderStorageEntity.externalId}) to replicate into.
     */
    public static final String STORAGE_POOL_IDS = "storagePoolIds";

    public static final String LIBRARY_ID = "libraryId";

    public static final String DATACENTER_ID = "datacenterId";

    /** {@link java.util.UUID} of {@code content_library_distribution} row (core-services owns this table). */
    public static final String DISTRIBUTION_ID = "distributionId";
}
