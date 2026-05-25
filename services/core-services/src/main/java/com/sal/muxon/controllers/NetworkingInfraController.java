package com.sal.muxon.controllers;

import com.sal.muxon.db.model.DatacenterNetworkCapabilitiesEntity;
import com.sal.muxon.db.model.FabricNetworkEntity;
import com.sal.muxon.db.model.FabricNetworkEntity.FabricNetworkRole;
import com.sal.muxon.db.model.FabricNetworkEntity.FabricNetworkType;
import com.sal.muxon.db.model.NetworkEdgeNodeEntity;
import com.sal.muxon.db.model.NetworkEdgeNodeEntity.NetworkEdgeNodeType;
import com.sal.muxon.db.model.PublicIpPoolEntity;
import com.sal.muxon.services.DatacenterNetworkCapabilitiesService;
import com.sal.muxon.services.FabricNetworksService;
import com.sal.muxon.services.NetworkEdgeNodesService;
import com.sal.muxon.services.PublicIpPoolsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin-only controller for datacenter networking infrastructure:
 * fabric networks, public IP pools, network edge nodes, and capabilities.
 */
@RestController
@RequestMapping("/api/v1/datacenters/{datacenterId}")
public class NetworkingInfraController {

    @Autowired
    private FabricNetworksService fabricNetworksService;

    @Autowired
    private PublicIpPoolsService publicIpPoolsService;

    @Autowired
    private NetworkEdgeNodesService edgeNodesService;

    @Autowired
    private DatacenterNetworkCapabilitiesService capabilitiesService;

    // ─── Fabric Networks ─────────────────────────────────────────────────────

    @GetMapping("/fabric-networks")
    public ResponseEntity<List<FabricNetworkEntity>> listFabricNetworks(@PathVariable UUID datacenterId) {
        return ResponseEntity.ok(fabricNetworksService.listByDatacenter(datacenterId));
    }

    @PostMapping("/fabric-networks")
    public ResponseEntity<FabricNetworkEntity> createFabricNetwork(
            @PathVariable UUID datacenterId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        FabricNetworkType type = FabricNetworkType.valueOf((String) body.get("type"));
        FabricNetworkRole role = FabricNetworkRole.valueOf((String) body.get("role"));
        String externalId = (String) body.get("externalId");
        Integer vlanId = body.get("vlanId") != null ? ((Number) body.get("vlanId")).intValue() : null;
        return ResponseEntity.status(201).body(
                fabricNetworksService.create(datacenterId, name, type, role, externalId, vlanId, null));
    }

    @GetMapping("/fabric-networks/{fabricNetworkId}")
    public ResponseEntity<FabricNetworkEntity> getFabricNetwork(
            @PathVariable UUID datacenterId,
            @PathVariable UUID fabricNetworkId) {
        return ResponseEntity.ok(fabricNetworksService.getById(fabricNetworkId));
    }

    @DeleteMapping("/fabric-networks/{fabricNetworkId}")
    public ResponseEntity<Void> deleteFabricNetwork(
            @PathVariable UUID datacenterId,
            @PathVariable UUID fabricNetworkId) {
        fabricNetworksService.delete(fabricNetworkId);
        return ResponseEntity.noContent().build();
    }

    // ─── Public IP Pools ─────────────────────────────────────────────────────

    @GetMapping("/public-ip-pools")
    public ResponseEntity<List<PublicIpPoolEntity>> listPublicIpPools(@PathVariable UUID datacenterId) {
        return ResponseEntity.ok(publicIpPoolsService.listByDatacenter(datacenterId));
    }

    @PostMapping("/public-ip-pools")
    public ResponseEntity<PublicIpPoolEntity> createPublicIpPool(
            @PathVariable UUID datacenterId,
            @RequestBody Map<String, Object> body) {
        String cidr = (String) body.get("cidr");
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        return ResponseEntity.status(201).body(
                publicIpPoolsService.create(datacenterId, cidr, name, description));
    }

    @GetMapping("/public-ip-pools/{poolId}")
    public ResponseEntity<PublicIpPoolEntity> getPublicIpPool(
            @PathVariable UUID datacenterId,
            @PathVariable UUID poolId) {
        return ResponseEntity.ok(publicIpPoolsService.getById(poolId));
    }

    @DeleteMapping("/public-ip-pools/{poolId}")
    public ResponseEntity<Void> deletePublicIpPool(
            @PathVariable UUID datacenterId,
            @PathVariable UUID poolId) {
        publicIpPoolsService.delete(poolId);
        return ResponseEntity.noContent().build();
    }

    // ─── Network Edge Nodes ───────────────────────────────────────────────────

    @GetMapping("/network-edge-nodes")
    public ResponseEntity<List<NetworkEdgeNodeEntity>> listEdgeNodes(@PathVariable UUID datacenterId) {
        return ResponseEntity.ok(edgeNodesService.listByDatacenter(datacenterId));
    }

    @PostMapping("/network-edge-nodes")
    public ResponseEntity<NetworkEdgeNodeEntity> createEdgeNode(
            @PathVariable UUID datacenterId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String host = (String) body.get("host");
        NetworkEdgeNodeType type = NetworkEdgeNodeType.valueOf((String) body.get("type"));
        return ResponseEntity.status(201).body(
                edgeNodesService.register(datacenterId, name, host, type, null));
    }

    @GetMapping("/network-edge-nodes/{nodeId}")
    public ResponseEntity<NetworkEdgeNodeEntity> getEdgeNode(
            @PathVariable UUID datacenterId,
            @PathVariable UUID nodeId) {
        return ResponseEntity.ok(edgeNodesService.getById(nodeId));
    }

    @PutMapping("/network-edge-nodes/{nodeId}")
    public ResponseEntity<NetworkEdgeNodeEntity> updateEdgeNode(
            @PathVariable UUID datacenterId,
            @PathVariable UUID nodeId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String host = (String) body.get("host");
        NetworkEdgeNodeType type = body.get("type") != null
                ? NetworkEdgeNodeType.valueOf((String) body.get("type")) : null;
        return ResponseEntity.ok(edgeNodesService.update(nodeId, name, host, type, null));
    }

    @DeleteMapping("/network-edge-nodes/{nodeId}")
    public ResponseEntity<Void> deactivateEdgeNode(
            @PathVariable UUID datacenterId,
            @PathVariable UUID nodeId) {
        edgeNodesService.deactivate(nodeId);
        return ResponseEntity.noContent().build();
    }

    // ─── Network Capabilities ─────────────────────────────────────────────────

    @GetMapping("/network-capabilities")
    public ResponseEntity<DatacenterNetworkCapabilitiesEntity> getCapabilities(
            @PathVariable UUID datacenterId) {
        return ResponseEntity.ok(capabilitiesService.getByDatacenterId(datacenterId));
    }

    @PutMapping("/network-capabilities")
    public ResponseEntity<DatacenterNetworkCapabilitiesEntity> updateCapabilities(
            @PathVariable UUID datacenterId,
            @RequestBody Map<String, Object> body) {
        Boolean pub = (Boolean) body.get("publicIpSupported");
        Boolean vpn = (Boolean) body.get("vpnSupported");
        Boolean bgp = (Boolean) body.get("bgpSupported");
        Boolean ha = (Boolean) body.get("haGatewaySupported");
        Boolean vxlan = (Boolean) body.get("vxlanSupported");
        Boolean mr = (Boolean) body.get("multiRegionSupported");
        Boolean l7 = (Boolean) body.get("l7LbSupported");
        Boolean ipv6 = (Boolean) body.get("ipv6Supported");
        Boolean dual = (Boolean) body.get("dualStackSupported");
        return ResponseEntity.ok(capabilitiesService.update(datacenterId, pub, vpn, bgp, ha, vxlan, mr, l7, ipv6, dual));
    }
}
