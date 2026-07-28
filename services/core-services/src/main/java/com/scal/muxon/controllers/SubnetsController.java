package com.scal.muxon.controllers;

import com.scal.muxon.db.model.SubnetEntity;
import com.scal.muxon.db.model.SubnetRbacEntity;
import com.scal.muxon.db.model.SubnetRbacEntity.SubnetPrincipalType;
import com.scal.muxon.services.SubnetRbacService;
import com.scal.muxon.services.SubnetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}")
public class SubnetsController {

    @Autowired
    private SubnetService subnetService;

    @Autowired
    private SubnetRbacService subnetRbacService;

    @GetMapping("/subnets")
    public ResponseEntity<List<SubnetEntity>> listSubnets(
            @PathVariable UUID tenantId,
            @PathVariable UUID vpcId,
            @RequestParam(required = false) UUID stack) {
        List<SubnetEntity> subnets = stack != null
                ? subnetService.listByVpcForStack(vpcId, stack)
                : subnetService.listByVpc(vpcId);
        return ResponseEntity.ok(subnets);
    }

    @PostMapping("/subnets")
    public ResponseEntity<SubnetEntity> createSubnet(
            @PathVariable UUID tenantId,
            @PathVariable UUID vpcId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String cidr = (String) body.get("cidr");
        UUID datacenterId = UUID.fromString((String) body.get("datacenterId"));
        String gatewayIp = (String) body.get("gatewayIp");
        @SuppressWarnings("unchecked")
        List<String> dnsServers = (List<String>) body.get("dnsServers");
        boolean isPublic = Boolean.TRUE.equals(body.get("public"));
        String availabilityZone = (String) body.get("availabilityZone");
        SubnetEntity subnet = subnetService.create(tenantId, vpcId, name, cidr, datacenterId,
                gatewayIp, dnsServers, isPublic, availabilityZone);
        return ResponseEntity.status(201).body(subnet);
    }

    @GetMapping("/subnets/{subnetId}")
    public ResponseEntity<SubnetEntity> getSubnet(
            @PathVariable UUID tenantId,
            @PathVariable UUID vpcId,
            @PathVariable UUID subnetId) {
        return ResponseEntity.ok(subnetService.getById(subnetId));
    }

    @DeleteMapping("/subnets/{subnetId}")
    public ResponseEntity<Void> deleteSubnet(
            @PathVariable UUID tenantId,
            @PathVariable UUID vpcId,
            @PathVariable UUID subnetId) {
        subnetService.delete(subnetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subnets/{subnetId}/access")
    public ResponseEntity<List<SubnetRbacEntity>> listSubnetRbac(
            @PathVariable UUID tenantId,
            @PathVariable UUID vpcId,
            @PathVariable UUID subnetId) {
        return ResponseEntity.ok(subnetRbacService.listBySubnet(subnetId));
    }

    @PostMapping("/subnets/{subnetId}/access")
    public ResponseEntity<SubnetRbacEntity> createSubnetRbac(
            @PathVariable UUID tenantId,
            @PathVariable UUID vpcId,
            @PathVariable UUID subnetId,
            @RequestBody Map<String, Object> body) {
        SubnetPrincipalType principalType = SubnetPrincipalType.valueOf((String) body.get("principalType"));
        UUID principalId = UUID.fromString((String) body.get("principalId"));
        @SuppressWarnings("unchecked")
        List<String> permissions = (List<String>) body.get("permissions");
        return ResponseEntity.status(201).body(
                subnetRbacService.create(subnetId, principalType, principalId, permissions));
    }

    @DeleteMapping("/subnets/{subnetId}/access/{rbacId}")
    public ResponseEntity<Void> deleteSubnetRbac(
            @PathVariable UUID tenantId,
            @PathVariable UUID vpcId,
            @PathVariable UUID subnetId,
            @PathVariable UUID rbacId) {
        subnetRbacService.delete(rbacId);
        return ResponseEntity.noContent().build();
    }
}
