package com.scal.muxon.db.model;

import com.scal.muxon.api.enums.PluginCapabilityType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "plugin_capabilities")
public class PluginCapabilityEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plugin_id", nullable = false)
    private PluginEntity plugin;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "capability_type", nullable = false)
    private PluginCapabilityType capabilityType;

    @Column(name = "capability_name", nullable = false)
    private String capabilityName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> config;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PluginCapabilityEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PluginEntity getPlugin() { return plugin; }
    public void setPlugin(PluginEntity plugin) { this.plugin = plugin; }

    public PluginCapabilityType getCapabilityType() { return capabilityType; }
    public void setCapabilityType(PluginCapabilityType capabilityType) { this.capabilityType = capabilityType; }

    public String getCapabilityName() { return capabilityName; }
    public void setCapabilityName(String capabilityName) { this.capabilityName = capabilityName; }

    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
