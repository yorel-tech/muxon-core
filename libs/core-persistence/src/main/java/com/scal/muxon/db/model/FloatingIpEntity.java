package com.scal.muxon.db.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "floating_ip")
public class FloatingIpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id", nullable = false)
    private DatacenterEntity datacenter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "public_ip_pool_id", nullable = false)
    private PublicIpPoolEntity publicIpPool;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "ip_address_v6")
    private String ipAddressV6;

    @Column(name = "associated_vm_id")
    private UUID associatedVmId;

    @Column(name = "associated_private_ip")
    private String associatedPrivateIp;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private FloatingIpStatus status = FloatingIpStatus.AVAILABLE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FloatingIpEntity() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public TenantEntity getTenant() { return tenant; }
    public void setTenant(TenantEntity tenant) { this.tenant = tenant; }

    public DatacenterEntity getDatacenter() { return datacenter; }
    public void setDatacenter(DatacenterEntity datacenter) { this.datacenter = datacenter; }

    public PublicIpPoolEntity getPublicIpPool() { return publicIpPool; }
    public void setPublicIpPool(PublicIpPoolEntity publicIpPool) { this.publicIpPool = publicIpPool; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getIpAddressV6() { return ipAddressV6; }
    public void setIpAddressV6(String ipAddressV6) { this.ipAddressV6 = ipAddressV6; }

    public UUID getAssociatedVmId() { return associatedVmId; }
    public void setAssociatedVmId(UUID associatedVmId) { this.associatedVmId = associatedVmId; }

    public String getAssociatedPrivateIp() { return associatedPrivateIp; }
    public void setAssociatedPrivateIp(String associatedPrivateIp) { this.associatedPrivateIp = associatedPrivateIp; }

    public FloatingIpStatus getStatus() { return status; }
    public void setStatus(FloatingIpStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public enum FloatingIpStatus { AVAILABLE, ASSOCIATED, RELEASING }
}
