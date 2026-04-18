package com.krito.muxon.core.services;

import com.krito.muxon.api.model.*;
import com.krito.muxon.api.model.*;
import com.krito.muxon.db.model.ProviderEntity;
import com.krito.muxon.db.model.NodeClusterEntity;
import com.krito.muxon.db.repository.DatacenterRepository;
import com.krito.muxon.db.repository.NodeClusterRepository;
import com.krito.muxon.db.repository.ProviderRepository;
import com.krito.muxon.api.model.ProviderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class NodeClustersService {

    @Autowired
    private NodeClusterRepository nodeClusterRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private DatacenterRepository datacenterRepository;


    public NodeCluster createProviderNodeCluster(UUID providerId, NodeClusterCreate nodeClusterCreate) {
        // Validate provider type - only LIBVIRT providers support cluster creation
        ProviderEntity provider = providerRepository
                .findById(providerId)
                .orElseThrow();
        
        if (provider.getType() != ProviderType.LIBVIRT) {
            throw new IllegalArgumentException("Node cluster creation is only supported for LIBVIRT providers. Current provider type: " + provider.getType());
        }

        NodeClusterEntity entity = new NodeClusterEntity();

        // Set provider
        entity.setProvider(provider);

        entity.setName(nodeClusterCreate.getName());

        if (nodeClusterCreate.getDescription() != null) {
            entity.setDescription(nodeClusterCreate.getDescription());
        }

        entity.setStatus(NodeCluster.StatusEnum.UNKNOWN);
        entity.setMetadata(nodeClusterCreate.getMetadata());

        NodeClusterEntity saved = nodeClusterRepository.save(entity);
        return mapEntityToApi(saved);
    }

    public NodeCluster getNodeCluster(UUID clusterId) {
        NodeClusterEntity entity = nodeClusterRepository.findById(clusterId)
                .orElseThrow();
        return mapEntityToApi(entity);
    }

    @Transactional(readOnly = true)
    public NodeClusterList listProviderNodeClusters(UUID providerId, Integer page, Integer perPage, String sort,
                                            UUID datacenterId, String name) {
        final List<NodeClusterEntity> clustersForProvider = nodeClusterRepository.findByProvider_Id(providerId);
        List<NodeClusterEntity> entities = clustersForProvider;

        if (datacenterId != null) {
            entities = datacenterRepository.findById(datacenterId)
                    .filter(dc -> dc.getNodeCluster() != null
                            && dc.getNodeCluster().getProvider() != null
                            && providerId.equals(dc.getNodeCluster().getProvider().getId()))
                    .map(dc -> {
                        final UUID clusterId = dc.getNodeCluster().getId();
                        return clustersForProvider.stream()
                                .filter(e -> e.getId().equals(clusterId))
                                .toList();
                    })
                    .orElse(List.of());
        }

        Stream<NodeClusterEntity> stream = entities.stream();
        if (name != null && !name.isBlank()) {
            stream = stream.filter(e -> e.getName() != null && e.getName().contains(name));
        }
        List<NodeClusterEntity> filtered = stream.toList();

        int per = perPage != null ? perPage : 20;
        int p = page != null ? page : 1;
        int from = Math.max(0, (p - 1) * per);
        List<NodeClusterEntity> pageSlice = filtered.stream().skip(from).limit(per).toList();

        List<NodeCluster> apiClusters = pageSlice.stream()
                .map(this::mapEntityToApi)
                .toList();

        NodeClusterList clusterList = new NodeClusterList();
        clusterList.setTotal(filtered.size());
        clusterList.setPage(p);
        clusterList.setPerPage(per);
        clusterList.setItems(apiClusters);
        return clusterList;
    }

    public NodeCluster updateNodeCluster(UUID clusterId, NodeClusterUpdate nodeClusterUpdate) {
        NodeClusterEntity entity = nodeClusterRepository.findById(clusterId)
                .orElseThrow();

        // Validate provider type - only LIBVIRT providers support cluster updates
        if (entity.getProvider() != null && entity.getProvider().getType() != ProviderType.LIBVIRT) {
            throw new IllegalArgumentException("Node cluster updates are only supported for LIBVIRT providers. Current provider type: " + entity.getProvider().getType());
        }

        if (nodeClusterUpdate.getName() != null) {
            entity.setName(nodeClusterUpdate.getName());
        }
        if (nodeClusterUpdate.getDescription() != null) {
            entity.setDescription(nodeClusterUpdate.getDescription());
        }

        if (nodeClusterUpdate.getMetadata() != null) {
            entity.setMetadata(nodeClusterUpdate.getMetadata());
        }

        entity.setUpdatedAt(Instant.now());
        NodeClusterEntity saved = nodeClusterRepository.save(entity);
        return mapEntityToApi(saved);
    }

    public void deleteNodeCluster(UUID clusterId) {
        NodeClusterEntity entity = nodeClusterRepository.findById(clusterId)
                .orElseThrow();
        
        // Validate provider type - only LIBVIRT providers support cluster deletion
        if (entity.getProvider() != null && entity.getProvider().getType() != ProviderType.LIBVIRT) {
            throw new IllegalArgumentException("Node cluster deletion is only supported for LIBVIRT providers. Current provider type: " + entity.getProvider().getType());
        }
        
        nodeClusterRepository.deleteById(clusterId);
    }

    public NodeList listClusterNodes(UUID clusterId, Integer page, Integer perPage, String sort) {
        // TODO: Implement proper repository query to get nodes by clusterId
        // For now, return empty list
        NodeList nodeList = new NodeList();
        nodeList.setTotal(0);
        nodeList.setPage(page != null ? page : 1);
        nodeList.setPerPage(perPage != null ? perPage : 20);
        nodeList.setItems(List.of());
        return nodeList;
    }

    public NodeClusterList listDatacenterClusters(UUID datacenterId, Integer page, Integer perPage, String sort,
            String name) {
        List<NodeClusterEntity> entities;

        // TODO: Implement proper filtering with pagination
        // For now, return all entities (datacenter filtering will need to be implemented via provider relationship)
        entities = nodeClusterRepository.findAll().stream()
                .filter(entity -> name == null || (entity.getName() != null && entity.getName().contains(name)))
                .limit(perPage != null ? perPage : 20)
                .toList();

        List<NodeCluster> apiClusters = entities.stream()
                .map(this::mapEntityToApi)
                .toList();

        NodeClusterList clusterList = new NodeClusterList();
        clusterList.setTotal(apiClusters.size());
        clusterList.setPage(page != null ? page : 1);
        clusterList.setPerPage(perPage != null ? perPage : 20);
        clusterList.setItems(apiClusters);
        return clusterList;
    }

    private NodeCluster mapEntityToApi(NodeClusterEntity entity) {
        NodeCluster api = new NodeCluster();
        api.setId(entity.getId());

        // Map provider reference
        if (entity.getProvider() != null) {
            api.setProviderId(entity.getProvider().getId());
        }

        api.setName(entity.getName());
        api.setDescription(entity.getDescription());

        // Handle JSONB field
        api.setMetadata(entity.getMetadata());

        if (entity.getCreatedAt() != null) {
            api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getUpdatedAt() != null) {
            api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }

        return api;
    }
}
