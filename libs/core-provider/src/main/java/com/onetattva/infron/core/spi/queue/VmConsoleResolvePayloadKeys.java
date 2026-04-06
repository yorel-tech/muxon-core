package com.onetattva.infron.core.spi.queue;

/**
 * JSON payload keys for {@link VmQueueCommands#CONSOLE_RESOLVE} queue rows.
 */
public final class VmConsoleResolvePayloadKeys {

    private VmConsoleResolvePayloadKeys() {
    }

    public static final String TENANT_DATACENTER_GRANT_ID = "tenantDatacenterGrantId";
    public static final String EXTERNAL_ID = "externalId";
    public static final String NODE_ID = "nodeId";

    /** Present with value {@code true} when orchestrator completed successfully. */
    public static final String RESOLVED = "resolved";
    public static final String CONSOLE_TYPE = "consoleType";
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String TLS = "tls";
    public static final String PASSWORD = "password";
}
