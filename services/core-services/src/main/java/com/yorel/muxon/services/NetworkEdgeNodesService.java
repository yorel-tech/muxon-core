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
package com.yorel.muxon.services;

import com.yorel.muxon.db.model.DatacenterEntity;
import com.yorel.muxon.db.model.NetworkEdgeNodeEntity;
import com.yorel.muxon.db.model.NetworkEdgeNodeEntity.NetworkEdgeNodeStatus;
import com.yorel.muxon.db.model.NetworkEdgeNodeEntity.NetworkEdgeNodeType;
import com.yorel.muxon.db.repository.DatacenterRepository;
import com.yorel.muxon.db.repository.NetworkEdgeNodeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NetworkEdgeNodesService {

  @Autowired private NetworkEdgeNodeRepository edgeNodeRepository;

  @Autowired private DatacenterRepository datacenterRepository;

  public List<NetworkEdgeNodeEntity> listByDatacenter(UUID datacenterId) {
    return edgeNodeRepository.findByDatacenterId(datacenterId);
  }

  @Transactional
  public NetworkEdgeNodeEntity register(
      UUID datacenterId, String name, String host, NetworkEdgeNodeType type, String capabilities) {
    DatacenterEntity dc =
        datacenterRepository
            .findById(datacenterId)
            .orElseThrow(
                () -> new EntityNotFoundException("Datacenter not found: " + datacenterId));

    // In OSS, only one STANDARD node per datacenter
    if (type == NetworkEdgeNodeType.STANDARD
        && edgeNodeRepository.existsByDatacenterIdAndType(
            datacenterId, NetworkEdgeNodeType.STANDARD)) {
      throw new IllegalStateException(
          "CONFLICT: A STANDARD edge node already exists in this datacenter. Enterprise supports HA pairs.");
    }

    NetworkEdgeNodeEntity entity = new NetworkEdgeNodeEntity();
    entity.setDatacenter(dc);
    entity.setName(name);
    entity.setHost(host);
    entity.setType(type);
    entity.setStatus(NetworkEdgeNodeStatus.ACTIVE);
    entity.setCapabilities(capabilities);
    return edgeNodeRepository.save(entity);
  }

  public NetworkEdgeNodeEntity getById(UUID nodeId) {
    return edgeNodeRepository
        .findById(nodeId)
        .orElseThrow(() -> new EntityNotFoundException("Network edge node not found: " + nodeId));
  }

  @Transactional
  public NetworkEdgeNodeEntity update(
      UUID nodeId, String name, String host, NetworkEdgeNodeType type, String capabilities) {
    NetworkEdgeNodeEntity entity = getById(nodeId);
    if (name != null) entity.setName(name);
    if (host != null) entity.setHost(host);
    if (type != null) entity.setType(type);
    if (capabilities != null) entity.setCapabilities(capabilities);
    return edgeNodeRepository.save(entity);
  }

  @Transactional
  public void deactivate(UUID nodeId) {
    NetworkEdgeNodeEntity entity = getById(nodeId);
    entity.setStatus(NetworkEdgeNodeStatus.INACTIVE);
    edgeNodeRepository.save(entity);
  }

  public Optional<NetworkEdgeNodeEntity> findActiveEdgeNode(UUID datacenterId) {
    return edgeNodeRepository.findFirstByDatacenterIdAndStatus(
        datacenterId, NetworkEdgeNodeStatus.ACTIVE);
  }
}
