package com.onetattva.infron.core.providers;

import com.onetattva.infron.api.model.ProviderType;
import com.onetattva.infron.core.providers.libvirt.LibvirtVmProvider;
import com.onetattva.infron.db.model.ProviderEntity;
import com.onetattva.infron.db.repository.NodeRepository;
import com.onetattva.infron.db.repository.ProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for VM providers with tenant datacenter grant resolution and caching.
 * Resolves the provider for a tenant datacenter grant via database lookup
 * and caches the result. Provider instances are created dynamically from database entities.
 */
@Component
public class TenantAwareVmProviderRegistry {

    private static final Logger logger = LoggerFactory.getLogger(TenantAwareVmProviderRegistry.class);
    private static final long CACHE_TTL_MS = 300_000L; // 5 minutes

    private final TenantDatacenterGrantResolver grantResolver;
    private final ProviderRepository providerRepository;
    private final NodeRepository nodeRepository;

    private static final class CachedEntry {
        final Optional<VmProvider> provider;
        final long expireAt;

        CachedEntry(Optional<VmProvider> provider, long expireAt) {
            this.provider = provider;
            this.expireAt = expireAt;
        }
    }

    private final Map<UUID, CachedEntry> cache = new ConcurrentHashMap<>();

    public TenantAwareVmProviderRegistry(TenantDatacenterGrantResolver grantResolver,
                                       ProviderRepository providerRepository,
                                       NodeRepository nodeRepository) {
        this.grantResolver = grantResolver;
        this.providerRepository = providerRepository;
        this.nodeRepository = nodeRepository;
    }

    /**
     * Resolve the VM provider for a tenant datacenter grant.
     * Uses caching to avoid repeated database lookups.
     *
     * @param tenantDatacenterGrantId the tenant datacenter grant ID
     * @return the provider if found
     */
    public Optional<VmProvider> resolveProviderForTenantDatacenter(UUID tenantDatacenterGrantId) {
        Optional<String> providerIdOpt = grantResolver.resolveProviderId(tenantDatacenterGrantId);
        if (providerIdOpt.isEmpty()) {
            logger.warn("No provider found for tenant datacenter grant {}", tenantDatacenterGrantId);
            return Optional.empty();
        }
        
        return getProviderById(providerIdOpt.get());
    }

    /**
     * Get a provider by ID with caching.
     *
     * @param providerId the provider ID (UUID string or logical ID like "mock")
     * @return the provider if found
     */
    public Optional<VmProvider> getProviderById(String providerId) {
        // Try cache first for UUID-based providers
        try {
            UUID providerUuid = UUID.fromString(providerId);
            CachedEntry cached = cache.get(providerUuid);
            
            if (cached != null && cached.expireAt > System.currentTimeMillis()) {
                logger.debug("Cache hit for provider ID {}", providerId);
                return cached.provider;
            }
            
            // Cache miss or expired - resolve from database
            Optional<VmProvider> provider = resolveProviderFromDatabase(providerUuid);
            cache.put(providerUuid, new CachedEntry(provider, System.currentTimeMillis() + CACHE_TTL_MS));
            return provider;
            
        } catch (IllegalArgumentException e) {
            // Not a UUID – fall through to logical IDs (e.g., "mock")
        }

        // Logical provider IDs (e.g. mock provider for tests)
        if ("mock".equalsIgnoreCase(providerId)) {
            return Optional.of(new MockVmProvider());
        }

        logger.warn("Cannot resolve VmProvider for id '{}': not a known UUID or logical provider id", providerId);
        return Optional.empty();
    }

    private Optional<VmProvider> resolveProviderFromDatabase(UUID providerUuid) {
        Optional<ProviderEntity> providerEntityOpt = providerRepository.findById(providerUuid);
        if (providerEntityOpt.isEmpty()) {
            logger.warn("No ProviderEntity found for ID {}", providerUuid);
            return Optional.empty();
        }

        ProviderEntity providerEntity = providerEntityOpt.get();
        ProviderType type = providerEntity.getType();

        return switch (type) {
            case LIBVIRT -> {
                yield Optional.of(new LibvirtVmProvider(providerUuid, nodeRepository));
            }
            case PROXMOX, KUBERNETES -> {
                logger.warn("Dynamic VmProvider creation not implemented for provider type {} (id={})", type, providerUuid);
                yield Optional.empty();
            }
        };
    }

    /**
     * Get all providers from database.
     *
     * @return all providers
     */
    public Map<String, VmProvider> getAllProviders() {
        Map<String, VmProvider> allProviders = new ConcurrentHashMap<>();
        
        // Add mock provider
        allProviders.put("mock", new MockVmProvider());
        
        // Add all database providers
        List<ProviderEntity> providerEntities = providerRepository.findAll();
        for (ProviderEntity providerEntity : providerEntities) {
            Optional<VmProvider> provider = resolveProviderFromDatabase(providerEntity.getId());
            provider.ifPresent(vmProvider -> allProviders.put(vmProvider.id(), vmProvider));
        }
        
        return allProviders;
    }

    /**
     * Clear the provider cache.
     */
    public void clearCache() {
        cache.clear();
        logger.info("Cleared provider cache");
    }
}
