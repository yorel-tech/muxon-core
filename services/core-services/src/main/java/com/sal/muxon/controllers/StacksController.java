package com.sal.muxon.controllers;

import com.sal.muxon.db.model.StackEntity;
import com.sal.muxon.services.StackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/datacenters/{datacenterId}/stacks")
public class StacksController {

    @Autowired
    private StackService stackService;

    @GetMapping
    public ResponseEntity<List<StackEntity>> listStacks(
            @PathVariable UUID tenantId,
            @PathVariable UUID datacenterId) {
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
            @PathVariable UUID tenantId,
            @PathVariable UUID datacenterId,
            @PathVariable UUID stackId) {
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
            @PathVariable UUID tenantId,
            @PathVariable UUID datacenterId,
            @PathVariable UUID stackId) {
        stackService.delete(stackId);
        return ResponseEntity.noContent().build();
    }
}
