package com.sal.muxon.providers.libvirt;

import com.sal.muxon.providers.storage.StorageDiscoveryProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Libvirt provider module.
 * <p>
 * Registers the Libvirt storage discovery provider with the global registry
 * so that core services can discover storage from Libvirt hypervisors.
 * </p>
 */
@Configuration
public class LibvirtProviderConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LibvirtProviderConfiguration.class);

    @Bean
    public LibvirtStorageDiscoveryProvider libvirtStorageDiscoveryProvider(
            StorageDiscoveryProviderRegistry registry) {
        
        LibvirtStorageDiscoveryProvider provider = new LibvirtStorageDiscoveryProvider();
        registry.register(provider);
        
        log.info("Registered Libvirt storage discovery provider");
        
        return provider;
    }
}
