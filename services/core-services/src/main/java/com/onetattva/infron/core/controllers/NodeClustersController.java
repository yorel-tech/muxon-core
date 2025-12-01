package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.NodeClustersApi;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.services.NodeClustersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class NodeClustersController implements NodeClustersApi {

    @Autowired
    private NodeClustersService nodeClustersService;

    @Override
    public ResponseEntity<NodeCluster> createNodeCluster(NodeClusterCreate nodeClusterCreate) {
        NodeCluster nodeCluster = nodeClustersService.createNodeCluster(nodeClusterCreate);
        return ResponseEntity.status(201).body(nodeCluster);
    }

    @Override
    public ResponseEntity<Void> deleteNodeCluster(UUID clusterId) {
        nodeClustersService.deleteNodeCluster(clusterId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<NodeCluster> getNodeCluster(UUID clusterId) {
        NodeCluster nodeCluster = nodeClustersService.getNodeCluster(clusterId);
        return ResponseEntity.ok(nodeCluster);
    }

    @Override
    public ResponseEntity<NodeClusterList> listNodeClusters(Integer page, Integer perPage, String sort,
                                                            UUID datacenterId, String name) {
        NodeClusterList clusterList = nodeClustersService.listNodeClusters(page, perPage, sort, datacenterId, name);
        return ResponseEntity.ok(clusterList);
    }

    @Override
    public ResponseEntity<NodeList> listClusterNodes(UUID clusterId, Integer page, Integer perPage, String sort) {
        NodeList nodeList = nodeClustersService.listClusterNodes(clusterId, page, perPage, sort);
        return ResponseEntity.ok(nodeList);
    }

    @Override
    public ResponseEntity<NodeCluster> patchNodeCluster(UUID clusterId, NodeClusterUpdate nodeClusterUpdate) {
        NodeCluster nodeCluster = nodeClustersService.updateNodeCluster(clusterId, nodeClusterUpdate);
        return ResponseEntity.ok(nodeCluster);
    }

    @Override
    public ResponseEntity<NodeCluster> updateNodeCluster(UUID clusterId, NodeClusterUpdate nodeClusterUpdate) {
        NodeCluster nodeCluster = nodeClustersService.updateNodeCluster(clusterId, nodeClusterUpdate);
        return ResponseEntity.ok(nodeCluster);
    }

    @Override
    public ResponseEntity<NodeClusterList> listDatacenterClusters(UUID datacenterId, Integer page, Integer perPage,
            String sort, String name) {
        NodeClusterList clusterList = nodeClustersService.listDatacenterClusters(datacenterId, page, perPage, sort,
                name);
        return ResponseEntity.ok(clusterList);
    }
}
