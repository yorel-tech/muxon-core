package com.scal.muxon.db.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "load_balancer_target")
public class LoadBalancerTargetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_balancer_id", nullable = false)
    private LoadBalancerEntity loadBalancer;

    @Column(name = "vm_id")
    private UUID vmId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(nullable = false)
    private int port;

    @Column(nullable = false)
    private int weight = 1;

    @Column(nullable = false)
    private String status = "HEALTHY";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LoadBalancerTargetEntity() {}

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

    public LoadBalancerEntity getLoadBalancer() { return loadBalancer; }
    public void setLoadBalancer(LoadBalancerEntity loadBalancer) { this.loadBalancer = loadBalancer; }

    public UUID getVmId() { return vmId; }
    public void setVmId(UUID vmId) { this.vmId = vmId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
