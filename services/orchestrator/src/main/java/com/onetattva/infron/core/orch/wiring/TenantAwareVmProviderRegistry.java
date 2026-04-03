package com.onetattva.infron.core.orch.wiring;

import com.onetattva.infron.api.model.ProviderType;
import com.onetattva.infron.core.providers.MockVmProvider;
import com.onetattva.infron.core.providers.TenantDatacenterGrantResolver;
import com.onetattva.infron.core.providers.VmProvider;
import com.onetattva.infron.core.providers.libvirt.LibvirtVmProvider;
import com.onetattva.infron.core.providers.proxmox.ProxmoxNodeInventoryProvider;
import com.onetattva.infron.core.providers.proxmox.ProxmoxVmProvider;
import com.onetattva.infron.db.model.NodeEntity;
import com.onetattva.infron.db.model.ProviderEntity;
import com.onetattva.infron.db.repository.NodeRepository;
import com.onetattva.infron.db.repository.ProviderRepository;
import com.onetattva.infron.db.repository.VmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for VM providers with tenant datacenter grant resolution and caching.
 */
@Component
public class TenantAwareVmProviderRegistry {

    private static final Logger logger = LoggerFactory.getLogger(TenantAwareVmProviderRegistry.class);
    private static final long CACHE_TTL_MS = 300_000L;

    private final TenantDatacenterGrantResolver grantResolver;
    private final ProviderRepository providerRepository;
    private final NodeRepository nodeRepository;
    private final VmRepository vmRepository;

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
                                         NodeRepository nodeRepository,
                                         VmRepository vmRepository) {
        this.grantResolver = grantResolver;
        this.providerRepository = providerRepository;
        this.nodeRepository = nodeRepository;
        this.vmRepository = vmRepository;
    }

    public Optional<com.onetattva.infron.core.providers.ProviderContext> createContextForTenantDatacenter(UUID tenantDatacenterGrantId) {
        Optional<String> providerIdOpt = grantResolver.resolveProviderId(tenantDatacenterGrantId);
        if (providerIdOpt.isEmpty()) {
            return Optional.empty();
        }
        return createContextForProvider(UUID.fromString(providerIdOpt.get()), tenantDatacenterGrantId);
    }

    private Optional<com.onetattva.infron.core.providers.ProviderContext> createContextForProvider(UUID providerId, UUID tenantDatacenterGrantId) {
        Optional<ProviderEntity> providerEntityOpt = providerRepository.findById(providerId);
        if (providerEntityOpt.isEmpty()) {
            return Optional.empty();
        }
        ProviderEntity providerEntity = providerEntityOpt.get();
        ProviderType type = providerEntity.getType();
        return switch (type) {
            case LIBVIRT -> createLibvirtContext(providerEntity, tenantDatacenterGrantId);
            case PROXMOX -> createProxmoxContext(providerEntity, tenantDatacenterGrantId);
            default -> Optional.empty();
        };
    }

    private Optional<com.onetattva.infron.core.providers.ProviderContext> createLibvirtContext(ProviderEntity providerEntity, UUID tenantDatacenterGrantId) {
        UUID targetNodeId = selectTargetNodeForLibvirt(tenantDatacenterGrantId);
        if (targetNodeId == null) {
            return Optional.empty();
        }
        return Optional.of(new com.onetattva.infron.core.providers.libvirt.LibvirtProviderContext(providerEntity, nodeRepository, targetNodeId));
    }

    private Optional<com.onetattva.infron.core.providers.ProviderContext> createProxmoxContext(ProviderEntity providerEntity, UUID tenantDatacenterGrantId) {
        ProxmoxNodeInfo nodeInfo = selectTargetNodeForProxmox(providerEntity, tenantDatacenterGrantId);
        if (nodeInfo == null) {
            logger.warn("No Proxmox node available for provider {} and tenant grant {}",
                    providerEntity.getId(), tenantDatacenterGrantId);
            return Optional.empty();
        }
        String preferredStorage = getPreferredStorage(tenantDatacenterGrantId);
        return Optional.of(new com.onetattva.infron.core.providers.proxmox.ProxmoxProviderContext(
                providerEntity,
                nodeInfo.nodeName(),
                nodeInfo.nodeId(),
                preferredStorage));
    }

    private UUID selectTargetNodeForLibvirt(UUID tenantDatacenterGrantId) {
        List<NodeEntity> nodes = nodeRepository.findAll();
        return nodes.isEmpty() ? null : nodes.get(0).getId();
    }

    private ProxmoxNodeInfo selectTargetNodeForProxmox(ProviderEntity providerEntity, UUID tenantDatacenterGrantId) {
        List<NodeEntity> nodes = nodeRepository.findByProviderId(providerEntity.getId());
        if (!nodes.isEmpty()) {
            NodeEntity selected = nodes.stream()
                    .filter(NodeEntity::isActive)
                    .findFirst()
                    .orElse(nodes.get(0));
            String nodeName = selected.getName();
            String nodeExternalId = selected.getExternalId();
            String proxmoxNodeId = nodeExternalId != null && !nodeExternalId.isBlank()
                    ? nodeExternalId
                    : nodeName;
            if (proxmoxNodeId == null || proxmoxNodeId.isBlank()) {
                logger.warn("Selected Proxmox node row {} has no usable name/externalId for tenant grant {}",
                        selected.getId(), tenantDatacenterGrantId);
                return null;
            }
            if (nodeName == null || nodeName.isBlank()) {
                nodeName = proxmoxNodeId;
            }
            return new ProxmoxNodeInfo(nodeName, proxmoxNodeId);
        }
        try {
            Map<String, Object> connectionInfo = new HashMap<>();
            connectionInfo.put("endpoint", providerEntity.getEndpoint());
            connectionInfo.put("credentials", providerEntity.getCredentials());
            ProxmoxNodeInventoryProvider inventoryProvider = new ProxmoxNodeInventoryProvider();
            List<ProxmoxNodeInventoryProvider.DiscoveredNode> discovered = inventoryProvider.discoverNodes(connectionInfo);
            if (discovered.isEmpty()) {
                logger.warn("No Proxmox nodes discovered live for provider {} and tenant grant {}",
                        providerEntity.getId(), tenantDatacenterGrantId);
                return null;
            }
            ProxmoxNodeInventoryProvider.DiscoveredNode selected = discovered.get(0);
            String proxmoxNode = selected.name();
            if (proxmoxNode == null || proxmoxNode.isBlank()) {
                logger.warn("Discovered Proxmox node has blank name for provider {} and tenant grant {}",
                        providerEntity.getId(), tenantDatacenterGrantId);
                return null;
            }
            return new ProxmoxNodeInfo(proxmoxNode, proxmoxNode);
        } catch (Exception e) {
            logger.warn("Failed to discover Proxmox nodes live for provider {} and tenant grant {}: {}",
                    providerEntity.getId(), tenantDatacenterGrantId, e.getMessage());
            return null;
        }
    }

    private String getPreferredStorage(UUID tenantDatacenterGrantId) {
        return "local-lvm";
    }

    private record ProxmoxNodeInfo(String nodeName, String nodeId) {
    }

    public Optional<VmProvider> resolveProviderForTenantDatacenter(UUID tenantDatacenterGrantId) {
        Optional<String> providerIdOpt = grantResolver.resolveProviderId(tenantDatacenterGrantId);
        if (providerIdOpt.isEmpty()) {
            logger.warn("No provider found for tenant datacenter grant {}", tenantDatacenterGrantId);
            return Optional.empty();
        }
        return getProviderById(providerIdOpt.get());
    }

    public Optional<VmProvider> getProviderById(String providerId) {
        try {
            UUID providerUuid = UUID.fromString(providerId);
            CachedEntry cached = cache.get(providerUuid);
            if (cached != null && cached.expireAt > System.currentTimeMillis()) {
                logger.debug("Cache hit for provider ID {}", providerId);
                return cached.provider;
            }
            Optional<VmProvider> provider = resolveProviderFromDatabase(providerUuid);
            cache.put(providerUuid, new CachedEntry(provider, System.currentTimeMillis() + CACHE_TTL_MS));
            return provider;
        } catch (IllegalArgumentException e) {
            // not a UUID
        }
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
            case LIBVIRT -> Optional.of(new LibvirtVmProvider(providerUuid, nodeRepository));
            case PROXMOX -> Optional.of(new ProxmoxVmProvider(providerUuid, providerRepository, vmRepository, nodeRepository));
            default -> Optional.empty();
        };
    }

    public Map<String, VmProvider> getAllProviders() {
        Map<String, VmProvider> allProviders = new ConcurrentHashMap<>();
        allProviders.put("mock", new MockVmProvider());
        List<ProviderEntity> providerEntities = providerRepository.findAll();
        for (ProviderEntity providerEntity : providerEntities) {
            Optional<VmProvider> provider = resolveProviderFromDatabase(providerEntity.getId());
            provider.ifPresent(vmProvider -> allProviders.put(vmProvider.id(), vmProvider));
        }
        return allProviders;
    }

    public void clearCache() {
        cache.clear();
        logger.info("Cleared provider cache");
    }
}
