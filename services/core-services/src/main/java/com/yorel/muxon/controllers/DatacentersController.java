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

import com.yorel.muxon.api.DatacentersApi;
import com.yorel.muxon.api.model.Datacenter;
import com.yorel.muxon.api.model.DatacenterCapacity;
import com.yorel.muxon.api.model.DatacenterCreate;
import com.yorel.muxon.api.model.DatacenterList;
import com.yorel.muxon.api.model.DatacenterNetworkCapabilities;
import com.yorel.muxon.api.model.DatacenterSettings;
import com.yorel.muxon.api.model.DatacenterUpdate;
import com.yorel.muxon.api.model.FabricNetwork;
import com.yorel.muxon.api.model.FabricNetworkCreate;
import com.yorel.muxon.api.model.FabricNetworkRole;
import com.yorel.muxon.api.model.FabricNetworkType;
import com.yorel.muxon.api.model.NetworkEdgeNode;
import com.yorel.muxon.api.model.NetworkEdgeNodeCreate;
import com.yorel.muxon.api.model.NetworkEdgeNodeStatus;
import com.yorel.muxon.api.model.NetworkEdgeNodeType;
import com.yorel.muxon.api.model.ProviderType;
import com.yorel.muxon.api.model.PublicIpPool;
import com.yorel.muxon.api.model.PublicIpPoolCreate;
import com.yorel.muxon.api.model.StorageClassValidationResult;
import com.yorel.muxon.db.model.DatacenterNetworkCapabilitiesEntity;
import com.yorel.muxon.db.model.FabricNetworkEntity;
import com.yorel.muxon.db.model.NetworkEdgeNodeEntity;
import com.yorel.muxon.db.model.PublicIpPoolEntity;
import com.yorel.muxon.services.DatacenterNetworkCapabilitiesService;
import com.yorel.muxon.services.DatacentersService;
import com.yorel.muxon.services.FabricNetworksService;
import com.yorel.muxon.services.NetworkEdgeNodesService;
import com.yorel.muxon.services.PublicIpPoolsService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DatacentersController implements DatacentersApi {

  @Autowired private DatacentersService datacentersService;

  @Autowired private FabricNetworksService fabricNetworksService;

  @Autowired private PublicIpPoolsService publicIpPoolsService;

  @Autowired private NetworkEdgeNodesService edgeNodesService;

  @Autowired private DatacenterNetworkCapabilitiesService capabilitiesService;

  @Override
  public ResponseEntity<Datacenter> createDatacenter(DatacenterCreate datacenterCreate) {
    Datacenter datacenter = datacentersService.createDatacenter(datacenterCreate);
    return ResponseEntity.status(201).body(datacenter);
  }

  @Override
  public ResponseEntity<Void> deleteDatacenter(UUID datacenterId) {
    datacentersService.deleteDatacenter(datacenterId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Datacenter> getDatacenter(UUID datacenterId) {
    Datacenter datacenter = datacentersService.getDatacenter(datacenterId);
    return ResponseEntity.ok(datacenter);
  }

  @Override
  public ResponseEntity<DatacenterSettings> getDatacenterSettings(UUID datacenterId) {
    DatacenterSettings settings = datacentersService.getDatacenterSettings(datacenterId);
    return ResponseEntity.ok(settings);
  }

  @Override
  public ResponseEntity<DatacenterList> listDatacenters(
      Integer page, Integer perPage, ProviderType providerType) {
    DatacenterList datacenterList = datacentersService.listDatacenters(page, perPage, providerType);
    return ResponseEntity.ok(datacenterList);
  }

  @Override
  public ResponseEntity<Datacenter> replaceDatacenter(
      UUID datacenterId, DatacenterUpdate datacenterUpdate) {
    Datacenter datacenter = datacentersService.replaceDatacenter(datacenterId, datacenterUpdate);
    return ResponseEntity.ok(datacenter);
  }

  @Override
  public ResponseEntity<DatacenterSettings> replaceDatacenterSettings(
      UUID datacenterId, DatacenterSettings datacenterSettings) {
    DatacenterSettings settings =
        datacentersService.replaceDatacenterSettings(datacenterId, datacenterSettings);
    return ResponseEntity.ok(settings);
  }

  @Override
  public ResponseEntity<Datacenter> updateDatacenter(
      UUID datacenterId, DatacenterUpdate datacenterUpdate) {
    Datacenter datacenter = datacentersService.updateDatacenter(datacenterId, datacenterUpdate);
    return ResponseEntity.ok(datacenter);
  }

  @Override
  public ResponseEntity<DatacenterSettings> updateDatacenterSettings(
      UUID datacenterId, DatacenterSettings datacenterSettings) {
    DatacenterSettings settings =
        datacentersService.updateDatacenterSettings(datacenterId, datacenterSettings);
    return ResponseEntity.ok(settings);
  }

  // @PutMapping("/datacenters/{datacenterId}/capacity")
  public ResponseEntity<DatacenterCapacity> updateDatacenterCapacity(
      UUID datacenterId, DatacenterCapacity capacity) {
    DatacenterCapacity updated =
        datacentersService.updateDatacenterCapacity(datacenterId, capacity);
    return ResponseEntity.ok(updated);
  }

  @Override
  public ResponseEntity<java.util.List<String>> getDatacenterStorageClasses(UUID datacenterId) {
    java.util.List<String> storageClasses =
        datacentersService.getAvailableStorageClasses(datacenterId);
    return ResponseEntity.ok(storageClasses);
  }

  @Override
  public ResponseEntity<StorageClassValidationResult> validateDatacenterStorageClasses(
      UUID datacenterId, java.util.List<String> requestBody) {
    StorageClassValidationResult result =
        datacentersService.validateDatacenterStorageClasses(datacenterId, requestBody);
    return ResponseEntity.ok(result);
  }

  // ── Fabric networks ──────────────────────────────────────────────────────

  @Override
  public ResponseEntity<List<FabricNetwork>> listFabricNetworks(UUID datacenterId) {
    return ResponseEntity.ok(
        fabricNetworksService.listByDatacenter(datacenterId).stream()
            .map(this::toFabricNetwork)
            .toList());
  }

  @Override
  public ResponseEntity<FabricNetwork> createFabricNetwork(
      UUID datacenterId, FabricNetworkCreate create) {
    FabricNetworkEntity entity =
        fabricNetworksService.create(
            datacenterId,
            create.getName(),
            FabricNetworkEntity.FabricNetworkType.valueOf(create.getType().name()),
            FabricNetworkEntity.FabricNetworkRole.valueOf(create.getRole().name()),
            create.getExternalId(),
            create.getVlanId(),
            null);
    return ResponseEntity.status(201).body(toFabricNetwork(entity));
  }

  @Override
  public ResponseEntity<FabricNetwork> getFabricNetwork(UUID datacenterId, UUID fabricNetworkId) {
    return ResponseEntity.ok(toFabricNetwork(fabricNetworksService.getById(fabricNetworkId)));
  }

  @Override
  public ResponseEntity<Void> deleteFabricNetwork(UUID datacenterId, UUID fabricNetworkId) {
    fabricNetworksService.delete(fabricNetworkId);
    return ResponseEntity.noContent().build();
  }

  // ── Public IP pools ──────────────────────────────────────────────────────

  @Override
  public ResponseEntity<List<PublicIpPool>> listPublicIpPools(UUID datacenterId) {
    return ResponseEntity.ok(
        publicIpPoolsService.listByDatacenter(datacenterId).stream()
            .map(this::toPublicIpPool)
            .toList());
  }

  @Override
  public ResponseEntity<PublicIpPool> createPublicIpPool(
      UUID datacenterId, PublicIpPoolCreate create) {
    PublicIpPoolEntity entity =
        publicIpPoolsService.create(
            datacenterId, create.getCidr(), create.getName(), create.getDescription());
    return ResponseEntity.status(201).body(toPublicIpPool(entity));
  }

  @Override
  public ResponseEntity<PublicIpPool> getPublicIpPool(UUID datacenterId, UUID poolId) {
    return ResponseEntity.ok(toPublicIpPool(publicIpPoolsService.getById(poolId)));
  }

  @Override
  public ResponseEntity<Void> deletePublicIpPool(UUID datacenterId, UUID poolId) {
    publicIpPoolsService.delete(poolId);
    return ResponseEntity.noContent().build();
  }

  // ── Network edge nodes ───────────────────────────────────────────────────

  @Override
  public ResponseEntity<List<NetworkEdgeNode>> listNetworkEdgeNodes(UUID datacenterId) {
    return ResponseEntity.ok(
        edgeNodesService.listByDatacenter(datacenterId).stream()
            .map(this::toNetworkEdgeNode)
            .toList());
  }

  @Override
  public ResponseEntity<NetworkEdgeNode> createNetworkEdgeNode(
      UUID datacenterId, NetworkEdgeNodeCreate create) {
    NetworkEdgeNodeEntity entity =
        edgeNodesService.register(
            datacenterId,
            create.getName(),
            create.getHost(),
            NetworkEdgeNodeEntity.NetworkEdgeNodeType.valueOf(create.getType().name()),
            null);
    return ResponseEntity.status(201).body(toNetworkEdgeNode(entity));
  }

  @Override
  public ResponseEntity<NetworkEdgeNode> getNetworkEdgeNode(UUID datacenterId, UUID nodeId) {
    return ResponseEntity.ok(toNetworkEdgeNode(edgeNodesService.getById(nodeId)));
  }

  @Override
  public ResponseEntity<NetworkEdgeNode> updateNetworkEdgeNode(
      UUID datacenterId, UUID nodeId, NetworkEdgeNodeCreate create) {
    NetworkEdgeNodeEntity.NetworkEdgeNodeType type =
        create.getType() != null
            ? NetworkEdgeNodeEntity.NetworkEdgeNodeType.valueOf(create.getType().name())
            : null;
    NetworkEdgeNodeEntity entity =
        edgeNodesService.update(nodeId, create.getName(), create.getHost(), type, null);
    return ResponseEntity.ok(toNetworkEdgeNode(entity));
  }

  @Override
  public ResponseEntity<Void> deleteNetworkEdgeNode(UUID datacenterId, UUID nodeId) {
    edgeNodesService.deactivate(nodeId);
    return ResponseEntity.noContent().build();
  }

  // ── Network capabilities ─────────────────────────────────────────────────

  @Override
  public ResponseEntity<DatacenterNetworkCapabilities> getDatacenterNetworkCapabilities(
      UUID datacenterId) {
    return ResponseEntity.ok(toCapabilities(capabilitiesService.getByDatacenterId(datacenterId)));
  }

  @Override
  public ResponseEntity<DatacenterNetworkCapabilities> updateDatacenterNetworkCapabilities(
      UUID datacenterId, DatacenterNetworkCapabilities body) {
    DatacenterNetworkCapabilitiesEntity entity =
        capabilitiesService.update(
            datacenterId,
            body.getPublicIpSupported(),
            body.getVpnSupported(),
            body.getBgpSupported(),
            body.getHaGatewaySupported(),
            body.getVxlanSupported(),
            body.getMultiRegionSupported(),
            body.getL7LbSupported(),
            body.getIpv6Supported(),
            body.getDualStackSupported());
    return ResponseEntity.ok(toCapabilities(entity));
  }

  // ── Mappers ──────────────────────────────────────────────────────────────

  private FabricNetwork toFabricNetwork(FabricNetworkEntity entity) {
    FabricNetwork api = new FabricNetwork();
    api.setId(entity.getId());
    if (entity.getNodeCluster() != null) {
      api.setNodeClusterId(entity.getNodeCluster().getId());
    }
    api.setName(entity.getName());
    if (entity.getType() != null) {
      api.setType(FabricNetworkType.fromValue(entity.getType().name()));
    }
    if (entity.getRole() != null) {
      api.setRole(FabricNetworkRole.fromValue(entity.getRole().name()));
    }
    api.setExternalId(entity.getExternalId());
    api.setVlanId(entity.getVlanId());
    if (entity.getStatus() != null) {
      api.setStatus(FabricNetwork.StatusEnum.fromValue(entity.getStatus().name()));
    }
    api.setCreatedAt(toOffset(entity.getCreatedAt()));
    api.setUpdatedAt(toOffset(entity.getUpdatedAt()));
    return api;
  }

  private PublicIpPool toPublicIpPool(PublicIpPoolEntity entity) {
    PublicIpPool api = new PublicIpPool();
    api.setId(entity.getId());
    if (entity.getDatacenter() != null) {
      api.setDatacenterId(entity.getDatacenter().getId());
    }
    api.setCidr(entity.getCidr());
    api.setName(entity.getName());
    api.setDescription(entity.getDescription());
    api.setTotalIps(entity.getTotalIps());
    api.setAllocatedIps(entity.getAllocatedIps());
    api.setAvailableIps(entity.getAvailableIps());
    if (entity.getStatus() != null) {
      api.setStatus(PublicIpPool.StatusEnum.fromValue(entity.getStatus()));
    }
    api.setCreatedAt(toOffset(entity.getCreatedAt()));
    return api;
  }

  private NetworkEdgeNode toNetworkEdgeNode(NetworkEdgeNodeEntity entity) {
    NetworkEdgeNode api = new NetworkEdgeNode();
    api.setId(entity.getId());
    if (entity.getDatacenter() != null) {
      api.setDatacenterId(entity.getDatacenter().getId());
    }
    api.setName(entity.getName());
    api.setHost(entity.getHost());
    if (entity.getType() != null) {
      api.setType(NetworkEdgeNodeType.fromValue(entity.getType().name()));
    }
    if (entity.getStatus() != null) {
      api.setStatus(NetworkEdgeNodeStatus.fromValue(entity.getStatus().name()));
    }
    api.setCreatedAt(toOffset(entity.getCreatedAt()));
    api.setUpdatedAt(toOffset(entity.getUpdatedAt()));
    return api;
  }

  private DatacenterNetworkCapabilities toCapabilities(DatacenterNetworkCapabilitiesEntity entity) {
    DatacenterNetworkCapabilities api = new DatacenterNetworkCapabilities();
    if (entity.getDatacenter() != null) {
      api.setDatacenterId(entity.getDatacenter().getId());
    }
    api.setPublicIpSupported(entity.isPublicIpSupported());
    api.setVpnSupported(entity.isVpnSupported());
    api.setBgpSupported(entity.isBgpSupported());
    api.setHaGatewaySupported(entity.isHaGatewaySupported());
    api.setVxlanSupported(entity.isVxlanSupported());
    api.setMultiRegionSupported(entity.isMultiRegionSupported());
    api.setL7LbSupported(entity.isL7LbSupported());
    api.setIpv6Supported(entity.isIpv6Supported());
    api.setDualStackSupported(entity.isDualStackSupported());
    api.setUpdatedAt(toOffset(entity.getUpdatedAt()));
    return api;
  }

  private static OffsetDateTime toOffset(java.time.Instant instant) {
    return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
  }
}
