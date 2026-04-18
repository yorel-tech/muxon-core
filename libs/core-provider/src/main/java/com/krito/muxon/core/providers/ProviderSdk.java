package com.krito.muxon.core.providers;

import com.krito.muxon.api.model.ProviderType;

import java.util.Map;

/**
 * Generic provider-SDK interface for provider-level APIs (connection testing, capabilities, etc.).
 * <p>
 * Implementations live in provider modules (proxmox/libvirt/etc.) so core services do not depend
 * on provider SDK libraries.
 */
public interface ProviderSdk {

    /**
     * The provider type this SDK supports.
     */
    ProviderType providerType();

    /**
     * Test connectivity/auth against the provider referenced by {@link ProviderConnectionInfo}.
     *
     * @param connectionInfo endpoint/credentials/connectionConfig for the provider
     * @return test outcome including optional capabilities
     */
    ProviderSdkConnectionTestResult testConnection(ProviderConnectionInfo connectionInfo);

    /**
     * Fetch provider capabilities (or an approximation).
     *
     * @param connectionInfo endpoint/credentials/connectionConfig for the provider
     * @return capabilities as a key/value map persisted by core-services
     */
    Map<String, String> getCapabilities(ProviderConnectionInfo connectionInfo);
}

