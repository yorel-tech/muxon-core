package com.krito.muxon.info;

import java.util.List;

/**
 * Contract for discovering available modules (compute, storage, network, etc.)
 * in the running system.
 */
public interface ModuleProvider {

    /**
     * Return all known modules contributed by this provider.
     *
     * @return list of module descriptors
     */
    List<ModuleDescriptor> getModules();
}

