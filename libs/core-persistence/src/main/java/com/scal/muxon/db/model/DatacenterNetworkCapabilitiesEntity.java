package com.scal.muxon.db.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "datacenter_network_capabilities")
public class DatacenterNetworkCapabilitiesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id", nullable = false, unique = true)
    private DatacenterEntity datacenter;

    @Column(name = "public_ip_supported", nullable = false)
    private boolean publicIpSupported = false;

    @Column(name = "vpn_supported", nullable = false)
    private boolean vpnSupported = false;

    @Column(name = "bgp_supported", nullable = false)
    private boolean bgpSupported = false;

    @Column(name = "ha_gateway_supported", nullable = false)
    private boolean haGatewaySupported = false;

    @Column(name = "vxlan_supported", nullable = false)
    private boolean vxlanSupported = false;

    @Column(name = "multi_region_supported", nullable = false)
    private boolean multiRegionSupported = false;

    @Column(name = "l7_lb_supported", nullable = false)
    private boolean l7LbSupported = false;

    @Column(name = "ipv6_supported", nullable = false)
    private boolean ipv6Supported = false;

    @Column(name = "dual_stack_supported", nullable = false)
    private boolean dualStackSupported = false;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DatacenterNetworkCapabilitiesEntity() {}

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public DatacenterEntity getDatacenter() { return datacenter; }
    public void setDatacenter(DatacenterEntity datacenter) { this.datacenter = datacenter; }

    public boolean isPublicIpSupported() { return publicIpSupported; }
    public void setPublicIpSupported(boolean publicIpSupported) { this.publicIpSupported = publicIpSupported; }

    public boolean isVpnSupported() { return vpnSupported; }
    public void setVpnSupported(boolean vpnSupported) { this.vpnSupported = vpnSupported; }

    public boolean isBgpSupported() { return bgpSupported; }
    public void setBgpSupported(boolean bgpSupported) { this.bgpSupported = bgpSupported; }

    public boolean isHaGatewaySupported() { return haGatewaySupported; }
    public void setHaGatewaySupported(boolean haGatewaySupported) { this.haGatewaySupported = haGatewaySupported; }

    public boolean isVxlanSupported() { return vxlanSupported; }
    public void setVxlanSupported(boolean vxlanSupported) { this.vxlanSupported = vxlanSupported; }

    public boolean isMultiRegionSupported() { return multiRegionSupported; }
    public void setMultiRegionSupported(boolean multiRegionSupported) { this.multiRegionSupported = multiRegionSupported; }

    public boolean isL7LbSupported() { return l7LbSupported; }
    public void setL7LbSupported(boolean l7LbSupported) { this.l7LbSupported = l7LbSupported; }

    public boolean isIpv6Supported() { return ipv6Supported; }
    public void setIpv6Supported(boolean ipv6Supported) { this.ipv6Supported = ipv6Supported; }

    public boolean isDualStackSupported() { return dualStackSupported; }
    public void setDualStackSupported(boolean dualStackSupported) { this.dualStackSupported = dualStackSupported; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
