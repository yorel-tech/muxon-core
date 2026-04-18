package com.krito.muxon.core.info;

import java.util.Set;

/**
 * Contract for components that contribute capabilities to the unified
 * capability set exposed via the /api/v1/info endpoint.
 */
public interface CapabilityProvider {

    /**
     * Return the set of capabilities contributed by this provider.
     *
     * @return a set of capability strings
     */
    Set<String> getCapabilities();
}

