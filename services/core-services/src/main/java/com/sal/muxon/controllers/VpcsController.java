package com.sal.muxon.controllers;

import com.sal.muxon.db.model.VpcEntity;
import com.sal.muxon.services.VpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/vpcs")
public class VpcsController {

    @Autowired
    private VpcService vpcService;

    @GetMapping
    public ResponseEntity<List<VpcEntity>> listVpcs(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(vpcService.listByTenant(tenantId));
    }

    @PostMapping
    public ResponseEntity<VpcEntity> createVpc(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String cidr = (String) body.get("cidr");
        String description = (String) body.get("description");
        @SuppressWarnings("unchecked")
        Map<String, String> metadata = (Map<String, String>) body.get("metadata");
        return ResponseEntity.status(201).body(vpcService.create(tenantId, name, cidr, description, metadata));
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
