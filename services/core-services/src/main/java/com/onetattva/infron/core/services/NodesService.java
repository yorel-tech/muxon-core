package com.onetattva.infron.core.services;

import com.onetattva.infron.api.model.EntityReference;
import com.onetattva.infron.api.model.Node;
import com.onetattva.infron.api.model.NodeList;
import com.onetattva.infron.api.model.NodeProviderType;
import com.onetattva.infron.db.model.NodeEntity;
import com.onetattva.infron.db.repository.NodeRepository;
import com.onetattva.infron.db.repository.NodeClusterRepository;
import com.onetattva.infron.db.repository.ProviderRepository;
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

    public java.util.Map<String, String> updateNodeMetadata(UUID nodeId, java.util.Map<String, String> metadata) {
        NodeEntity entity = nodeRepository.findById(nodeId)
                .orElseThrow();

        if (metadata != null) {
            entity.setMetadata(metadata);
        }

        entity.setUpdatedAt(Instant.now());
        NodeEntity saved = nodeRepository.save(entity);
        return saved.getMetadata();
    }

    public void deleteNode(UUID nodeId) {
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
        if (entity.getProvider() != null) {
            api.setProvider(new EntityReference()
                    .id(entity.getProvider().getId())
                    .name(entity.getProvider().getName()));
        }

        api.setName(entity.getName());
        api.setExternalId(entity.getExternalId());
        api.setRole(entity.getRole());
        api.setCpuTotal(entity.getCpuTotal());
        api.setMemMb(entity.getMemMb());
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
