package com.sal.muxon.orch;

import com.sal.muxon.providers.libvirt.LibvirtProviderConfiguration;
import com.sal.muxon.providers.proxmox.ProxmoxProviderConfiguration;
import com.sal.muxon.providers.storage.StorageDiscoveryProviderRegistry;
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
