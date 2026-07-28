/**
 * Entity for user_role_bindings view.
 * This view combines tenant_user and role_binding details.
 */
package com.yorel.muxon.db.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "user_role_bindings")
public class UserRoleBindingViewEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "identity_provider_id")
    private String identityProviderId;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "user_metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> userMetadata;

    @Column(name = "user_created_at")
    private Instant userCreatedAt;

    @Column(name = "user_updated_at")
    private Instant userUpdatedAt;

    @Column(name = "binding_id")
    private UUID bindingId;

    @Column(name = "subject_type")
    private String subjectType;

    @Column(name = "subject_id")
    private String subjectId;

    @Column(name = "scope_type")
    private String scopeType;

    @Column(name = "scope_id")
    private UUID scopeId;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "binding_created_at")
    private Instant bindingCreatedAt;

    @Column(name = "role_name")
    private String roleName;

    @Column(name = "role_description")
    private String roleDescription;

    // Constructors
    public UserRoleBindingViewEntity() {
    }

    // Getters and setters
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getIdentityProviderId() {
        return identityProviderId;
    }

    public void setIdentityProviderId(String identityProviderId) {
        this.identityProviderId = identityProviderId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Map<String, String> getUserMetadata() {
        return userMetadata;
    }

    public void setUserMetadata(Map<String, String> userMetadata) {
        this.userMetadata = userMetadata;
    }

    public Instant getUserCreatedAt() {
        return userCreatedAt;
    }

    public void setUserCreatedAt(Instant userCreatedAt) {
        this.userCreatedAt = userCreatedAt;
    }

    public Instant getUserUpdatedAt() {
        return userUpdatedAt;
    }

    public void setUserUpdatedAt(Instant userUpdatedAt) {
        this.userUpdatedAt = userUpdatedAt;
    }

    public UUID getBindingId() {
        return bindingId;
    }

    public void setBindingId(UUID bindingId) {
        this.bindingId = bindingId;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public UUID getScopeId() {
        return scopeId;
    }

    public void setScopeId(UUID scopeId) {
        this.scopeId = scopeId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getBindingCreatedAt() {
        return bindingCreatedAt;
    }

    public void setBindingCreatedAt(Instant bindingCreatedAt) {
        this.bindingCreatedAt = bindingCreatedAt;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
public String getRoleDescription() {
    return roleDescription;
}

public void setRoleDescription(String roleDescription) {
    this.roleDescription = roleDescription;
}

}
