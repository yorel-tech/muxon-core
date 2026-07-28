package com.scal.muxon.controllers;

import com.scal.muxon.db.model.LoadBalancerEntity;
import com.scal.muxon.db.model.LoadBalancerEntity.LbScheme;
import com.scal.muxon.services.LoadBalancersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/load-balancers")
public class LoadBalancersController {

    @Autowired
    private LoadBalancersService loadBalancersService;

    @GetMapping
    public ResponseEntity<List<LoadBalancerEntity>> listLoadBalancers(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId) {
        return ResponseEntity.ok(loadBalancersService.listByVpc(vpcId));
    }

    @PostMapping
    public ResponseEntity<LoadBalancerEntity> createLoadBalancer(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        UUID subnetId = UUID.fromString((String) body.get("subnetId"));
        LbScheme scheme = LbScheme.valueOf((String) body.get("scheme"));
        UUID floatingIpId = body.get("floatingIpId") != null
                ? UUID.fromString((String) body.get("floatingIpId")) : null;
        return ResponseEntity.status(201).body(
                loadBalancersService.create(vpcId, name, subnetId, scheme, floatingIpId));
    }

    @GetMapping("/{loadBalancerId}")
    public ResponseEntity<LoadBalancerEntity> getLoadBalancer(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @PathVariable UUID loadBalancerId) {
        return ResponseEntity.ok(loadBalancersService.getById(loadBalancerId));
    }

    @DeleteMapping("/{loadBalancerId}")
    public ResponseEntity<Void> deleteLoadBalancer(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @PathVariable UUID loadBalancerId) {
        loadBalancersService.delete(loadBalancerId);
        return ResponseEntity.noContent().build();
    }
}
