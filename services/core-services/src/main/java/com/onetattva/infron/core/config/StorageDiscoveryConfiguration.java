package com.onetattva.infron.core.config;

import com.onetattva.infron.core.providers.storage.StorageDiscoveryProviderRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for storage discovery infrastructure.
 * <p>
 * Creates the global registry that provider modules use to register
 * their storage discovery implementations.
 * </p>
 */
@Configuration
public class StorageDiscoveryConfiguration {

    @Bean
    public StorageDiscoveryProviderRegistry storageDiscoveryProviderRegistry() {
        return new StorageDiscoveryProviderRegistry();
    }
}
