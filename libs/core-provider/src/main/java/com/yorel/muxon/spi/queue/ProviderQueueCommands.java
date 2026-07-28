package com.yorel.muxon.spi.queue;

/**
 * Queue command types for provider-scoped work ({@code EntityType.PROVIDER}).
 * <p>
 * Produced by core-services and consumed by the orchestrator.
 * </p>
 */
public final class ProviderQueueCommands {

    private ProviderQueueCommands() {
    }

    public static final String CONNECTION_TEST = "PROVIDER_CONNECTION_TEST_COMMAND";

    public static final String CAPABILITIES_DISCOVERY = "PROVIDER_CAPABILITIES_DISCOVERY_COMMAND";

    public static final String STORAGE_DISCOVERY = "PROVIDER_STORAGE_DISCOVERY_COMMAND";

    /** Full inventory sync: capabilities (where applicable), node cluster + nodes, storage. */
    public static final String INVENTORY_SYNC = "PROVIDER_INVENTORY_SYNC_COMMAND";

    /** Push content library artifacts to provider storage via provider API (e.g. Proxmox upload). */
    public static final String CONTENT_DATACENTER_REPLICATE = "CONTENT_DATACENTER_REPLICATE_COMMAND";
}
