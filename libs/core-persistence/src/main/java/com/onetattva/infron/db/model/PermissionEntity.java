/**
 * Entity for permission table.
 */
package com.onetattva.infron.db.model;

import com.onetattva.infron.api.enums.RoleScopeType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "permission", uniqueConstraints = @UniqueConstraint(columnNames = "action"))
public class PermissionEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String action;

    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "role_scope default 'SYSTEM'")
    private RoleScopeType scope;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors
    public PermissionEntity() {
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RoleScopeType getScope() {
        return scope;
    }

    public void setScope(RoleScopeType scope) {
        this.scope = scope;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
