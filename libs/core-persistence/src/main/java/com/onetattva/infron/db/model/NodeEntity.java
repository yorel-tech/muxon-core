/**
 * Entity for node table.
 */
package com.onetattva.infron.db.model;

import com.onetattva.infron.api.model.Node;
import com.onetattva.infron.api.model.Resources;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "node")
public class NodeEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cluster_id", nullable = true)
    private NodeClusterEntity cluster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderEntity provider;

    @Column(name = "external_id")
    private String externalId;

    @Column
    private String role;

    @Column(name = "cpu_total")
    private Integer cpuTotal;

    @Column(name = "mem_mb")
    private Integer memMb;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false)
    private Node.StatusEnum status;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> credentials;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "ip_addresses", columnDefinition = "text[]")
    private List<String> ipAddresses; // TEXT[] - but API uses List<String>

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "capabilities", columnDefinition = "text[]")
    private List<String> capabilities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Resources resources;

    // Constructors
    public NodeEntity() {
    }

    public NodeEntity(String name) {
        setName(name);
    }

    // Getters and setters
    public NodeClusterEntity getCluster() {
        return cluster;
    }

    public void setCluster(NodeClusterEntity cluster) {
        this.cluster = cluster;
    }

    public ProviderEntity getProvider() {
        return provider;
    }

    public void setProvider(ProviderEntity provider) {
        this.provider = provider;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getCpuTotal() {
        return cpuTotal;
    }

    public void setCpuTotal(Integer cpuTotal) {
        this.cpuTotal = cpuTotal;
    }

    public Integer getMemMb() {
        return memMb;
    }

    public void setMemMb(Integer memMb) {
        this.memMb = memMb;
    }

    public Node.StatusEnum getStatus() {
        return status;
    }

    public void setStatus(Node.StatusEnum status) {
        this.status = status;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Map<String, String> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, String> credentials) {
        this.credentials = credentials;
    }

    public List<String> getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(List<String> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    public Resources getResources() {
        return resources;
    }

    public void setResources(Resources resources) {
        this.resources = resources;
    }

    // Helper methods
    public boolean isActive() {
        return status == Node.StatusEnum.READY;
    }
}
