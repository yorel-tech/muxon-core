package com.sal.muxon.worker.wiring;

import com.sal.muxon.api.model.ProviderType;
import com.sal.muxon.providers.MockVmProvider;
import com.sal.muxon.providers.VmProvider;
import com.sal.muxon.providers.libvirt.LibvirtVmProvider;
import com.sal.muxon.providers.proxmox.ProxmoxNodeInventoryProvider;
import com.sal.muxon.providers.proxmox.ProxmoxVmProvider;
import com.sal.muxon.db.model.NodeEntity;
import com.sal.muxon.db.model.ProviderEntity;
import com.sal.muxon.db.model.ProviderStorageEntity;
import com.sal.muxon.db.repository.NodeRepository;
import com.sal.muxon.db.repository.ProviderRepository;
import com.sal.muxon.db.repository.ProviderStorageRepository;
import com.sal.muxon.db.repository.VmRepository;
import com.sal.muxon.db.resolver.TransactionalGrantProviderResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for VM providers with tenant datacenter grant resolution and caching.
 *
 * <p>This is used by worker executors to resolve a provider and provider context
 * for a VM's tenant-datacenter grant.
 */
@Component
public class TenantAwareVmProviderRegistry {

    private static final Logger logger = LoggerFactory.getLogger(TenantAwareVmProviderRegistry.class);
    private static final long CACHE_TTL_MS = 300_000L;

    private final TransactionalGrantProviderResolver grantProviderResolver;
    private final ProviderRepository providerRepository;
    private final ProviderStorageRepository providerStorageRepository;
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

    public TenantAwareVmProviderRegistry(
            TransactionalGrantProviderResolver grantProviderResolver,
            ProviderRepository providerRepository,
            ProviderStorageRepository providerStorageRepository,
            NodeRepository nodeRepository,
            VmRepository vmRepository) {
        this.grantProviderResolver = grantProviderResolver;
        this.providerRepository = providerRepository;
        this.providerStorageRepository = providerStorageRepository;
        this.nodeRepository = nodeRepository;
        this.vmRepository = vmRepository;
    }

    public Optional<com.sal.muxon.providers.ProviderContext> createContextForTenantDatacenter(
            UUID tenantDatacenterGrantId) {
        Optional<String> providerIdOpt = grantProviderResolver.resolveProviderId(tenantDatacenterGrantId);
        if (providerIdOpt.isEmpty()) {
            logger.debug("createContext: no provider id for tenantDatacenterGrantId={}", tenantDatacenterGrantId);
            return Optional.empty();
        }
        Optional<com.sal.muxon.providers.ProviderContext> ctx =
                createContextForProvider(UUID.fromString(providerIdOpt.get()), tenantDatacenterGrantId);
        if (ctx.isEmpty()) {
            logger.debug(
                    "createContext: failed for tenantDatacenterGrantId={}, providerId={}",
                    tenantDatacenterGrantId,
                    providerIdOpt.get());
        } else if (logger.isDebugEnabled()) {
            logger.debug(
                    "createContext: grantId={}, providerId={}, type={}, metadata={}",
                    tenantDatacenterGrantId,
                    providerIdOpt.get(),
                    ctx.get().getProviderType(),
                    ctx.get().getMetadata());
        }
        return ctx;
    }

    private Optional<com.sal.muxon.providers.ProviderContext> createContextForProvider(
            UUID providerId, UUID tenantDatacenterGrantId) {
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

    private Optional<com.sal.muxon.providers.ProviderContext> createLibvirtContext(
            ProviderEntity providerEntity, UUID tenantDatacenterGrantId) {
        UUID targetNodeId = selectTargetNodeForLibvirt(tenantDatacenterGrantId);
        if (targetNodeId == null) {
            return Optional.empty();
        }
        return Optional.of(
                new com.sal.muxon.providers.libvirt.LibvirtProviderContext(
                        providerEntity, nodeRepository, targetNodeId));
    }

    private Optional<com.sal.muxon.providers.ProviderContext> createProxmoxContext(
            ProviderEntity providerEntity, UUID tenantDatacenterGrantId) {
        ProxmoxNodeInfo nodeInfo = selectTargetNodeForProxmox(providerEntity, tenantDatacenterGrantId);
        if (nodeInfo == null) {
            logger.warn(
                    "No Proxmox node available for provider {} and tenant grant {}",
                    providerEntity.getId(),
                    tenantDatacenterGrantId);
            return Optional.empty();
        }
        String preferredStorage = getPreferredStorage(tenantDatacenterGrantId);
        String importSource =
                resolveProxmoxImportSourceStorage(providerEntity.getId(), preferredStorage).orElse(null);
        if (importSource != null) {
            logger.debug(
                    "Proxmox VM disk pool {} vs content import pool {} for provider {}",
                    preferredStorage,
                    importSource,
                    providerEntity.getId());
        }
        return Optional.of(
                new com.sal.muxon.providers.proxmox.ProxmoxProviderContext(
                        providerEntity,
                        nodeInfo.nodeName(),
                        nodeInfo.nodeId(),
                        preferredStorage,
                        importSource));
    }

    private UUID selectTargetNodeForLibvirt(UUID tenantDatacenterGrantId) {
        List<NodeEntity> nodes = nodeRepository.findAll();
        return nodes.isEmpty() ? null : nodes.getFirst().getId();
    }

    private ProxmoxNodeInfo selectTargetNodeForProxmox(ProviderEntity providerEntity, UUID tenantDatacenterGrantId) {
        List<NodeEntity> nodes = nodeRepository.findByProviderId(providerEntity.getId());
        if (!nodes.isEmpty()) {
            NodeEntity selected =
                    nodes.stream().filter(NodeEntity::isActive).findFirst().orElse(nodes.getFirst());
            String nodeName = selected.getName();
            String nodeExternalId = selected.getExternalId();
            String proxmoxNodeId =
                    nodeExternalId != null && !nodeExternalId.isBlank() ? nodeExternalId : nodeName;
            if (proxmoxNodeId == null || proxmoxNodeId.isBlank()) {
                logger.warn(
                        "Selected Proxmox node row {} has no usable name/externalId for tenant grant {}",
                        selected.getId(),
                        tenantDatacenterGrantId);
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
                logger.warn(
                        "No Proxmox nodes discovered live for provider {} and tenant grant {}",
                        providerEntity.getId(),
                        tenantDatacenterGrantId);
                return null;
            }
            ProxmoxNodeInventoryProvider.DiscoveredNode selected = discovered.getFirst();
            String proxmoxNode = selected.name();
            if (proxmoxNode == null || proxmoxNode.isBlank()) {
                logger.warn(
                        "Discovered Proxmox node has blank name for provider {} and tenant grant {}",
                        providerEntity.getId(),
                        tenantDatacenterGrantId);
                return null;
            }
            return new ProxmoxNodeInfo(proxmoxNode, proxmoxNode);
        } catch (Exception e) {
            logger.warn(
                    "Failed to discover Proxmox nodes live for provider {} and tenant grant {}: {}",
                    providerEntity.getId(),
                    tenantDatacenterGrantId,
                    e.getMessage());
            return null;
        }
    }

    private String getPreferredStorage(UUID tenantDatacenterGrantId) {
        return "local-lvm";
    }

    /**
     * When new VM disks use LVM/thin pools but content-library images were uploaded to directory-backed
     * storage (e.g. {@code local:import/...}), return that storage's Proxmox id for {@code import-from}.
     */
    private Optional<String> resolveProxmoxImportSourceStorage(UUID providerId, String diskStoragePool) {
        if (diskStoragePool == null || diskStoragePool.isBlank()) {
            return Optional.empty();
        }
        String diskLower = diskStoragePool.toLowerCase(Locale.ROOT);
        if (!diskLower.contains("lvm")) {
            return Optional.empty();
        }
        List<ProviderStorageEntity> storages = providerStorageRepository.findByProviderIdAndEnabled(providerId, true);
        List<ProviderStorageEntity> candidates = new ArrayList<>();
        for (ProviderStorageEntity ps : storages) {
            if (!"proxmox".equalsIgnoreCase(ps.getProviderType())) {
                continue;
            }
            if (diskStoragePool.equals(ps.getExternalId())) {
                continue;
            }
            if (!pathStyleProxmoxImportStorage(ps)) {
                continue;
            }
            if (!storageAcceptsImportContent(ps)) {
                continue;
            }
            candidates.add(ps);
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        candidates.sort(Comparator.comparing(ProviderStorageEntity::getExternalId, String.CASE_INSENSITIVE_ORDER));
        Optional<ProviderStorageEntity> localNamed =
                candidates.stream().filter(s -> "local".equalsIgnoreCase(s.getExternalId())).findFirst();
        return Optional.of(localNamed.orElse(candidates.getFirst()).getExternalId());
    }

    private static boolean pathStyleProxmoxImportStorage(ProviderStorageEntity ps) {
        String t = ps.getStorageType();
        if (t == null) {
            return false;
        }
        return switch (t.toLowerCase(Locale.ROOT)) {
            case "dir", "nfs", "cifs" -> true;
            default -> false;
        };
    }

    private static boolean storageAcceptsImportContent(ProviderStorageEntity ps) {
        if (ps.getCapabilities() == null) {
            return false;
        }
        Object raw = ps.getCapabilities().get("content_types");
        if (raw == null) {
            return false;
        }
        List<String> types = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o != null) {
                    types.add(o.toString().trim().toLowerCase(Locale.ROOT));
                }
            }
        } else {
            for (String part : raw.toString().split(",")) {
                String t = part.trim().toLowerCase(Locale.ROOT);
                if (!t.isEmpty()) {
                    types.add(t);
                }
            }
        }
        return types.contains("import");
    }

    private record ProxmoxNodeInfo(String nodeName, String nodeId) {}

    public Optional<VmProvider> resolveProviderForTenantDatacenter(UUID tenantDatacenterGrantId) {
        Optional<String> providerIdOpt = grantProviderResolver.resolveProviderId(tenantDatacenterGrantId);
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

