package com.scal.muxon.db.model;

import com.scal.muxon.api.enums.CatalogContributionStatus;
import com.scal.muxon.api.enums.CatalogItemType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "plugin_catalog_contributions")
public class PluginCatalogContributionEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plugin_id", nullable = false)
    private PluginEntity plugin;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "catalog_item_type", nullable = false)
    private CatalogItemType catalogItemType;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_schema", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> configSchema;

    @Column(name = "target_capability")
    private String targetCapability;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private CatalogContributionStatus status = CatalogContributionStatus.PENDING_APPROVAL;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PluginCatalogContributionEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PluginEntity getPlugin() { return plugin; }
    public void setPlugin(PluginEntity plugin) { this.plugin = plugin; }

    public CatalogItemType getCatalogItemType() { return catalogItemType; }
    public void setCatalogItemType(CatalogItemType catalogItemType) { this.catalogItemType = catalogItemType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, Object> getConfigSchema() { return configSchema; }
    public void setConfigSchema(Map<String, Object> configSchema) { this.configSchema = configSchema; }

    public String getTargetCapability() { return targetCapability; }
    public void setTargetCapability(String targetCapability) { this.targetCapability = targetCapability; }

    public CatalogContributionStatus getStatus() { return status; }
    public void setStatus(CatalogContributionStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
