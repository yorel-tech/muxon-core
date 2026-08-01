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

import com.yorel.muxon.db.model.IpamPrefixDelegationEntity;
import com.yorel.muxon.services.IpamPrefixDelegationService;
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
@RequestMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/subnets/{subnetId}/prefix-delegations")
public class IpamPrefixDelegationController {

  @Autowired private IpamPrefixDelegationService delegationService;

  @GetMapping
  public ResponseEntity<List<IpamPrefixDelegationEntity>> listDelegations(
      @PathVariable UUID tenantId, @PathVariable UUID vpcId, @PathVariable UUID subnetId) {
    return ResponseEntity.ok(delegationService.listBySubnet(subnetId));
  }

  @PostMapping
  public ResponseEntity<IpamPrefixDelegationEntity> createDelegation(
      @PathVariable UUID tenantId,
      @PathVariable UUID vpcId,
      @PathVariable UUID subnetId,
      @RequestBody Map<String, Object> body) {
    UUID stackId = UUID.fromString((String) body.get("stackId"));
    UUID nodeVmId = UUID.fromString((String) body.get("nodeVmId"));
    String delegatedCidr = (String) body.get("delegatedCidr");
    return ResponseEntity.status(201)
        .body(delegationService.create(subnetId, stackId, nodeVmId, delegatedCidr));
  }

  @DeleteMapping("/{delegationId}")
  public ResponseEntity<Void> releaseDelegation(
      @PathVariable UUID tenantId,
      @PathVariable UUID vpcId,
      @PathVariable UUID subnetId,
      @PathVariable UUID delegationId) {
    delegationService.release(delegationId);
    return ResponseEntity.noContent().build();
  }
}
