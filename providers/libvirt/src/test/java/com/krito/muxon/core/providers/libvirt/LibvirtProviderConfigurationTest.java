package com.krito.muxon.core.providers.libvirt;

import com.krito.muxon.core.providers.storage.StorageDiscoveryProviderRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = {
    TestStorageDiscoveryRegistryConfig.class,
    LibvirtProviderConfiguration.class
})
@ActiveProfiles("test")
public class LibvirtProviderConfigurationTest {

    @Autowired
    private StorageDiscoveryProviderRegistry registry;

    @Autowired
    private LibvirtStorageDiscoveryProvider provider;

    @Test
    public void testRegistryBeanInjection() {
        assertNotNull(registry, "StorageDiscoveryProviderRegistry should be injected");
        assertNotNull(provider, "LibvirtStorageDiscoveryProvider should be injected");
        
        // Verify the provider is registered
        assertNotNull(registry.getProvider("libvirt"), "Libvirt provider should be registered");
    }
}
