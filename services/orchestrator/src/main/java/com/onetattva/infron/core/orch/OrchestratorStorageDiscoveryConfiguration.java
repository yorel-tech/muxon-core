package com.onetattva.infron.core.orch;

import com.onetattva.infron.core.providers.libvirt.LibvirtProviderConfiguration;
import com.onetattva.infron.core.providers.proxmox.ProxmoxProviderConfiguration;
import com.onetattva.infron.core.providers.storage.StorageDiscoveryProviderRegistry;
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
