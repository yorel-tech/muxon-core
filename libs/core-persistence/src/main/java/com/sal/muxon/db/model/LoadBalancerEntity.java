package com.sal.muxon.db.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "load_balancer")
public class LoadBalancerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vpc_id", nullable = false)
    private VpcEntity vpc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subnet_id", nullable = false)
    private SubnetEntity subnet;

    @Column(nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private LbScheme scheme = LbScheme.INTERNAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floating_ip_id")
    private FloatingIpEntity floatingIp;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public LoadBalancerEntity() {}

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

    public SubnetEntity getSubnet() { return subnet; }
    public void setSubnet(SubnetEntity subnet) { this.subnet = subnet; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LbScheme getScheme() { return scheme; }
    public void setScheme(LbScheme scheme) { this.scheme = scheme; }

    public FloatingIpEntity getFloatingIp() { return floatingIp; }
    public void setFloatingIp(FloatingIpEntity floatingIp) { this.floatingIp = floatingIp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public enum LbScheme { INTERNAL, INTERNET_FACING }
}
