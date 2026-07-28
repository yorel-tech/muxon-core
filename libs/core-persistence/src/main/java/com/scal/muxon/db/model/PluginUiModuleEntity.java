package com.scal.muxon.db.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plugin_ui_modules")
public class PluginUiModuleEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plugin_id", nullable = false)
    private PluginEntity plugin;

    @Column(name = "module_id", nullable = false, unique = true)
    private String moduleId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "module_url", nullable = false)
    private String moduleUrl;

    @Column(name = "required_permissions", columnDefinition = "text[]")
    private List<String> requiredPermissions;

    @Column(name = "target_kinds", columnDefinition = "text[]")
    private List<String> targetKinds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PluginUiModuleEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PluginEntity getPlugin() { return plugin; }
    public void setPlugin(PluginEntity plugin) { this.plugin = plugin; }

    public String getModuleId() { return moduleId; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getModuleUrl() { return moduleUrl; }
    public void setModuleUrl(String moduleUrl) { this.moduleUrl = moduleUrl; }

    public List<String> getRequiredPermissions() { return requiredPermissions; }
    public void setRequiredPermissions(List<String> requiredPermissions) { this.requiredPermissions = requiredPermissions; }

    public List<String> getTargetKinds() { return targetKinds; }
    public void setTargetKinds(List<String> targetKinds) { this.targetKinds = targetKinds; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
