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
import com.yorel.muxon.db.model.FloatingIpEntity;
import com.yorel.muxon.db.model.FloatingIpEntity.FloatingIpStatus;
import com.yorel.muxon.db.model.InternetGatewayEntity;
import com.yorel.muxon.db.model.NatGatewayEntity;
import com.yorel.muxon.db.model.NetworkEdgeNodeEntity;
import com.yorel.muxon.db.model.PublicIpPoolEntity;
import com.yorel.muxon.db.model.SubnetEntity;
import com.yorel.muxon.db.model.TenantEntity;
import com.yorel.muxon.db.model.VpcEntity;
import com.yorel.muxon.db.repository.DatacenterRepository;
import com.yorel.muxon.db.repository.FloatingIpRepository;
import com.yorel.muxon.db.repository.InternetGatewayRepository;
import com.yorel.muxon.db.repository.NatGatewayRepository;
import com.yorel.muxon.db.repository.SubnetRepository;
import com.yorel.muxon.db.repository.TenantRepository;
import com.yorel.muxon.db.repository.VpcRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GatewaysService {

  @Autowired private InternetGatewayRepository igwRepository;

  @Autowired private NatGatewayRepository natRepository;

  @Autowired private FloatingIpRepository floatingIpRepository;

  @Autowired private VpcRepository vpcRepository;

  @Autowired private SubnetRepository subnetRepository;

  @Autowired private DatacenterRepository datacenterRepository;

  @Autowired private TenantRepository tenantRepository;

  @Autowired private NetworkEdgeNodesService edgeNodesService;

  @Autowired private PublicIpPoolsService poolsService;

  // ─── Internet Gateways ────────────────────────────────────────────────────

  public Optional<InternetGatewayEntity> getInternetGateway(UUID vpcId) {
    return igwRepository.findByVpcId(vpcId);
  }

  @Transactional
  public InternetGatewayEntity createInternetGateway(UUID vpcId, String name) {
    VpcEntity vpc =
        vpcRepository
            .findById(vpcId)
            .orElseThrow(() -> new EntityNotFoundException("VPC not found: " + vpcId));

    if (igwRepository.existsByVpcId(vpcId)) {
      throw new IllegalStateException("CONFLICT: Internet gateway already exists for VPC " + vpcId);
    }

    // Find active edge node in the VPC's datacenter (subnets carry datacenter, use first subnet's
    // datacenter)
    List<SubnetEntity> subnets = subnetRepository.findByVpcId(vpcId);
    UUID datacenterId = subnets.isEmpty() ? null : subnets.get(0).getDatacenter().getId();

    NetworkEdgeNodeEntity edgeNode = null;
    if (datacenterId != null) {
      edgeNode =
          edgeNodesService
              .findActiveEdgeNode(datacenterId)
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "UNPROCESSABLE: No active network edge node available in datacenter "
                              + datacenterId));
    }

    InternetGatewayEntity entity = new InternetGatewayEntity();
    entity.setVpc(vpc);
    entity.setName(name);
    entity.setStatus("ACTIVE");
    entity.setNetworkEdgeNode(edgeNode);
    return igwRepository.save(entity);
  }

  @Transactional
  public void deleteInternetGateway(UUID vpcId) {
    InternetGatewayEntity igw =
        igwRepository
            .findByVpcId(vpcId)
            .orElseThrow(
                () -> new EntityNotFoundException("Internet gateway not found for VPC: " + vpcId));
    igwRepository.delete(igw);
  }

  // ─── NAT Gateways ─────────────────────────────────────────────────────────

  public List<NatGatewayEntity> listNatGateways(UUID vpcId) {
    return natRepository.findByVpcId(vpcId);
  }

  @Transactional
  public NatGatewayEntity createNatGateway(
      UUID vpcId, String name, UUID subnetId, UUID floatingIpId) {
    VpcEntity vpc =
        vpcRepository
            .findById(vpcId)
            .orElseThrow(() -> new EntityNotFoundException("VPC not found: " + vpcId));
    SubnetEntity subnet =
        subnetRepository
            .findById(subnetId)
            .orElseThrow(() -> new EntityNotFoundException("Subnet not found: " + subnetId));

    if (!subnet.isPublic()) {
      throw new IllegalArgumentException(
          "UNPROCESSABLE: NAT gateway must be placed in a public subnet");
    }

    FloatingIpEntity floatingIp =
        floatingIpRepository
            .findById(floatingIpId)
            .orElseThrow(
                () -> new EntityNotFoundException("Floating IP not found: " + floatingIpId));

    NetworkEdgeNodeEntity edgeNode =
        edgeNodesService.findActiveEdgeNode(subnet.getDatacenter().getId()).orElse(null);

    NatGatewayEntity entity = new NatGatewayEntity();
    entity.setVpc(vpc);
    entity.setSubnet(subnet);
    entity.setFloatingIp(floatingIp);
    entity.setName(name);
    entity.setStatus("ACTIVE");
    entity.setNetworkEdgeNode(edgeNode);
    return natRepository.save(entity);
  }

  public NatGatewayEntity getNatGateway(UUID natGatewayId) {
    return natRepository
        .findById(natGatewayId)
        .orElseThrow(() -> new EntityNotFoundException("NAT gateway not found: " + natGatewayId));
  }

  @Transactional
  public void deleteNatGateway(UUID natGatewayId) {
    natRepository.deleteById(natGatewayId);
  }

  // ─── Floating IPs ─────────────────────────────────────────────────────────

  public List<FloatingIpEntity> listFloatingIps(UUID tenantId, UUID datacenterId) {
    return floatingIpRepository.findByTenantIdAndDatacenterId(tenantId, datacenterId);
  }

  @Transactional
  public FloatingIpEntity allocateFloatingIp(UUID tenantId, UUID datacenterId) {
    TenantEntity tenant =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));
    DatacenterEntity dc =
        datacenterRepository
            .findById(datacenterId)
            .orElseThrow(
                () -> new EntityNotFoundException("Datacenter not found: " + datacenterId));

    // Find an available pool
    PublicIpPoolEntity pool =
        poolsService.listByDatacenter(datacenterId).stream()
            .filter(p -> "ACTIVE".equals(p.getStatus()) && p.getAllocatedIps() < p.getTotalIps())
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "POOL_EXHAUSTED: No available public IPs in datacenter " + datacenterId));

    poolsService.incrementAllocated(pool.getId());

    // Generate placeholder IP (real implementation would pull from pool CIDR)
    String ip = generateIpFromPool(pool);

    FloatingIpEntity entity = new FloatingIpEntity();
    entity.setTenant(tenant);
    entity.setDatacenter(dc);
    entity.setPublicIpPool(pool);
    entity.setIpAddress(ip);
    entity.setStatus(FloatingIpStatus.AVAILABLE);
    return floatingIpRepository.save(entity);
  }

  public FloatingIpEntity getFloatingIp(UUID floatingIpId) {
    return floatingIpRepository
        .findById(floatingIpId)
        .orElseThrow(() -> new EntityNotFoundException("Floating IP not found: " + floatingIpId));
  }

  @Transactional
  public FloatingIpEntity associateFloatingIp(UUID floatingIpId, UUID vmId, String privateIp) {
    FloatingIpEntity entity = getFloatingIp(floatingIpId);
    entity.setAssociatedVmId(vmId);
    entity.setAssociatedPrivateIp(privateIp);
    entity.setStatus(FloatingIpStatus.ASSOCIATED);
    return floatingIpRepository.save(entity);
  }

  @Transactional
  public FloatingIpEntity disassociateFloatingIp(UUID floatingIpId) {
    FloatingIpEntity entity = getFloatingIp(floatingIpId);
    entity.setAssociatedVmId(null);
    entity.setAssociatedPrivateIp(null);
    entity.setStatus(FloatingIpStatus.AVAILABLE);
    return floatingIpRepository.save(entity);
  }

  @Transactional
  public void releaseFloatingIp(UUID floatingIpId) {
    FloatingIpEntity entity = getFloatingIp(floatingIpId);
    entity.setStatus(FloatingIpStatus.RELEASING);
    floatingIpRepository.save(entity);
    poolsService.decrementAllocated(entity.getPublicIpPool().getId());
    floatingIpRepository.deleteById(floatingIpId);
  }

  private String generateIpFromPool(PublicIpPoolEntity pool) {
    // In production, this would allocate a specific IP from the pool CIDR
    // For now, return a placeholder based on the pool's allocated count
    return pool.getCidr().split("/")[0].replace(".0", "." + (pool.getAllocatedIps() + 1));
  }
}
