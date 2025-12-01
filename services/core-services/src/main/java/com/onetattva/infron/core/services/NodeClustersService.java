package com.onetattva.infron.core.services;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.db.model.DatacenterEntity;
import com.onetattva.infron.db.model.NodeClusterEntity;
import com.onetattva.infron.db.repository.NodeClusterRepository;
import com.onetattva.infron.db.repository.DatacenterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class NodeClustersService {

    @Autowired
    private NodeClusterRepository nodeClusterRepository;

    @Autowired
    private DatacenterRepository datacenterRepository;


    public NodeCluster createNodeCluster(NodeClusterCreate nodeClusterCreate) {
        NodeClusterEntity entity = new NodeClusterEntity();
        entity.setId(UUID.randomUUID());

        // Set datacenter
        DatacenterEntity datacenter = datacenterRepository
                .findById(nodeClusterCreate.getDatacenterId())
                .orElseThrow();
        entity.setDatacenter(datacenter);

        entity.setName(nodeClusterCreate.getName());

        if (nodeClusterCreate.getDescription() != null) {
            entity.setDescription(nodeClusterCreate.getDescription());
        }

        entity.setMetadata(nodeClusterCreate.getMetadata());

        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        NodeClusterEntity saved = nodeClusterRepository.save(entity);
        return mapEntityToApi(saved);
    }

    public NodeCluster getNodeCluster(UUID clusterId) {
        NodeClusterEntity entity = nodeClusterRepository.findById(clusterId)
                .orElseThrow();
        return mapEntityToApi(entity);
    }

    public NodeClusterList listNodeClusters(Integer page, Integer perPage, String sort, UUID datacenterId,
                                            String name) {
        List<NodeClusterEntity> entities;

        // TODO: Implement proper filtering with pagination
        // For now, return all entities
        entities = nodeClusterRepository.findAll().stream()
                .filter(entity -> datacenterId == null
                        || (entity.getDatacenter() != null && entity.getDatacenter().getId().equals(datacenterId)))
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

    public NodeCluster updateNodeCluster(UUID clusterId, NodeClusterUpdate nodeClusterUpdate) {
        NodeClusterEntity entity = nodeClusterRepository.findById(clusterId)
                .orElseThrow();

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
        entities = nodeClusterRepository.findAll().stream()
                .filter(entity -> entity.getDatacenter() != null && entity.getDatacenter().getId().equals(datacenterId))
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

        // Map datacenter reference
        if (entity.getDatacenter() != null) {
            api.setDatacenter(new EntityReference()
                    .id(entity.getDatacenter().getId())
                    .name(entity.getDatacenter().getName()));
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
