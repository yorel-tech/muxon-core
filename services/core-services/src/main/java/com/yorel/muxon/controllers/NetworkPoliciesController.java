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

import com.yorel.muxon.db.model.TenantNetworkPolicyEntity;
import com.yorel.muxon.services.TenantNetworkPolicyService;
import com.yorel.muxon.services.TenantNetworkPolicyService.EffectiveCapabilities;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NetworkPoliciesController {

  @Autowired private TenantNetworkPolicyService policyService;

  @GetMapping("/api/v1/admin/tenants/{tenantId}/network-policy")
  public ResponseEntity<TenantNetworkPolicyEntity> getTenantNetworkPolicy(
      @PathVariable UUID tenantId) {
    return ResponseEntity.ok(policyService.getPolicy(tenantId));
  }

  @PutMapping("/api/v1/admin/tenants/{tenantId}/network-policy")
  public ResponseEntity<TenantNetworkPolicyEntity> updateTenantNetworkPolicy(
      @PathVariable UUID tenantId, @RequestBody Map<String, Object> body) {
    Integer maxVpcs =
        body.get("maxVpcs") != null ? ((Number) body.get("maxVpcs")).intValue() : null;
    Integer maxPublicIps =
        body.get("maxPublicIps") != null ? ((Number) body.get("maxPublicIps")).intValue() : null;
    Integer maxSubnets =
        body.get("maxSubnetsPerVpc") != null
            ? ((Number) body.get("maxSubnetsPerVpc")).intValue()
            : null;
    Boolean vpn = (Boolean) body.get("vpnAllowed");
    Boolean peering = (Boolean) body.get("peeringAllowed");
    Boolean ha = (Boolean) body.get("haNetworkingAllowed");
    Integer bw =
        body.get("bandwidthLimitMbps") != null
            ? ((Number) body.get("bandwidthLimitMbps")).intValue()
            : null;
    return ResponseEntity.ok(
        policyService.updatePolicy(
            tenantId, maxVpcs, maxPublicIps, maxSubnets, vpn, peering, ha, bw));
  }

  @GetMapping("/api/v1/tenants/{tenantId}/datacenters/{datacenterId}/network-capabilities")
  public ResponseEntity<EffectiveCapabilities> getEffectiveCapabilities(
      @PathVariable UUID tenantId, @PathVariable UUID datacenterId) {
    return ResponseEntity.ok(policyService.resolveEffectiveCapabilities(tenantId, datacenterId));
  }
}
