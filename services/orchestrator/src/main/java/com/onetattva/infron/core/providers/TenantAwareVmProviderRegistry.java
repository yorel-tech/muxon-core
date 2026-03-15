package com.onetattva.infron.core.providers;

import com.onetattva.infron.api.model.ProviderType;
import com.onetattva.infron.core.providers.libvirt.LibvirtVmProvider;
import com.onetattva.infron.core.providers.spec.NodeSpec;
import com.onetattva.infron.db.model.NodeEntity;
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
                // Find the associated node for libvirt provider
                List<NodeEntity> nodes = nodeRepository.findByProviderId(providerUuid);
                if (nodes.isEmpty()) {
                    logger.warn("No nodes found for libvirt provider {}", providerUuid);
                    yield Optional.empty();
                }
                NodeEntity node = nodes.get(0); // Libvirt providers have one node
                
                // Build Libvirt connection URI (local qemu:///system or qemu+ssh from credentials)
                String libvirtUri = buildLibvirtUriFromCredentials(node);

                NodeSpec nodeSpec = NodeSpec.builder()
                        .id(node.getId())
                        .name(node.getName())
                        .externalId(node.getExternalId())
                        .endpoint(libvirtUri)
                        .role(node.getRole())
                        .cpuTotal(node.getCpuTotal())
                        .memMb(node.getMemMb())
                        .status(node.getStatus())
                        .lastSeenAt(node.getLastSeenAt())
                        .credentials(node.getCredentials())
                        .ipAddresses(node.getIpAddresses())
                        .capabilities(node.getCapabilities())
                        .resources(node.getResources())
                        .build();
                
                yield Optional.of(new LibvirtVmProvider(nodeSpec));
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

    /**
     * Build Libvirt connection URI from node credentials.
     * <ul>
     *   <li>If credentials contain {@code uri}, that value is used (e.g. {@code qemu:///system} for local).</li>
     *   <li>Otherwise builds SSH URI from {@code host} and {@code user}: {@code qemu+ssh://user@host/system}.</li>
     * </ul>
     *
     * @param node the node entity containing credentials
     * @return Libvirt URI (e.g. qemu:///system or qemu+ssh://user@host/system)
     */
    private String buildLibvirtUriFromCredentials(NodeEntity node) {
        try {
            Map<String, String> credentials = node.getCredentials();
            if (credentials == null) {
                throw new IllegalArgumentException("Node credentials are required");
            }

            String uri = credentials.get("uri");
            if (uri != null && !uri.isBlank()) {
                logger.debug("Using explicit Libvirt URI for node {}: {}", node.getId(), uri);
                return uri.trim();
            }

            String host = credentials.get("host");
            String user = credentials.get("user");
            if (host == null || user == null) {
                throw new IllegalArgumentException(
                    "Node credentials must contain either 'uri' (e.g. qemu:///system) or both 'host' and 'user' for SSH");
            }

            String sshUri = String.format("qemu+ssh://%s@%s/system", user, host);
            logger.debug("Built SSH Libvirt URI for node {}: {}", node.getId(), sshUri);
            return sshUri;
        } catch (Exception e) {
            logger.error("Failed to build Libvirt URI from node credentials for node {}: {}",
                    node.getId(), e.getMessage(), e);
            throw new RuntimeException("Invalid node credentials for Libvirt connection", e);
        }
    }
}
