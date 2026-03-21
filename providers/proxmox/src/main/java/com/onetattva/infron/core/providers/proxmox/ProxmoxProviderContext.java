package com.onetattva.infron.core.providers.proxmox;

import com.onetattva.infron.core.providers.*;
import com.onetattva.infron.db.model.ProviderEntity;
import com.onetattva.infron.api.model.ProviderType;
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
    
    public ProxmoxProviderContext(ProviderEntity providerEntity,
                                 String targetNodeName,
                                 String targetNodeId,
                                 String preferredStoragePool) {
        this.providerEntity = providerEntity;
        this.targetNodeName = targetNodeName;
        this.targetNodeId = targetNodeId;
        this.preferredStoragePool = preferredStoragePool;
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
        return Map.of(
            "providerId", providerEntity.getId().toString(),
            "clusterEndpoint", providerEntity.getEndpoint(),
            "targetNode", targetNodeName,
            "targetNodeId", targetNodeId,
            "storagePool", preferredStoragePool,
            "nodeType", "proxmox"
        );
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
