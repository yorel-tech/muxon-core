package com.sal.muxon.db.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "network_edge_node")
public class NetworkEdgeNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id", nullable = false)
    private DatacenterEntity datacenter;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String host;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private NetworkEdgeNodeType type = NetworkEdgeNodeType.STANDARD;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private NetworkEdgeNodeStatus status = NetworkEdgeNodeStatus.ACTIVE;

    @Column(columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String capabilities;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NetworkEdgeNodeEntity() {}

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

    public DatacenterEntity getDatacenter() { return datacenter; }
    public void setDatacenter(DatacenterEntity datacenter) { this.datacenter = datacenter; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public NetworkEdgeNodeType getType() { return type; }
    public void setType(NetworkEdgeNodeType type) { this.type = type; }

    public NetworkEdgeNodeStatus getStatus() { return status; }
    public void setStatus(NetworkEdgeNodeStatus status) { this.status = status; }

    public String getCapabilities() { return capabilities; }
    public void setCapabilities(String capabilities) { this.capabilities = capabilities; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public enum NetworkEdgeNodeType { STANDARD, HA_PRIMARY, HA_SECONDARY }
    public enum NetworkEdgeNodeStatus { ACTIVE, MAINTENANCE, INACTIVE }
}
