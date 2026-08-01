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

import com.yorel.muxon.db.model.RouteTableEntity;
import com.yorel.muxon.db.model.RouteTableEntryEntity;
import com.yorel.muxon.db.model.RouteTableEntryEntity.RouteTargetType;
import com.yorel.muxon.services.RouteTablesService;
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
@RequestMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/route-tables")
public class RouteTablesController {

  @Autowired private RouteTablesService routeTablesService;

  @GetMapping
  public ResponseEntity<List<RouteTableEntity>> listRouteTables(
      @PathVariable UUID tenantId, @PathVariable UUID vpcId) {
    return ResponseEntity.ok(routeTablesService.listByVpc(vpcId));
  }

  @PostMapping
  public ResponseEntity<RouteTableEntity> createRouteTable(
      @PathVariable UUID tenantId,
      @PathVariable UUID vpcId,
      @RequestBody Map<String, Object> body) {
    String name = (String) body.get("name");
    return ResponseEntity.status(201).body(routeTablesService.create(vpcId, name));
  }

  @GetMapping("/{routeTableId}")
  public ResponseEntity<RouteTableEntity> getRouteTable(
      @PathVariable UUID tenantId, @PathVariable UUID vpcId, @PathVariable UUID routeTableId) {
    return ResponseEntity.ok(routeTablesService.getById(routeTableId));
  }

  @DeleteMapping("/{routeTableId}")
  public ResponseEntity<Void> deleteRouteTable(
      @PathVariable UUID tenantId, @PathVariable UUID vpcId, @PathVariable UUID routeTableId) {
    routeTablesService.delete(routeTableId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{routeTableId}/entries")
  public ResponseEntity<List<RouteTableEntryEntity>> listEntries(
      @PathVariable UUID tenantId, @PathVariable UUID vpcId, @PathVariable UUID routeTableId) {
    return ResponseEntity.ok(routeTablesService.listEntries(routeTableId));
  }

  @PostMapping("/{routeTableId}/entries")
  public ResponseEntity<RouteTableEntryEntity> addEntry(
      @PathVariable UUID tenantId,
      @PathVariable UUID vpcId,
      @PathVariable UUID routeTableId,
      @RequestBody Map<String, Object> body) {
    String destinationCidr = (String) body.get("destinationCidr");
    RouteTargetType targetType = RouteTargetType.valueOf((String) body.get("targetType"));
    UUID targetId =
        body.get("targetId") != null ? UUID.fromString((String) body.get("targetId")) : null;
    return ResponseEntity.status(201)
        .body(routeTablesService.addEntry(routeTableId, destinationCidr, targetType, targetId));
  }

  @DeleteMapping("/{routeTableId}/entries/{entryId}")
  public ResponseEntity<Void> deleteEntry(
      @PathVariable UUID tenantId,
      @PathVariable UUID vpcId,
      @PathVariable UUID routeTableId,
      @PathVariable UUID entryId) {
    routeTablesService.deleteEntry(entryId);
    return ResponseEntity.noContent().build();
  }
}
