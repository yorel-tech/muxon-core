package com.onetattva.infron.gateway.controllers;

import com.onetattva.infron.api.NodesApi;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.services.NodesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class NodesController implements NodesApi {

    @Autowired
    private NodesService nodesService;

    @Override
    public ResponseEntity<Node> createNode(NodeCreate nodeCreate) {
        Node node = nodesService.createNode(nodeCreate);
        return ResponseEntity.status(201).body(node);
    }

    @Override
    public ResponseEntity<Void> deleteNode(UUID nodeId) {
        nodesService.deleteNode(nodeId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Node> getNode(UUID nodeId) {
        Node node = nodesService.getNode(nodeId);
        return ResponseEntity.ok(node);
    }

    @Override
    public ResponseEntity<NodeList> listNodes(Integer page, Integer perPage, String sort, UUID clusterId,
            UUID datacenterId, String name, NodeProviderType providerType) {
        NodeList nodeList = nodesService.listNodes(page, perPage, sort, clusterId, datacenterId, name,
                providerType);
        return ResponseEntity.ok(nodeList);
    }

    @Override
    public ResponseEntity<Map<String, String>> getNodeMetadata(UUID nodeId) {
        Map<String, String> metadata = nodesService.getNodeMetadata(nodeId);
        return ResponseEntity.ok(metadata);
    }

    @Override
    public ResponseEntity<Map<String, String>> updateNodeMetadata(UUID nodeId, Map<String, String> metadata) {
        Map<String, String> updatedMetadata = nodesService.updateNodeMetadata(nodeId, metadata);
        return ResponseEntity.ok(updatedMetadata);
    }
}
