package com.krito.muxon.core.orch;

import com.krito.muxon.core.providers.libvirt.LibvirtProviderConfiguration;
import com.krito.muxon.core.providers.proxmox.ProxmoxProviderConfiguration;
import com.krito.muxon.core.providers.storage.StorageDiscoveryProviderRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Wires {@link StorageDiscoveryProviderRegistry} and registers provider-module discovery
 * implementations for orchestrator-side storage sync.
 */
@Configuration
@Import({ProxmoxProviderConfiguration.class, LibvirtProviderConfiguration.class})
public class OrchestratorStorageDiscoveryConfiguration {

    @Bean
    public StorageDiscoveryProviderRegistry storageDiscoveryProviderRegistry() {
        return new StorageDiscoveryProviderRegistry();
    }
}
