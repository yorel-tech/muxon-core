package com.onetattva.infron.core.providers.libvirt;

import com.onetattva.infron.core.providers.storage.StorageDiscoveryProviderRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestStorageDiscoveryRegistryConfig {

    @Bean
    public StorageDiscoveryProviderRegistry storageDiscoveryProviderRegistry() {
        return new StorageDiscoveryProviderRegistry();
    }
}
