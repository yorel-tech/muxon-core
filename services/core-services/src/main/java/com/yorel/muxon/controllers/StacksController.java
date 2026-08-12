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

import com.yorel.muxon.db.model.StackEntity;
import com.yorel.muxon.services.StackService;
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
@RequestMapping("/api/v1/tenants/{tenantId}/datacenters/{datacenterId}/stacks")
public class StacksController {

  @Autowired private StackService stackService;

  @GetMapping
  public ResponseEntity<List<StackEntity>> listStacks(
      @PathVariable UUID tenantId, @PathVariable UUID datacenterId) {
    return ResponseEntity.ok(stackService.listByGrant(tenantId, datacenterId));
  }

  @PostMapping
  public ResponseEntity<StackEntity> createStack(
      @PathVariable UUID tenantId,
      @PathVariable UUID datacenterId,
      @RequestBody Map<String, Object> body) {
    String name = (String) body.get("name");
    String description = (String) body.get("description");
    @SuppressWarnings("unchecked")
    Map<String, String> metadata = (Map<String, String>) body.get("metadata");
    StackEntity stack = stackService.create(tenantId, datacenterId, name, description, metadata);
    return ResponseEntity.status(201).body(stack);
  }

  @GetMapping("/{stackId}")
  public ResponseEntity<StackEntity> getStack(
      @PathVariable UUID tenantId, @PathVariable UUID datacenterId, @PathVariable UUID stackId) {
    return ResponseEntity.ok(stackService.getByIdAndTenant(stackId, tenantId));
  }

  @PutMapping("/{stackId}")
  public ResponseEntity<StackEntity> updateStack(
      @PathVariable UUID tenantId,
      @PathVariable UUID datacenterId,
      @PathVariable UUID stackId,
      @RequestBody Map<String, Object> body) {
    String name = (String) body.get("name");
    String description = (String) body.get("description");
    @SuppressWarnings("unchecked")
    Map<String, String> metadata = (Map<String, String>) body.get("metadata");
    return ResponseEntity.ok(stackService.update(stackId, name, description, metadata));
  }

  @DeleteMapping("/{stackId}")
  public ResponseEntity<Void> deleteStack(
      @PathVariable UUID tenantId, @PathVariable UUID datacenterId, @PathVariable UUID stackId) {
    stackService.delete(stackId);
    return ResponseEntity.noContent().build();
  }
}
