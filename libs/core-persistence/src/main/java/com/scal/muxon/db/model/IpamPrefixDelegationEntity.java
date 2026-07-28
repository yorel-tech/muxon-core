package com.scal.muxon.db.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ipam_prefix_delegation")
public class IpamPrefixDelegationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subnet_id", nullable = false)
    private SubnetEntity subnet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stack_id", nullable = false)
    private StackEntity stack;

    @Column(name = "node_vm_id", nullable = false)
    private UUID nodeVmId;

    @Column(name = "delegated_cidr", nullable = false)
    private String delegatedCidr;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private IpamDelegationStatus status = IpamDelegationStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public IpamPrefixDelegationEntity() {}

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

    public SubnetEntity getSubnet() { return subnet; }
    public void setSubnet(SubnetEntity subnet) { this.subnet = subnet; }

    public StackEntity getStack() { return stack; }
    public void setStack(StackEntity stack) { this.stack = stack; }

    public UUID getNodeVmId() { return nodeVmId; }
    public void setNodeVmId(UUID nodeVmId) { this.nodeVmId = nodeVmId; }

    public String getDelegatedCidr() { return delegatedCidr; }
    public void setDelegatedCidr(String delegatedCidr) { this.delegatedCidr = delegatedCidr; }

    public IpamDelegationStatus getStatus() { return status; }
    public void setStatus(IpamDelegationStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public enum IpamDelegationStatus { PENDING, ACTIVE, ROUTE_ERROR, RELEASED }
}
