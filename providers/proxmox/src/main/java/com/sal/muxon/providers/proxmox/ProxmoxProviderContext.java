package com.sal.muxon.providers.proxmox;

import com.sal.muxon.providers.*;
import com.sal.muxon.db.model.ProviderEntity;
import com.sal.muxon.api.model.ProviderType;
import java.util.*;

/**
 * Proxmox-specific provider context
 * Uses external resource names and IDs (cluster, node names, VMIDs) - database independent
 */
public class ProxmoxProviderContext implements ProviderContext {
    
    private final ProviderEntity providerEntity;
    private final String targetNodeName;  // Proxmox node name
    private final String targetNodeId;    // Proxmox node ID (numeric string)
    private final String preferredStoragePool;
    /** Proxmox storage id where disk-import images live (e.g. {@code local}); null if same as {@link #preferredStoragePool}. */
    private final String importSourceStorage;

    public ProxmoxProviderContext(ProviderEntity providerEntity,
                                 String targetNodeName,
                                 String targetNodeId,
                                 String preferredStoragePool) {
        this(providerEntity, targetNodeName, targetNodeId, preferredStoragePool, null);
    }

    public ProxmoxProviderContext(ProviderEntity providerEntity,
                                 String targetNodeName,
                                 String targetNodeId,
                                 String preferredStoragePool,
                                 String importSourceStorage) {
        this.providerEntity = providerEntity;
        this.targetNodeName = targetNodeName;
        this.targetNodeId = targetNodeId;
        this.preferredStoragePool = preferredStoragePool;
        this.importSourceStorage = importSourceStorage != null && !importSourceStorage.isBlank()
                ? importSourceStorage.trim()
                : null;
    }
    
    @Override
    public ProviderType getProviderType() {
        return ProviderType.PROXMOX;
    }
    
    @Override
    public ProviderConnectionInfo getConnectionInfo() {
        return ProviderConnectionInfo.builder()
            .endpoint(providerEntity.getEndpoint())  // Proxmox cluster URL
            .credentials(providerEntity.getCredentials())  // API token or username/password
            .connectionConfig(Map.of(
                "clusterEndpoint", providerEntity.getEndpoint(),
                "targetNode", targetNodeName,
                "targetNodeId", targetNodeId,
                "verifySsl", "false"  // For self-signed certificates
            ))
            .build();
    }
    
    @Override
    public Optional<ProviderPlacementInfo> getPlacementInfo() {
        Reference nodeRef = Reference.external(targetNodeName, targetNodeId);
        
        return Optional.of(ProviderPlacementInfo.builder()
            .targetResource(nodeRef)
            .preferences(Map.of(
                "storagePool", preferredStoragePool != null ? preferredStoragePool : "local-lvm",
                "networkBridge", "vmbr0",
                "nodeName", targetNodeName,
                "nodeId", targetNodeId
            ))
            .constraints(Map.of(
                "clusterManaged", "true"
            ))
            .build());
    }
    
    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("providerId", providerEntity.getId().toString());
        m.put("clusterEndpoint", providerEntity.getEndpoint());
        m.put("targetNode", targetNodeName);
        m.put("targetNodeId", targetNodeId);
        m.put("storagePool", preferredStoragePool);
        m.put("nodeType", "proxmox");
        if (importSourceStorage != null
                && (preferredStoragePool == null || !importSourceStorage.equals(preferredStoragePool))) {
            m.put("importSourceStorage", importSourceStorage);
        }
        return Collections.unmodifiableMap(m);
    }
    
    @Override
    public Optional<Reference> getTargetResource() {
        return Optional.of(Reference.external(targetNodeName, targetNodeId));
    }
    
    // Helper methods
    public String getClusterEndpoint() {
        return providerEntity.getEndpoint();
    }
    
    public Map<String, String> getCredentials() {
        return providerEntity.getCredentials();
    }
    
    public String getTargetNodeId() {
        return targetNodeId;
    }
}
