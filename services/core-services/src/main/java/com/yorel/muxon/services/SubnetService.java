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
import com.yorel.muxon.db.model.FabricNetworkEntity;
import com.yorel.muxon.db.model.SubnetEntity;
import com.yorel.muxon.db.model.SubnetEntity.SubnetStatus;
import com.yorel.muxon.db.model.SubnetRbacEntity;
import com.yorel.muxon.db.model.SubnetRbacEntity.SubnetPrincipalType;
import com.yorel.muxon.db.model.VpcEntity;
import com.yorel.muxon.db.repository.DatacenterRepository;
import com.yorel.muxon.db.repository.FabricNetworkRepository;
import com.yorel.muxon.db.repository.SubnetRbacRepository;
import com.yorel.muxon.db.repository.SubnetRepository;
import com.yorel.muxon.db.repository.VpcRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubnetService {

  @Autowired private SubnetRepository subnetRepository;

  @Autowired private VpcRepository vpcRepository;

  @Autowired private DatacenterRepository datacenterRepository;

  @Autowired private FabricNetworkRepository fabricNetworkRepository;

  @Autowired private SubnetRbacRepository subnetRbacRepository;

  @Autowired private FabricNetworksService fabricNetworksService;

  public List<SubnetEntity> listByVpc(UUID vpcId) {
    return subnetRepository.findByVpcId(vpcId);
  }

  public List<SubnetEntity> listByVpcForStack(UUID vpcId, UUID stackId) {
    List<UUID> authorizedSubnetIds = subnetRbacRepository.findAuthorizedSubnetIds(stackId, vpcId);
    return subnetRepository.findByVpcId(vpcId).stream()
        .filter(s -> authorizedSubnetIds.contains(s.getId()))
        .toList();
  }

  @Transactional
  public SubnetEntity create(
      UUID tenantId,
      UUID vpcId,
      String name,
      String cidr,
      UUID datacenterId,
      String gatewayIp,
      List<String> dnsServers,
      boolean isPublic,
      String availabilityZone) {
    VpcEntity vpc =
        vpcRepository
            .findById(vpcId)
            .orElseThrow(() -> new EntityNotFoundException("VPC not found: " + vpcId));

    if (!vpc.getTenant().getId().equals(tenantId)) {
      throw new EntityNotFoundException("VPC not found: " + vpcId);
    }

    // Validate CIDR is within VPC CIDR
    if (!isCidrContainedIn(cidr, vpc.getCidr())) {
      throw new IllegalArgumentException(
          "INVALID_CIDR: Subnet CIDR " + cidr + " must be within VPC CIDR " + vpc.getCidr());
    }

    // Check for CIDR overlap within this VPC
    UUID nullUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    if (subnetRepository.existsOverlappingCidrInVpc(vpcId, cidr, nullUUID)) {
      throw new IllegalStateException(
          "CONFLICT: Subnet CIDR " + cidr + " overlaps with existing subnet in this VPC");
    }

    DatacenterEntity datacenter =
        datacenterRepository
            .findById(datacenterId)
            .orElseThrow(
                () -> new EntityNotFoundException("Datacenter not found: " + datacenterId));

    // Auto-select TENANT_OVERLAY fabric network
    List<FabricNetworkEntity> tenantOverlayNetworks =
        fabricNetworksService.findActiveTenantOverlayNetworks(datacenterId);
    FabricNetworkEntity fabricNetwork =
        tenantOverlayNetworks.isEmpty() ? null : tenantOverlayNetworks.get(0);

    SubnetEntity entity = new SubnetEntity();
    entity.setVpc(vpc);
    entity.setDatacenter(datacenter);
    entity.setFabricNetwork(fabricNetwork);
    entity.setName(name);
    entity.setCidr(cidr);
    entity.setGatewayIp(gatewayIp);
    entity.setDnsServers(dnsServers);
    entity.setPublic(isPublic);
    entity.setAvailabilityZone(availabilityZone);
    entity.setStatus(SubnetStatus.PENDING);

    SubnetEntity saved = subnetRepository.save(entity);

    // Auto-create default subnet_rbac: all stacks in tenant can ATTACH
    createDefaultRbacBinding(saved, tenantId);

    return saved;
  }

  public SubnetEntity getById(UUID subnetId) {
    return subnetRepository
        .findById(subnetId)
        .orElseThrow(() -> new EntityNotFoundException("Subnet not found: " + subnetId));
  }

  @Transactional
  public SubnetEntity updateStatus(UUID subnetId, SubnetStatus status, String providerHandle) {
    SubnetEntity entity = getById(subnetId);
    entity.setStatus(status);
    if (providerHandle != null) entity.setProviderHandle(providerHandle);
    return subnetRepository.save(entity);
  }

  @Transactional
  public SubnetEntity update(UUID subnetId, String name, List<String> dnsServers) {
    SubnetEntity entity = getById(subnetId);
    if (name != null) entity.setName(name);
    if (dnsServers != null) entity.setDnsServers(dnsServers);
    return subnetRepository.save(entity);
  }

  @Transactional
  public void delete(UUID subnetId) {
    if (!subnetRepository.existsById(subnetId)) {
      throw new EntityNotFoundException("Subnet not found: " + subnetId);
    }
    subnetRepository.deleteById(subnetId);
  }

  private void createDefaultRbacBinding(SubnetEntity subnet, UUID tenantId) {
    // Default open binding for all stacks in the tenant (using tenantId as ROLE principal)
    SubnetRbacEntity rbac = new SubnetRbacEntity();
    rbac.setSubnet(subnet);
    rbac.setPrincipalType(SubnetPrincipalType.ROLE);
    rbac.setPrincipalId(tenantId);
    rbac.setPermissions(List.of("ATTACH", "VIEW"));
    subnetRbacRepository.save(rbac);
  }

  private boolean isCidrContainedIn(String cidr, String vpcCidr) {
    // Simple prefix-length based check
    try {
      String[] cidrParts = cidr.split("/");
      String[] vpcParts = vpcCidr.split("/");
      int cidrPrefix = Integer.parseInt(cidrParts[1]);
      int vpcPrefix = Integer.parseInt(vpcParts[1]);
      if (cidrPrefix < vpcPrefix) return false;

      long cidrAddr = ipToLong(cidrParts[0]);
      long vpcAddr = ipToLong(vpcParts[0]);
      long vpcMask = prefixToMask(vpcPrefix);
      return (cidrAddr & vpcMask) == (vpcAddr & vpcMask);
    } catch (Exception e) {
      return false;
    }
  }

  private long ipToLong(String ip) {
    String[] parts = ip.split("\\.");
    long result = 0;
    for (String part : parts) {
      result = result * 256 + Integer.parseInt(part.trim());
    }
    return result;
  }

  private long prefixToMask(int prefix) {
    return prefix == 0 ? 0 : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
  }
}
