package com.yorel.muxon.db.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "route_table")
public class RouteTableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vpc_id", nullable = false)
    private VpcEntity vpc;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_main", nullable = false)
    private boolean isMain = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public RouteTableEntity() {}

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

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isMain() { return isMain; }
    public void setMain(boolean main) { isMain = main; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
