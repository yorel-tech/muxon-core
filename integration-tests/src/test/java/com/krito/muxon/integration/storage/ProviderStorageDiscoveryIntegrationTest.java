package com.krito.muxon.integration.storage;

import com.krito.muxon.db.model.ProviderEntity;
import com.krito.muxon.db.model.ProviderStorageEntity;
import com.krito.muxon.db.repository.ProviderRepository;
import com.krito.muxon.db.repository.ProviderStorageRepository;
import com.krito.muxon.services.storage.ProviderStorageDiscoveryService;
import com.krito.muxon.services.storage.CapabilityMappingService;
import com.krito.muxon.api.model.ProviderType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.krito.muxon.CoreServicesApplication;
import org.testcontainers.DockerClientFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for provider storage discovery and normalization.
 */
@SpringBootTest(classes = CoreServicesApplication.class)
@ActiveProfiles("test")
@Transactional
public class ProviderStorageDiscoveryIntegrationTest {

    @BeforeAll
    static void requireDocker() {
        if (!DockerClientFactory.instance().isDockerAvailable()) {
            throw new TestAbortedException("Docker is not available; skipping SpringBootTest storage integration tests");
        }
    }

    @Autowired
    private ProviderStorageDiscoveryService discoveryService;

    @Autowired
    private CapabilityMappingService capabilityMappingService;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private ProviderStorageRepository providerStorageRepository;

    private ProviderEntity testProvider;

    @BeforeEach
    public void setup() {
        // Create test provider
        testProvider = new ProviderEntity();
        testProvider.setName("test-provider");
        testProvider.setType(ProviderType.LIBVIRT);
        testProvider.setEndpoint("ssh://test@localhost/system");
        testProvider.setCredentials(Map.of("sshPrivateKey", "test-key"));
        testProvider.setStatus("ACTIVE");
        providerRepository.save(testProvider);
    }

    @Test
    public void testDiscoveryServiceInitialization() {
        // Then: Service should be initialized with adapters
        assertNotNull(discoveryService);
        assertTrue(discoveryService.isDiscoverySupported("libvirt"));
        assertTrue(discoveryService.isDiscoverySupported("proxmox"));
        assertFalse(discoveryService.isDiscoverySupported("unknown"));
    }

    @Test
    public void testCapabilityNormalization_Libvirt() {
        // When: Normalize Libvirt storage types
        Map<String, Object> rbdCapabilities = capabilityMappingService
            .normalizeStorageType("rbd", "libvirt");
        Map<String, Object> lvmCapabilities = capabilityMappingService
            .normalizeStorageType("lvm", "libvirt");
        Map<String, Object> dirCapabilities = capabilityMappingService
            .normalizeStorageType("dir", "libvirt");

        // Then: Should have correct capabilities
        assertEquals("high", rbdCapabilities.get("performance"));
        assertEquals("replicated", rbdCapabilities.get("redundancy"));
        
        assertNotNull(lvmCapabilities.get("performance"));
        assertEquals("none", lvmCapabilities.get("redundancy"));
        
        assertEquals("low", dirCapabilities.get("performance"));
    }

    @Test
    public void testCapabilityNormalization_Proxmox() {
        // When: Normalize Proxmox storage types
        Map<String, Object> rbdCapabilities = capabilityMappingService
            .normalizeStorageType("rbd", "proxmox");
        Map<String, Object> zfsCapabilities = capabilityMappingService
            .normalizeStorageType("zfspool", "proxmox");
        Map<String, Object> lvmThinCapabilities = capabilityMappingService
            .normalizeStorageType("lvmthin", "proxmox");

        // Then: Should have correct capabilities
        assertEquals("high", rbdCapabilities.get("performance"));
        assertEquals("replicated", rbdCapabilities.get("redundancy"));
        
        assertEquals("medium", zfsCapabilities.get("performance"));
        assertEquals("medium", lvmThinCapabilities.get("performance"));
    }

    @Test
    public void testCapabilityMatching() {
        // Given: Storage type and requirements
        String storageType = "rbd";
        String infronCapability = "performance";
        Object infronValue = "high";

        // When: Check if storage type matches capability
        boolean matches = capabilityMappingService.matchesCapability(
            storageType, infronCapability, infronValue, "libvirt"
        );

        // Then: Should match
        assertTrue(matches);

        // When: Check non-matching
        boolean noMatch = capabilityMappingService.matchesCapability(
            "dir", "performance", "high", "libvirt"
        );

        // Then: Should not match
        assertFalse(noMatch);
    }

    @Test
    public void testProviderStorageRepository() {
        // Given: Create provider storage entries
        ProviderStorageEntity storage1 = createTestStorage("pool1", "rbd");
        ProviderStorageEntity storage2 = createTestStorage("pool2", "lvm");

        // When: Query by provider
        List<ProviderStorageEntity> byProvider = providerStorageRepository
            .findByProviderId(testProvider.getId());

        // Then: Should find both
        assertEquals(2, byProvider.size());

        // When: Query by provider and enabled
        List<ProviderStorageEntity> enabled = providerStorageRepository
            .findByProviderIdAndEnabled(testProvider.getId(), true);

        // Then: Should find both (both are enabled)
        assertEquals(2, enabled.size());

        // When: Query by storage type
        List<ProviderStorageEntity> rbdStorage = providerStorageRepository
            .findByStorageType("rbd");

        // Then: Should find one
        assertEquals(1, rbdStorage.size());
        assertEquals("pool1", rbdStorage.get(0).getName());
    }

    @Test
    public void testProviderStorageSync() {
        // Given: Create initial storage
        createTestStorage("old_pool", "rbd");
        
        List<ProviderStorageEntity> before = providerStorageRepository
            .findByProviderId(testProvider.getId());
        assertEquals(1, before.size());

        // When: Delete and recreate (simulating sync)
        providerStorageRepository.deleteByProviderId(testProvider.getId());
        createTestStorage("new_pool", "rbd");

        // Then: Should have new storage
        List<ProviderStorageEntity> after = providerStorageRepository
            .findByProviderId(testProvider.getId());
        assertEquals(1, after.size());
        assertEquals("new_pool", after.get(0).getName());
    }

    @Test
    public void testCapabilityTranslation() {
        // Given: Infron capabilities
        Map<String, Object> infronCapabilities = Map.of(
            "performance", "high",
            "media", "ssd"
        );

        // When: Translate to Libvirt
        Map<String, Object> libvirtCapabilities = capabilityMappingService
            .translateToProviderCapabilities(infronCapabilities, "libvirt");

        // Then: Should have provider-specific capabilities
        assertNotNull(libvirtCapabilities);
        assertNotNull(libvirtCapabilities.get("pool_type"));

        // When: Translate to Proxmox
        Map<String, Object> proxmoxCapabilities = capabilityMappingService
            .translateToProviderCapabilities(infronCapabilities, "proxmox");

        // Then: Should have provider-specific capabilities
        assertNotNull(proxmoxCapabilities);
        assertNotNull(proxmoxCapabilities.get("storage_type"));
    }

    @Test
    public void testFindByCapabilities() {
        // Given: Create storage with specific capabilities
        ProviderStorageEntity storage = createTestStorage("test_pool", "rbd");
        storage.setCapabilities(Map.of(
            "performance", "high",
            "media", "ssd"
        ));
        providerStorageRepository.save(storage);

        // When: Search by capabilities (using JSONB containment)
        String capabilitiesJson = "{\"performance\": \"high\"}";
        List<ProviderStorageEntity> found = providerStorageRepository
            .findByCapabilitiesContaining(capabilitiesJson);

        // Then: Should find the storage
        assertFalse(found.isEmpty());
        assertTrue(found.stream().anyMatch(s -> s.getName().equals("test_pool")));
    }

    private ProviderStorageEntity createTestStorage(String name, String storageType) {
        ProviderStorageEntity storage = new ProviderStorageEntity();
        storage.setProviderId(testProvider.getId());
        storage.setProviderType("libvirt");
        storage.setExternalId(name);
        storage.setName(name);
        storage.setStorageType(storageType);
        storage.setCapabilities(new HashMap<>());
        storage.setMetrics(Map.of(
            "free_gb", 100L,
            "total_gb", 200L,
            "estimated_iops", 10000
        ));
        storage.setEnabled(true);
        return providerStorageRepository.save(storage);
    }
}
