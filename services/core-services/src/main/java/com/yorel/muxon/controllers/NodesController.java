/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.controllers;

import com.yorel.muxon.api.NodesApi;
import com.yorel.muxon.api.model.Node;
import com.yorel.muxon.api.model.NodeCreate;
import com.yorel.muxon.api.model.NodeList;
import com.yorel.muxon.api.model.NodeProviderType;
import com.yorel.muxon.api.model.NodeUpdate;
import com.yorel.muxon.services.NodesService;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NodesController implements NodesApi {

  @Autowired private NodesService nodesService;

  @Override
  public ResponseEntity<Node> getNode(UUID providerId, UUID nodeId) {
    Node node = nodesService.getNode(nodeId);
    return ResponseEntity.ok(node);
  }

  @Override
  public ResponseEntity<NodeList> listNodes(
      UUID providerId,
      Integer page,
      Integer perPage,
      String sort,
      UUID clusterId,
      String name,
      NodeProviderType providerType) {
    NodeList nodeList =
        nodesService.listNodes(page, perPage, sort, clusterId, null, name, providerType);
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
