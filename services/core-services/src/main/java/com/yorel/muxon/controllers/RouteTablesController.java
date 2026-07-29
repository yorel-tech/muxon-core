package com.yorel.muxon.controllers;

import com.yorel.muxon.db.model.RouteTableEntity;
import com.yorel.muxon.db.model.RouteTableEntryEntity;
import com.yorel.muxon.db.model.RouteTableEntryEntity.RouteTargetType;
import com.yorel.muxon.services.RouteTablesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/route-tables")
public class RouteTablesController {

    @Autowired
    private RouteTablesService routeTablesService;

    @GetMapping
    public ResponseEntity<List<RouteTableEntity>> listRouteTables(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId) {
        return ResponseEntity.ok(routeTablesService.listByVpc(vpcId));
    }

    @PostMapping
    public ResponseEntity<RouteTableEntity> createRouteTable(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        return ResponseEntity.status(201).body(routeTablesService.create(vpcId, name));
    }

    @GetMapping("/{routeTableId}")
    public ResponseEntity<RouteTableEntity> getRouteTable(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @PathVariable UUID routeTableId) {
        return ResponseEntity.ok(routeTablesService.getById(routeTableId));
    }

    @DeleteMapping("/{routeTableId}")
    public ResponseEntity<Void> deleteRouteTable(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @PathVariable UUID routeTableId) {
        routeTablesService.delete(routeTableId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{routeTableId}/entries")
    public ResponseEntity<List<RouteTableEntryEntity>> listEntries(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @PathVariable UUID routeTableId) {
        return ResponseEntity.ok(routeTablesService.listEntries(routeTableId));
    }

    @PostMapping("/{routeTableId}/entries")
    public ResponseEntity<RouteTableEntryEntity> addEntry(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @PathVariable UUID routeTableId,
            @RequestBody Map<String, Object> body) {
        String destinationCidr = (String) body.get("destinationCidr");
        RouteTargetType targetType = RouteTargetType.valueOf((String) body.get("targetType"));
        UUID targetId = body.get("targetId") != null ? UUID.fromString((String) body.get("targetId")) : null;
        return ResponseEntity.status(201).body(
                routeTablesService.addEntry(routeTableId, destinationCidr, targetType, targetId));
    }

    @DeleteMapping("/{routeTableId}/entries/{entryId}")
    public ResponseEntity<Void> deleteEntry(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @PathVariable UUID routeTableId, @PathVariable UUID entryId) {
        routeTablesService.deleteEntry(entryId);
        return ResponseEntity.noContent().build();
    }
}
