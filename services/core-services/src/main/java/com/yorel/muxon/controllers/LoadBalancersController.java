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

import com.yorel.muxon.db.model.LoadBalancerEntity;
import com.yorel.muxon.db.model.LoadBalancerEntity.LbScheme;
import com.yorel.muxon.services.LoadBalancersService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/load-balancers")
public class LoadBalancersController {

  @Autowired private LoadBalancersService loadBalancersService;

  @GetMapping
  public ResponseEntity<List<LoadBalancerEntity>> listLoadBalancers(
      @PathVariable UUID tenantId, @PathVariable UUID vpcId) {
    return ResponseEntity.ok(loadBalancersService.listByVpc(vpcId));
  }

  @PostMapping
  public ResponseEntity<LoadBalancerEntity> createLoadBalancer(
      @PathVariable UUID tenantId,
      @PathVariable UUID vpcId,
      @RequestBody Map<String, Object> body) {
    String name = (String) body.get("name");
    UUID subnetId = UUID.fromString((String) body.get("subnetId"));
    LbScheme scheme = LbScheme.valueOf((String) body.get("scheme"));
    UUID floatingIpId =
        body.get("floatingIpId") != null
            ? UUID.fromString((String) body.get("floatingIpId"))
            : null;
    return ResponseEntity.status(201)
        .body(loadBalancersService.create(vpcId, name, subnetId, scheme, floatingIpId));
  }

  @GetMapping("/{loadBalancerId}")
  public ResponseEntity<LoadBalancerEntity> getLoadBalancer(
      @PathVariable UUID tenantId, @PathVariable UUID vpcId, @PathVariable UUID loadBalancerId) {
    return ResponseEntity.ok(loadBalancersService.getById(loadBalancerId));
  }

  @DeleteMapping("/{loadBalancerId}")
  public ResponseEntity<Void> deleteLoadBalancer(
      @PathVariable UUID tenantId, @PathVariable UUID vpcId, @PathVariable UUID loadBalancerId) {
    loadBalancersService.delete(loadBalancerId);
    return ResponseEntity.noContent().build();
  }
}
