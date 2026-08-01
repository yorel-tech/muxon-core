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

import com.yorel.muxon.api.NodeClustersApi;
import com.yorel.muxon.api.model.NodeCluster;
import com.yorel.muxon.api.model.NodeClusterCreate;
import com.yorel.muxon.api.model.NodeClusterList;
import com.yorel.muxon.api.model.NodeClusterUpdate;
import com.yorel.muxon.api.model.NodeList;
import com.yorel.muxon.services.NodeClustersService;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NodeClustersController implements NodeClustersApi {

  @Autowired private NodeClustersService nodeClustersService;

  @Override
  public ResponseEntity<NodeCluster> createProviderNodeCluster(
      UUID providerId, NodeClusterCreate nodeClusterCreate) {
    NodeCluster nodeCluster =
        nodeClustersService.createProviderNodeCluster(providerId, nodeClusterCreate);
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
  public ResponseEntity<NodeClusterList> listProviderNodeClusters(
      UUID providerId, Integer page, Integer perPage, String sort, UUID datacenterId, String name) {
    NodeClusterList clusterList =
        nodeClustersService.listProviderNodeClusters(
            providerId, page, perPage, sort, datacenterId, name);
    return ResponseEntity.ok(clusterList);
  }

  @Override
  public ResponseEntity<NodeList> listClusterNodes(
      UUID clusterId, Integer page, Integer perPage, String sort) {
    NodeList nodeList = nodeClustersService.listClusterNodes(clusterId, page, perPage, sort);
    return ResponseEntity.ok(nodeList);
  }

  @Override
  public ResponseEntity<NodeCluster> patchNodeCluster(
      UUID clusterId, NodeClusterUpdate nodeClusterUpdate) {
    NodeCluster nodeCluster = nodeClustersService.updateNodeCluster(clusterId, nodeClusterUpdate);
    return ResponseEntity.ok(nodeCluster);
  }

  @Override
  public ResponseEntity<NodeCluster> updateNodeCluster(
      UUID clusterId, NodeClusterUpdate nodeClusterUpdate) {
    NodeCluster nodeCluster = nodeClustersService.updateNodeCluster(clusterId, nodeClusterUpdate);
    return ResponseEntity.ok(nodeCluster);
  }

  @Override
  public ResponseEntity<NodeClusterList> listDatacenterClusters(
      UUID datacenterId, Integer page, Integer perPage, String sort, String name) {
    NodeClusterList clusterList =
        nodeClustersService.listDatacenterClusters(datacenterId, page, perPage, sort, name);
    return ResponseEntity.ok(clusterList);
  }
}
