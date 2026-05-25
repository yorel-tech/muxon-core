package com.sal.muxon.providers.libvirt;

import com.sal.muxon.providers.*;
import com.sal.muxon.db.model.ProviderEntity;
import com.sal.muxon.db.model.NodeEntity;
import com.sal.muxon.db.repository.NodeRepository;
import com.sal.muxon.api.model.ProviderType;
import java.util.*;

/**
 * Libvirt-specific provider context
 * Uses database IDs for node management (Libvirt manages individual nodes)
 */
public class LibvirtProviderContext implements ProviderContext {
    
    private final ProviderEntity providerEntity;
    private final NodeRepository nodeRepository;
    private final UUID targetNodeId;  // Database UUID
    
    public LibvirtProviderContext(ProviderEntity providerEntity, 
                                 NodeRepository nodeRepository,
                                 UUID targetNodeId) {
        this.providerEntity = providerEntity;
        this.nodeRepository = nodeRepository;
        this.targetNodeId = targetNodeId;
    }
    
    @Override
    public ProviderType getProviderType() {
        return ProviderType.LIBVIRT;
    }
    
    @Override
    public ProviderConnectionInfo getConnectionInfo() {
        return ProviderConnectionInfo.builder()
            .endpoint(providerEntity.getEndpoint())
            .credentials(providerEntity.getCredentials())
            .connectionConfig(Map.of("nodeId", targetNodeId.toString()))
            .build();
    }
    
    @Override
    public Optional<ProviderPlacementInfo> getPlacementInfo() {
        Optional<NodeEntity> nodeOpt = nodeRepository.findById(targetNodeId);
        if (nodeOpt.isEmpty()) {
            return Optional.empty();
        }
        
        NodeEntity node = nodeOpt.get();
        Reference nodeRef = Reference.internal(
            node.getName(), 
            node.getName(),  // For libvirt, name is used as external ID
            targetNodeId
        );
        
        return Optional.of(ProviderPlacementInfo.builder()
            .targetResource(nodeRef)
            .preferences(node.getMetadata() != null ? node.getMetadata() : Map.of())
            .constraints(Map.of())
            .build());
    }
    
    @Override
    public Map<String, Object> getMetadata() {
        return Map.of(
            "providerId", providerEntity.getId().toString(),
            "nodeId", targetNodeId.toString(),
            "nodeType", "libvirt"
        );
    }
    
    @Override
    public Optional<Reference> getTargetResource() {
        Optional<NodeEntity> nodeOpt = nodeRepository.findById(targetNodeId);
        return nodeOpt.map(node -> Reference.internal(
            node.getName(), 
            node.getName(),  // External ID for libvirt
            targetNodeId
        ));
    }
    
    // Helper method to get node entity
    public Optional<NodeEntity> getTargetNodeEntity() {
        return nodeRepository.findById(targetNodeId);
    }
}
