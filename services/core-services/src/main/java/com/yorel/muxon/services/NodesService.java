package com.yorel.muxon.services;

import com.yorel.muxon.api.model.EntityReference;
import com.yorel.muxon.api.model.Node;
import com.yorel.muxon.api.model.NodeCreate;
import com.yorel.muxon.api.model.NodeList;
import com.yorel.muxon.api.model.NodeProviderType;
import com.yorel.muxon.api.model.NodeUpdate;
import com.yorel.muxon.db.model.NodeClusterEntity;
import com.yorel.muxon.db.model.NodeEntity;
import com.yorel.muxon.db.model.ProviderEntity;
import com.yorel.muxon.db.repository.NodeRepository;
import com.yorel.muxon.db.repository.NodeClusterRepository;
import com.yorel.muxon.db.repository.ProviderRepository;
import com.yorel.muxon.api.model.ProviderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class NodesService {

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private NodeClusterRepository nodeClusterRepository;

    @Autowired
    private ProviderRepository providerRepository;

    public Node getNode(UUID nodeId) {
        NodeEntity entity = nodeRepository.findById(nodeId)
                .orElseThrow();
        return mapEntityToApi(entity);
    }

    public NodeList listNodes(Integer page, Integer perPage, String sort, UUID clusterId, UUID datacenterId,
                              String name, NodeProviderType providerType) {
        List<NodeEntity> entities;

        // TODO: Implement proper filtering with pagination
        // For now, return all entities
        entities = nodeRepository.findAll().stream()
                .filter(entity -> clusterId == null
                        || (entity.getCluster() != null && entity.getCluster().getId().equals(clusterId)))
                .filter(entity -> datacenterId == null || false) // Remove datacenter filtering since field doesn't
                                                                 // exist
                .filter(entity -> name == null || (entity.getName() != null && entity.getName().contains(name)))
                .filter(entity -> providerType == null || false) // Remove providerType filtering since field doesn't
                                                                 // exist
                .limit(perPage != null ? perPage : 20)
                .toList();

        List<Node> apiNodes = entities.stream()
                .map(this::mapEntityToApi)
                .toList();

        NodeList nodeList = new NodeList();
        nodeList.setTotal(apiNodes.size());
        nodeList.setPage(page != null ? page : 1);
        nodeList.setPerPage(perPage != null ? perPage : 20);
        nodeList.setItems(apiNodes);
        return nodeList;
    }

    public java.util.Map<String, String> getNodeMetadata(UUID nodeId) {
        NodeEntity entity = nodeRepository.findById(nodeId)
                .orElseThrow();
        return entity.getMetadata();
    }

    public NodeList listProviderNodes(UUID providerId, Integer page, Integer perPage, String sort,
                                  UUID clusterId, String name) {
        // Validate provider type - only LIBVIRT providers support node listing
        ProviderEntity provider = providerRepository
                .findById(providerId)
                .orElseThrow();
        
        if (provider.getType() != ProviderType.LIBVIRT) {
            throw new IllegalArgumentException("Node listing is only supported for LIBVIRT providers. Current provider type: " + provider.getType());
        }

        List<NodeEntity> entities;

        // TODO: Implement proper filtering with pagination
        // For now, return all entities
        entities = nodeRepository.findAll().stream()
                .filter(entity -> providerId == null
                        || (entity.getProvider() != null && entity.getProvider().getId().equals(providerId)))
                .filter(entity -> clusterId == null
                        || (entity.getCluster() != null && entity.getCluster().getId().equals(clusterId)))
                .filter(entity -> name == null || (entity.getName() != null && entity.getName().contains(name)))
                .limit(perPage != null ? perPage : 20)
                .toList();

        List<Node> apiNodes = entities.stream()
                .map(this::mapEntityToApi)
                .toList();

        NodeList nodeList = new NodeList();
        nodeList.setTotal(apiNodes.size());
        nodeList.setPage(page != null ? page : 1);
        nodeList.setPerPage(perPage != null ? perPage : 20);
        nodeList.setItems(apiNodes);
        return nodeList;
    }

    public Node createNode(UUID providerId, NodeCreate nodeCreate) {
        // Validate provider type - only LIBVIRT providers support node creation
        ProviderEntity provider = providerRepository
                .findById(providerId)
                .orElseThrow();
        
        if (provider.getType() != ProviderType.LIBVIRT) {
            throw new IllegalArgumentException("Node creation is only supported for LIBVIRT providers. Current provider type: " + provider.getType());
        }

        NodeEntity entity = new NodeEntity();
        entity.setName(nodeCreate.getName());
        entity.setProvider(provider);
        
        if (nodeCreate.getClusterId() != null) {
            NodeClusterEntity cluster = nodeClusterRepository.findById(nodeCreate.getClusterId())
                    .orElseThrow();
            entity.setCluster(cluster);
        }
        
        entity.setExternalId(nodeCreate.getExternalId());
        entity.setRole(nodeCreate.getRole());
        entity.setCpuTotal(nodeCreate.getCpuTotal());
        entity.setMemMb(nodeCreate.getMemMb());
        // Map status from request (string) to enum; default to UNKNOWN if null
        if (nodeCreate.getStatus() != null) {
            entity.setStatus(Node.StatusEnum.fromValue(nodeCreate.getStatus()));
        } else {
            entity.setStatus(Node.StatusEnum.UNKNOWN);
        }
        entity.setCredentials(nodeCreate.getCredentials());
        entity.setIpAddresses(nodeCreate.getIpAddresses());
        entity.setMetadata(nodeCreate.getMetadata());
        
        NodeEntity saved = nodeRepository.save(entity);
        return mapEntityToApi(saved);
    }

    public Node updateNode(UUID nodeId, NodeUpdate nodeUpdate) {
        NodeEntity entity = nodeRepository.findById(nodeId)
                .orElseThrow();

        // Validate provider type - only LIBVIRT providers support node updates
        if (entity.getProvider() != null && entity.getProvider().getType() != ProviderType.LIBVIRT) {
            throw new IllegalArgumentException("Node updates are only supported for LIBVIRT providers. Current provider type: " + entity.getProvider().getType());
        }

        if (nodeUpdate.getName() != null) {
            entity.setName(nodeUpdate.getName());
        }
        if (nodeUpdate.getClusterId() != null) {
            NodeClusterEntity cluster = nodeClusterRepository.findById(nodeUpdate.getClusterId())
                    .orElseThrow();
            entity.setCluster(cluster);
        }
        if (nodeUpdate.getExternalId() != null) {
            entity.setExternalId(nodeUpdate.getExternalId());
        }
        if (nodeUpdate.getRole() != null) {
            entity.setRole(nodeUpdate.getRole());
        }
        if (nodeUpdate.getCpuTotal() != null) {
            entity.setCpuTotal(nodeUpdate.getCpuTotal());
        }
        if (nodeUpdate.getMemMb() != null) {
            entity.setMemMb(nodeUpdate.getMemMb());
        }
        if (nodeUpdate.getStatus() != null) {
            entity.setStatus(Node.StatusEnum.fromValue(nodeUpdate.getStatus()));
        }
        if (nodeUpdate.getCredentials() != null) {
            entity.setCredentials(nodeUpdate.getCredentials());
        }
        if (nodeUpdate.getIpAddresses() != null) {
            entity.setIpAddresses(nodeUpdate.getIpAddresses());
        }
        if (nodeUpdate.getMetadata() != null) {
            entity.setMetadata(nodeUpdate.getMetadata());
        }

        entity.setUpdatedAt(Instant.now());
        NodeEntity saved = nodeRepository.save(entity);
        return mapEntityToApi(saved);
    }

    public java.util.Map<String, String> updateNodeMetadata(UUID nodeId, java.util.Map<String, String> metadata) {
        NodeEntity entity = nodeRepository.findById(nodeId)
                .orElseThrow();

        // Validate provider type - only LIBVIRT providers support node updates
        if (entity.getProvider() != null && entity.getProvider().getType() != ProviderType.LIBVIRT) {
            throw new IllegalArgumentException("Node updates are only supported for LIBVIRT providers. Current provider type: " + entity.getProvider().getType());
        }

        if (metadata != null) {
            entity.setMetadata(metadata);
        }

        entity.setUpdatedAt(Instant.now());
        NodeEntity saved = nodeRepository.save(entity);
        return saved.getMetadata();
    }

    public void deleteNode(UUID nodeId) {
        NodeEntity entity = nodeRepository.findById(nodeId)
                .orElseThrow();
        
        // Validate provider type - only LIBVIRT providers support node deletion
        if (entity.getProvider() != null && entity.getProvider().getType() != ProviderType.LIBVIRT) {
            throw new IllegalArgumentException("Node deletion is only supported for LIBVIRT providers. Current provider type: " + entity.getProvider().getType());
        }
        
        nodeRepository.deleteById(nodeId);
    }

    private Node mapEntityToApi(NodeEntity entity) {
        Node api = new Node();
        api.setId(entity.getId());

        // Map cluster reference
        if (entity.getCluster() != null) {
            api.setCluster(new EntityReference()
                    .id(entity.getCluster().getId())
                    .name(entity.getCluster().getName()));
        }

        // Map provider reference
        if (entity.getProvider() != null) api.setProvider(new EntityReference()
                .id(entity.getProvider().getId())
                .name(entity.getProvider().getName()));

        api.setName(entity.getName());
        api.setExternalId(entity.getExternalId());
        api.setRole(entity.getRole());
        api.setResources(entity.getResources());
        api.setStatus(entity.getStatus());

        if (entity.getLastSeenAt() != null) {
            api.setLastSeenAt(entity.getLastSeenAt().atOffset(ZoneOffset.UTC));
        }

        if (entity.getCredentials() != null) {
            api.setCredentials(entity.getCredentials());
        }
        if (entity.getMetadata() != null) {
            api.setMetadata(entity.getMetadata());
        }

        if (entity.getIpAddresses() != null) {
            api.setIpAddresses(entity.getIpAddresses());
        }

        if (entity.getCreatedAt() != null) {
            api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getUpdatedAt() != null) {
            api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }

        return api;
    }
}
