package com.sal.muxon.db.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "route_table_entry")
public class RouteTableEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_table_id", nullable = false)
    private RouteTableEntity routeTable;

    @Column(name = "destination_cidr", nullable = false)
    private String destinationCidr;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "target_type", nullable = false)
    private RouteTargetType targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RouteTableEntryEntity() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public RouteTableEntity getRouteTable() { return routeTable; }
    public void setRouteTable(RouteTableEntity routeTable) { this.routeTable = routeTable; }

    public String getDestinationCidr() { return destinationCidr; }
    public void setDestinationCidr(String destinationCidr) { this.destinationCidr = destinationCidr; }

    public RouteTargetType getTargetType() { return targetType; }
    public void setTargetType(RouteTargetType targetType) { this.targetType = targetType; }

    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public enum RouteTargetType { INTERNET_GATEWAY, NAT_GATEWAY, LOCAL, VPC_PEERING, INSTANCE }
}
