/**
 * Entity for role table.
 */
package com.onetattva.infron.db.model;

import com.onetattva.infron.api.enums.RoleScopeType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "role", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class RoleEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "scope_type", nullable = false, columnDefinition = "role_scope default 'SYSTEM'")
    private RoleScopeType scopeType;

    @Column(name = "scope_id")
    private UUID scopeId;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean immutable;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permission",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<PermissionEntity> permissions = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors
    public RoleEntity() {
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RoleScopeType getScopeType() {
        return scopeType;
    }

    public void setScopeType(RoleScopeType scopeType) {
        this.scopeType = scopeType;
    }

    public UUID getScopeId() {
        return scopeId;
    }

    public void setScopeId(UUID scopeId) {
        this.scopeId = scopeId;
    }

    public Boolean getImmutable() {
        return immutable;
    }

    public void setImmutable(Boolean immutable) {
        this.immutable = immutable;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Set<PermissionEntity> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(Set<PermissionEntity> permissions) {
        this.permissions = permissions;
    }
}
