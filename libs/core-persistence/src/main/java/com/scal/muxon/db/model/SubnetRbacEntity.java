package com.scal.muxon.db.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "subnet_rbac")
public class SubnetRbacEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subnet_id", nullable = false)
    private SubnetEntity subnet;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "principal_type", nullable = false)
    private SubnetPrincipalType principalType;

    @Column(name = "principal_id", nullable = false)
    private UUID principalId;

    @Column(name = "permissions", columnDefinition = "TEXT[]", nullable = false)
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> permissions;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SubnetRbacEntity() {}

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

    public SubnetPrincipalType getPrincipalType() { return principalType; }
    public void setPrincipalType(SubnetPrincipalType principalType) { this.principalType = principalType; }

    public UUID getPrincipalId() { return principalId; }
    public void setPrincipalId(UUID principalId) { this.principalId = principalId; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public enum SubnetPrincipalType { USER, STACK, ROLE }
}
