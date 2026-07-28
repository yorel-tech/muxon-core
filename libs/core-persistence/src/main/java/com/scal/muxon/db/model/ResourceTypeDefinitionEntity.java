package com.scal.muxon.db.model;

import com.scal.muxon.api.enums.ResourceTypeStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "resource_type_definitions")
public class ResourceTypeDefinitionEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plugin_id", nullable = false)
    private PluginEntity plugin;

    @Column(nullable = false, unique = true)
    private String kind;

    @Column(name = "plural_kind", nullable = false)
    private String pluralKind;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> schema;

    @Column(name = "supported_operations", columnDefinition = "text[]")
    private List<String> supportedOperations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quota_dimensions", columnDefinition = "jsonb")
    private Map<String, Object> quotaDimensions;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private ResourceTypeStatus status = ResourceTypeStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ResourceTypeDefinitionEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PluginEntity getPlugin() { return plugin; }
    public void setPlugin(PluginEntity plugin) { this.plugin = plugin; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public String getPluralKind() { return pluralKind; }
    public void setPluralKind(String pluralKind) { this.pluralKind = pluralKind; }

    public Map<String, Object> getSchema() { return schema; }
    public void setSchema(Map<String, Object> schema) { this.schema = schema; }

    public List<String> getSupportedOperations() { return supportedOperations; }
    public void setSupportedOperations(List<String> supportedOperations) { this.supportedOperations = supportedOperations; }

    public Map<String, Object> getQuotaDimensions() { return quotaDimensions; }
    public void setQuotaDimensions(Map<String, Object> quotaDimensions) { this.quotaDimensions = quotaDimensions; }

    public ResourceTypeStatus getStatus() { return status; }
    public void setStatus(ResourceTypeStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
