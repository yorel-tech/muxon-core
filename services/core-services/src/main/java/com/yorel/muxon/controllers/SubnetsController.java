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

import com.yorel.muxon.db.model.SubnetEntity;
import com.yorel.muxon.db.model.SubnetRbacEntity;
import com.yorel.muxon.db.model.SubnetRbacEntity.SubnetPrincipalType;
import com.yorel.muxon.services.SubnetRbacService;
import com.yorel.muxon.services.SubnetService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}")
public class SubnetsController {

  @Autowired private SubnetService subnetService;

  @Autowired private SubnetRbacService subnetRbacService;

  @GetMapping("/subnets")
  public ResponseEntity<List<SubnetEntity>> listSubnets(
      @PathVariable UUID tenantId,
      @PathVariable UUID vpcId,
      @RequestParam(required = false) UUID stack) {
    List<SubnetEntity> subnets =
        stack != null
            ? subnetService.listByVpcForStack(vpcId, stack)
            : subnetService.listByVpc(vpcId);
    return ResponseEntity.ok(subnets);
  }

  @PostMapping("/subnets")
  public ResponseEntity<SubnetEntity> createSubnet(
      @PathVariable UUID tenantId,
      @PathVariable UUID vpcId,
      @RequestBody Map<String, Object> body) {
    String name = (String) body.get("name");
    String cidr = (String) body.get("cidr");
    UUID datacenterId = UUID.fromString((String) body.get("datacenterId"));
    String gatewayIp = (String) body.get("gatewayIp");
    @SuppressWarnings("unchecked")
    List<String> dnsServers = (List<String>) body.get("dnsServers");
    boolean isPublic = Boolean.TRUE.equals(body.get("public"));
    String availabilityZone = (String) body.get("availabilityZone");
    SubnetEntity subnet =
        subnetService.create(
            tenantId,
            vpcId,
            name,
            cidr,
            datacenterId,
            gatewayIp,
            dnsServers,
            isPublic,
            availabilityZone);
    return ResponseEntity.status(201).body(subnet);
  }

  @GetMapping("/subnets/{subnetId}")
  public ResponseEntity<SubnetEntity> getSubnet(
      @PathVariable UUID tenantId, @PathVariable UUID vpcId, @PathVariable UUID subnetId) {
    return ResponseEntity.ok(subnetService.getById(subnetId));
  }

  @DeleteMapping("/subnets/{subnetId}")
  public ResponseEntity<Void> deleteSubnet(
      @PathVariable UUID tenantId, @PathVariable UUID vpcId, @PathVariable UUID subnetId) {
    subnetService.delete(subnetId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/subnets/{subnetId}/access")
  public ResponseEntity<List<SubnetRbacEntity>> listSubnetRbac(
      @PathVariable UUID tenantId, @PathVariable UUID vpcId, @PathVariable UUID subnetId) {
    return ResponseEntity.ok(subnetRbacService.listBySubnet(subnetId));
  }

  @PostMapping("/subnets/{subnetId}/access")
  public ResponseEntity<SubnetRbacEntity> createSubnetRbac(
      @PathVariable UUID tenantId,
      @PathVariable UUID vpcId,
      @PathVariable UUID subnetId,
      @RequestBody Map<String, Object> body) {
    SubnetPrincipalType principalType =
        SubnetPrincipalType.valueOf((String) body.get("principalType"));
    UUID principalId = UUID.fromString((String) body.get("principalId"));
    @SuppressWarnings("unchecked")
    List<String> permissions = (List<String>) body.get("permissions");
    return ResponseEntity.status(201)
        .body(subnetRbacService.create(subnetId, principalType, principalId, permissions));
  }

  @DeleteMapping("/subnets/{subnetId}/access/{rbacId}")
  public ResponseEntity<Void> deleteSubnetRbac(
      @PathVariable UUID tenantId,
      @PathVariable UUID vpcId,
      @PathVariable UUID subnetId,
      @PathVariable UUID rbacId) {
    subnetRbacService.delete(rbacId);
    return ResponseEntity.noContent().build();
  }
}
