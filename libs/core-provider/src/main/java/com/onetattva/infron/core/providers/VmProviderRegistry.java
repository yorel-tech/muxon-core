package com.onetattva.infron.core.providers;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for VM providers.
 * Resolves the provider for a tenant datacenter grant via {@link TenantDatacenterGrantResolver}
 * (database-backed) and caches the result. Provider instances are discovered from the CDI container.
 */
@ApplicationScoped
public class VmProviderRegistry {

    private static final Logger logger = LoggerFactory.getLogger(VmProviderRegistry.class);
    private static final long CACHE_TTL_MS = 300_000L; // 5 minutes

    private final Map<String, VmProvider> providersById = new ConcurrentHashMap<>();

    @Inject
    TenantDatacenterGrantResolver grantResolver;

    @Inject
    List<VmProvider> providerBeans;

    private static final class CachedEntry {
        final Optional<VmProvider> provider;
        final long expireAt;

        CachedEntry(Optional<VmProvider> provider, long expireAt) {
            this.provider = provider;
            this.expireAt = expireAt;
        }
    }

    private final Map<UUID, CachedEntry> cache = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        if (providerBeans != null) {
            for (VmProvider p : providerBeans) {
                String id = p.id();
                if (id != null) {
                    providersById.put(id, p);
                    logger.info("Registered VM provider: {} - {}", id, p.description());
                }
            }
        }
    }

    /**
     * Get provider for a specific tenant datacenter grant.
     * Uses the resolver to load grant/datacenter from the database and caches the result.
     */
    public Optional<VmProvider> getProviderForTenantDatacenter(UUID tenantDatacenterGrantId) {
        if (providersById.isEmpty()) {
            return Optional.empty();
        }

        long now = System.currentTimeMillis();
        CachedEntry entry = cache.get(tenantDatacenterGrantId);
        if (entry != null && entry.expireAt > now) {
            return entry.provider;
        }

        Optional<String> providerIdOpt = grantResolver.resolveProviderId(tenantDatacenterGrantId);
        Optional<VmProvider> result = providerIdOpt.flatMap(this::getProvider);
        cache.put(tenantDatacenterGrantId, new CachedEntry(result, now + CACHE_TTL_MS));

        if (result.isEmpty() && providerIdOpt.isEmpty()) {
            logger.warn("No tenant datacenter grant or provider linked for grant ID: {}", tenantDatacenterGrantId);
        } else if (result.isEmpty()) {
            logger.warn("Provider ID resolved but no VM provider registered for ID: {}", providerIdOpt.get());
        }

        return result;
    }

    private Optional<VmProvider> getProvider(String providerId) {
        return Optional.ofNullable(providersById.get(providerId));
    }
}
