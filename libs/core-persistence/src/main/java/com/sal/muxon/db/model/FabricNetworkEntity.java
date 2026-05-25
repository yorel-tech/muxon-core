package com.sal.muxon.db.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fabric_network")
public class FabricNetworkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_cluster_id", nullable = false)
    private NodeClusterEntity nodeCluster;

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private FabricNetworkType type;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private FabricNetworkRole role;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "vlan_id")
    private Integer vlanId;

    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String config;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private FabricNetworkStatus status = FabricNetworkStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FabricNetworkEntity() {}

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

    public NodeClusterEntity getNodeCluster() { return nodeCluster; }
    public void setNodeCluster(NodeClusterEntity nodeCluster) { this.nodeCluster = nodeCluster; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public FabricNetworkType getType() { return type; }
    public void setType(FabricNetworkType type) { this.type = type; }

    public FabricNetworkRole getRole() { return role; }
    public void setRole(FabricNetworkRole role) { this.role = role; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public Integer getVlanId() { return vlanId; }
    public void setVlanId(Integer vlanId) { this.vlanId = vlanId; }

    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }

    public FabricNetworkStatus getStatus() { return status; }
    public void setStatus(FabricNetworkStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public enum FabricNetworkType { BRIDGE, VLAN, VXLAN, OVS, UNDERLAY }
    public enum FabricNetworkRole { TENANT_OVERLAY, STORAGE, MANAGEMENT, LIVE_MIGRATION }
    public enum FabricNetworkStatus { ACTIVE, INACTIVE }
}
