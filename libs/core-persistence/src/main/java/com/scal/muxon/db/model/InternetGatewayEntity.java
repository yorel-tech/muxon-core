package com.scal.muxon.db.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "internet_gateway")
public class InternetGatewayEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vpc_id", nullable = false, unique = true)
    private VpcEntity vpc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "network_edge_node_id")
    private NetworkEdgeNodeEntity networkEdgeNode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "ha_mode")
    private String haMode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public InternetGatewayEntity() {}

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

    public VpcEntity getVpc() { return vpc; }
    public void setVpc(VpcEntity vpc) { this.vpc = vpc; }

    public NetworkEdgeNodeEntity getNetworkEdgeNode() { return networkEdgeNode; }
    public void setNetworkEdgeNode(NetworkEdgeNodeEntity networkEdgeNode) { this.networkEdgeNode = networkEdgeNode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getHaMode() { return haMode; }
    public void setHaMode(String haMode) { this.haMode = haMode; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
