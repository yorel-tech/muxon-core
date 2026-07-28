package com.yorel.muxon.controllers;

import com.yorel.muxon.api.NodesApi;
import com.yorel.muxon.api.model.*;
import com.yorel.muxon.api.model.Node;
import com.yorel.muxon.api.model.NodeCreate;
import com.yorel.muxon.api.model.NodeList;
import com.yorel.muxon.api.model.NodeProviderType;
import com.yorel.muxon.api.model.NodeUpdate;
import com.yorel.muxon.services.NodesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class NodesController implements NodesApi {

    @Autowired
    private NodesService nodesService;

    @Override
    public ResponseEntity<Node> getNode(UUID providerId, UUID nodeId) {
        Node node = nodesService.getNode(nodeId);
        return ResponseEntity.ok(node);
    }

    @Override
    public ResponseEntity<NodeList> listNodes(UUID providerId, Integer page, Integer perPage, String sort,
                                              UUID clusterId, String name, NodeProviderType providerType) {
        NodeList nodeList = nodesService.listNodes(page, perPage, sort, clusterId, null, name,
                providerType);
        return ResponseEntity.ok(nodeList);
    }

    @Override
    public ResponseEntity<Node> createNode(UUID providerId, NodeCreate nodeCreate) {
        Node node = nodesService.createNode(providerId, nodeCreate);
        return new ResponseEntity<>(node, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Node> updateNode(UUID providerId, UUID nodeId, NodeUpdate nodeUpdate) {
        Node node = nodesService.updateNode(nodeId, nodeUpdate);
        return ResponseEntity.ok(node);
    }

    @Override
    public ResponseEntity<Void> deleteNode(UUID providerId, UUID nodeId) {
        nodesService.deleteNode(nodeId);
        return ResponseEntity.noContent().build();
    }
}
