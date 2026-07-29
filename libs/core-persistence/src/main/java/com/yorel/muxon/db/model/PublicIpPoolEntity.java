package com.yorel.muxon.db.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "public_ip_pool")
public class PublicIpPoolEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id", nullable = false)
    private DatacenterEntity datacenter;

    @Column(nullable = false)
    private String cidr;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "total_ips", nullable = false)
    private int totalIps;

    @Column(name = "allocated_ips", nullable = false)
    private int allocatedIps;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PublicIpPoolEntity() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public int getAvailableIps() {
        return totalIps - allocatedIps;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public DatacenterEntity getDatacenter() { return datacenter; }
    public void setDatacenter(DatacenterEntity datacenter) { this.datacenter = datacenter; }

    public String getCidr() { return cidr; }
    public void setCidr(String cidr) { this.cidr = cidr; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getTotalIps() { return totalIps; }
    public void setTotalIps(int totalIps) { this.totalIps = totalIps; }

    public int getAllocatedIps() { return allocatedIps; }
    public void setAllocatedIps(int allocatedIps) { this.allocatedIps = allocatedIps; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
