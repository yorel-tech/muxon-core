package com.scal.muxon.controllers;

import com.scal.muxon.db.model.IpamPrefixDelegationEntity;
import com.scal.muxon.services.IpamPrefixDelegationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/subnets/{subnetId}/prefix-delegations")
public class IpamPrefixDelegationController {

    @Autowired
    private IpamPrefixDelegationService delegationService;

    @GetMapping
    public ResponseEntity<List<IpamPrefixDelegationEntity>> listDelegations(
            @PathVariable UUID tenantId,
            @PathVariable UUID vpcId,
            @PathVariable UUID subnetId) {
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
        return ResponseEntity.status(201).body(
                delegationService.create(subnetId, stackId, nodeVmId, delegatedCidr));
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
