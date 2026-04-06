package com.onetattva.infron.core.providers.proxmox;

import com.onetattva.infron.core.providers.storage.StorageDiscoveryProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Proxmox provider module.
 * <p>
 * Registers the Proxmox storage discovery provider with the global registry
 * so that core services can discover storage from Proxmox clusters.
 * </p>
 */
@Configuration
public class ProxmoxProviderConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ProxmoxProviderConfiguration.class);

    @Bean
    public ProxmoxStorageDiscoveryProvider proxmoxStorageDiscoveryProvider(
            StorageDiscoveryProviderRegistry registry) {
        
        ProxmoxStorageDiscoveryProvider provider = new ProxmoxStorageDiscoveryProvider();
        registry.register(provider);
        
        log.info("Registered Proxmox storage discovery provider");
        
        return provider;
    }

    @Bean
    public ProxmoxStorageUploader proxmoxStorageUploader() {
        return new ProxmoxStorageUploader();
    }
}
