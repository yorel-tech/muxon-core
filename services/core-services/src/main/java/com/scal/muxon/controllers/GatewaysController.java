package com.scal.muxon.controllers;

import com.scal.muxon.db.model.FloatingIpEntity;
import com.scal.muxon.db.model.InternetGatewayEntity;
import com.scal.muxon.db.model.NatGatewayEntity;
import com.scal.muxon.services.GatewaysService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class GatewaysController {

    @Autowired
    private GatewaysService gatewaysService;

    // ─── Internet Gateways ─────────────────────────────────────────────────────

    @GetMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/internet-gateway")
    public ResponseEntity<InternetGatewayEntity> getInternetGateway(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId) {
        return gatewaysService.getInternetGateway(vpcId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/internet-gateway")
    public ResponseEntity<InternetGatewayEntity> createInternetGateway(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        return ResponseEntity.status(201).body(gatewaysService.createInternetGateway(vpcId, name));
    }

    @DeleteMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/internet-gateway")
    public ResponseEntity<Void> deleteInternetGateway(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId) {
        gatewaysService.deleteInternetGateway(vpcId);
        return ResponseEntity.noContent().build();
    }

    // ─── NAT Gateways ──────────────────────────────────────────────────────────

    @GetMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/nat-gateways")
    public ResponseEntity<List<NatGatewayEntity>> listNatGateways(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId) {
        return ResponseEntity.ok(gatewaysService.listNatGateways(vpcId));
    }

    @PostMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/nat-gateways")
    public ResponseEntity<NatGatewayEntity> createNatGateway(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        UUID subnetId = UUID.fromString((String) body.get("subnetId"));
        UUID floatingIpId = UUID.fromString((String) body.get("floatingIpId"));
        return ResponseEntity.status(201).body(gatewaysService.createNatGateway(vpcId, name, subnetId, floatingIpId));
    }

    @GetMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/nat-gateways/{natGatewayId}")
    public ResponseEntity<NatGatewayEntity> getNatGateway(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @PathVariable UUID natGatewayId) {
        return ResponseEntity.ok(gatewaysService.getNatGateway(natGatewayId));
    }

    @DeleteMapping("/api/v1/tenants/{tenantId}/vpcs/{vpcId}/nat-gateways/{natGatewayId}")
    public ResponseEntity<Void> deleteNatGateway(
            @PathVariable UUID tenantId, @PathVariable UUID vpcId,
            @PathVariable UUID natGatewayId) {
        gatewaysService.deleteNatGateway(natGatewayId);
        return ResponseEntity.noContent().build();
    }

    // ─── Floating IPs ──────────────────────────────────────────────────────────

    @GetMapping("/api/v1/tenants/{tenantId}/datacenters/{datacenterId}/floating-ips")
    public ResponseEntity<List<FloatingIpEntity>> listFloatingIps(
            @PathVariable UUID tenantId, @PathVariable UUID datacenterId) {
        return ResponseEntity.ok(gatewaysService.listFloatingIps(tenantId, datacenterId));
    }

    @PostMapping("/api/v1/tenants/{tenantId}/datacenters/{datacenterId}/floating-ips")
    public ResponseEntity<FloatingIpEntity> allocateFloatingIp(
            @PathVariable UUID tenantId, @PathVariable UUID datacenterId) {
        return ResponseEntity.status(201).body(gatewaysService.allocateFloatingIp(tenantId, datacenterId));
    }

    @GetMapping("/api/v1/tenants/{tenantId}/datacenters/{datacenterId}/floating-ips/{floatingIpId}")
    public ResponseEntity<FloatingIpEntity> getFloatingIp(
            @PathVariable UUID tenantId, @PathVariable UUID datacenterId,
            @PathVariable UUID floatingIpId) {
        return ResponseEntity.ok(gatewaysService.getFloatingIp(floatingIpId));
    }

    @DeleteMapping("/api/v1/tenants/{tenantId}/datacenters/{datacenterId}/floating-ips/{floatingIpId}")
    public ResponseEntity<Void> releaseFloatingIp(
            @PathVariable UUID tenantId, @PathVariable UUID datacenterId,
            @PathVariable UUID floatingIpId) {
        gatewaysService.releaseFloatingIp(floatingIpId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/tenants/{tenantId}/datacenters/{datacenterId}/floating-ips/{floatingIpId}/associate")
    public ResponseEntity<FloatingIpEntity> associateFloatingIp(
            @PathVariable UUID tenantId, @PathVariable UUID datacenterId,
            @PathVariable UUID floatingIpId,
            @RequestBody Map<String, Object> body) {
        UUID vmId = UUID.fromString((String) body.get("vmId"));
        String privateIp = (String) body.get("privateIp");
        return ResponseEntity.ok(gatewaysService.associateFloatingIp(floatingIpId, vmId, privateIp));
    }

    @PostMapping("/api/v1/tenants/{tenantId}/datacenters/{datacenterId}/floating-ips/{floatingIpId}/disassociate")
    public ResponseEntity<FloatingIpEntity> disassociateFloatingIp(
            @PathVariable UUID tenantId, @PathVariable UUID datacenterId,
            @PathVariable UUID floatingIpId) {
        return ResponseEntity.ok(gatewaysService.disassociateFloatingIp(floatingIpId));
    }
}
