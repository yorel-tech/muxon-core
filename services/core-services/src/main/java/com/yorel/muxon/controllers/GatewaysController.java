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

import com.yorel.muxon.db.model.FloatingIpEntity;
import com.yorel.muxon.db.model.InternetGatewayEntity;
import com.yorel.muxon.db.model.NatGatewayEntity;
import com.yorel.muxon.services.GatewaysService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewaysController {

  @Autowired private GatewaysService gatewaysService;

  // ─── Internet Gateways ─────────────────────────────────────────────────────

  @GetMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/internet-gateway")
  public ResponseEntity<InternetGatewayEntity> getInternetGateway(
      @PathVariable UUID tenantId, @PathVariable UUID vpcId) {
    return gatewaysService
        .getInternetGateway(vpcId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/internet-gateway")
  public ResponseEntity<InternetGatewayEntity> createInternetGateway(
      @PathVariable UUID tenantId,
      @PathVariable UUID vpcId,
      @RequestBody Map<String, Object> body) {
    String name = (String) body.get("name");
    return ResponseEntity.status(201).body(gatewaysService.createInternetGateway(vpcId, name));
  }

  @DeleteMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/internet-gateway")
  public ResponseEntity<Void> deleteInternetGateway(
      @PathVariable UUID tenantId, @PathVariable UUID vpcId) {
    gatewaysService.deleteInternetGateway(vpcId);
    return ResponseEntity.noContent().build();
  }

  // ─── NAT Gateways ──────────────────────────────────────────────────────────

  @GetMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/nat-gateways")
  public ResponseEntity<List<NatGatewayEntity>> listNatGateways(
      @PathVariable UUID tenantId, @PathVariable UUID vpcId) {
    return ResponseEntity.ok(gatewaysService.listNatGateways(vpcId));
  }

  @PostMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/nat-gateways")
  public ResponseEntity<NatGatewayEntity> createNatGateway(
      @PathVariable UUID tenantId,
      @PathVariable UUID vpcId,
      @RequestBody Map<String, Object> body) {
    String name = (String) body.get("name");
    UUID subnetId = UUID.fromString((String) body.get("subnetId"));
    UUID floatingIpId = UUID.fromString((String) body.get("floatingIpId"));
    return ResponseEntity.status(201)
        .body(gatewaysService.createNatGateway(vpcId, name, subnetId, floatingIpId));
  }

  @GetMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/nat-gateways/{natGatewayId}")
  public ResponseEntity<NatGatewayEntity> getNatGateway(
      @PathVariable UUID tenantId, @PathVariable UUID vpcId, @PathVariable UUID natGatewayId) {
    return ResponseEntity.ok(gatewaysService.getNatGateway(natGatewayId));
  }

  @DeleteMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/nat-gateways/{natGatewayId}")
  public ResponseEntity<Void> deleteNatGateway(
      @PathVariable UUID tenantId, @PathVariable UUID vpcId, @PathVariable UUID natGatewayId) {
    gatewaysService.deleteNatGateway(natGatewayId);
    return ResponseEntity.noContent().build();
  }

  // ─── Floating IPs ──────────────────────────────────────────────────────────

  @GetMapping("/api/v1/tenants/{tenantId}/datacenters/{datacenterId}/floating-ips")
  public ResponseEntity<List<FloatingIpEntity>> listFloatingIps(
      @PathVariable UUID tenantId, @PathVariable UUID datacenterId) {
    return ResponseEntity.ok(gatewaysService.listFloatingIps(tenantId, datacenterId));
  }

  @PostMapping("/api/v1/tenants/{tenantId}/datacenters/{datacenterId}/floating-ips")
  public ResponseEntity<FloatingIpEntity> allocateFloatingIp(
      @PathVariable UUID tenantId, @PathVariable UUID datacenterId) {
    return ResponseEntity.status(201)
        .body(gatewaysService.allocateFloatingIp(tenantId, datacenterId));
  }

  @GetMapping("/api/v1/tenants/{tenantId}/datacenters/{datacenterId}/floating-ips/{floatingIpId}")
  public ResponseEntity<FloatingIpEntity> getFloatingIp(
      @PathVariable UUID tenantId,
      @PathVariable UUID datacenterId,
      @PathVariable UUID floatingIpId) {
    return ResponseEntity.ok(gatewaysService.getFloatingIp(floatingIpId));
  }

  @DeleteMapping(
      "/api/v1/tenants/{tenantId}/datacenters/{datacenterId}/floating-ips/{floatingIpId}")
  public ResponseEntity<Void> releaseFloatingIp(
      @PathVariable UUID tenantId,
      @PathVariable UUID datacenterId,
      @PathVariable UUID floatingIpId) {
    gatewaysService.releaseFloatingIp(floatingIpId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping(
      "/api/v1/tenants/{tenantId}/datacenters/{datacenterId}/floating-ips/{floatingIpId}/associate")
  public ResponseEntity<FloatingIpEntity> associateFloatingIp(
      @PathVariable UUID tenantId,
      @PathVariable UUID datacenterId,
      @PathVariable UUID floatingIpId,
      @RequestBody Map<String, Object> body) {
    UUID vmId = UUID.fromString((String) body.get("vmId"));
    String privateIp = (String) body.get("privateIp");
    return ResponseEntity.ok(gatewaysService.associateFloatingIp(floatingIpId, vmId, privateIp));
  }

  @PostMapping(
      "/api/v1/tenants/{tenantId}/datacenters/{datacenterId}/floating-ips/{floatingIpId}/disassociate")
  public ResponseEntity<FloatingIpEntity> disassociateFloatingIp(
      @PathVariable UUID tenantId,
      @PathVariable UUID datacenterId,
      @PathVariable UUID floatingIpId) {
    return ResponseEntity.ok(gatewaysService.disassociateFloatingIp(floatingIpId));
  }
}
