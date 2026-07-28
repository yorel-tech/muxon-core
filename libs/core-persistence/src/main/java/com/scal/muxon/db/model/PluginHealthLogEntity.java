package com.scal.muxon.db.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "plugin_health_log")
public class PluginHealthLogEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plugin_id", nullable = false)
    private PluginEntity plugin;

    @CreationTimestamp
    @Column(name = "checked_at", nullable = false, updatable = false)
    private Instant checkedAt;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_detail")
    private String errorDetail;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    public PluginHealthLogEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PluginEntity getPlugin() { return plugin; }
    public void setPlugin(PluginEntity plugin) { this.plugin = plugin; }

    public Instant getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Instant checkedAt) { this.checkedAt = checkedAt; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorDetail() { return errorDetail; }
    public void setErrorDetail(String errorDetail) { this.errorDetail = errorDetail; }

    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
}
