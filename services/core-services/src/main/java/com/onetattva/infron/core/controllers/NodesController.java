package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.NodesApi;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.api.model.Node;
import com.onetattva.infron.api.model.NodeList;
import com.onetattva.infron.api.model.NodeProviderType;
import com.onetattva.infron.core.services.NodesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class NodesController implements NodesApi {

    @Autowired
    private NodesService nodesService;

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
}
