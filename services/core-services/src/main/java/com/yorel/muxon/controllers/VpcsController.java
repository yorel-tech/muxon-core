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

import com.yorel.muxon.db.model.VpcEntity;
import com.yorel.muxon.services.VpcService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/vpcs")
public class VpcsController {

  @Autowired private VpcService vpcService;

  @GetMapping
  public ResponseEntity<List<VpcEntity>> listVpcs(@PathVariable UUID tenantId) {
    return ResponseEntity.ok(vpcService.listByTenant(tenantId));
  }

  @PostMapping
  public ResponseEntity<VpcEntity> createVpc(
      @PathVariable UUID tenantId, @RequestBody Map<String, Object> body) {
    String name = (String) body.get("name");
    String cidr = (String) body.get("cidr");
    String description = (String) body.get("description");
    @SuppressWarnings("unchecked")
    Map<String, String> metadata = (Map<String, String>) body.get("metadata");
    return ResponseEntity.status(201)
        .body(vpcService.create(tenantId, name, cidr, description, metadata));
  }

  @GetMapping("/{vpcId}")
  public ResponseEntity<VpcEntity> getVpc(@PathVariable UUID tenantId, @PathVariable UUID vpcId) {
    return ResponseEntity.ok(vpcService.getByIdAndTenant(vpcId, tenantId));
  }

  @PutMapping("/{vpcId}")
  public ResponseEntity<VpcEntity> updateVpc(
      @PathVariable UUID tenantId,
      @PathVariable UUID vpcId,
      @RequestBody Map<String, Object> body) {
    String name = (String) body.get("name");
    String description = (String) body.get("description");
    return ResponseEntity.ok(vpcService.update(vpcId, name, description));
  }

  @DeleteMapping("/{vpcId}")
  public ResponseEntity<Void> deleteVpc(@PathVariable UUID tenantId, @PathVariable UUID vpcId) {
    vpcService.delete(vpcId);
    return ResponseEntity.noContent().build();
  }
}
