/**
 * Entity for provider table.
 */
package com.yorel.muxon.db.model;

import com.yorel.muxon.api.model.ProviderType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "providers")
public class ProviderEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "type", nullable = false)
    private ProviderType type;

    @Column(nullable = false)
    private String endpoint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> credentials; // JSONB

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> capabilities; // JSONB

    @Column(nullable = false)
    private String status;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<NodeClusterEntity> clusters = new ArrayList<>();

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<NodeEntity> nodes = new ArrayList<>();

    // Constructors
    public ProviderEntity() {
    }

    public ProviderEntity(UUID id, String name) {
        super(id, name);
    }

    // Getters and setters
    public ProviderType getType() {
        return type;
    }

    public void setType(ProviderType type) {
        this.type = type;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Map<String, String> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, String> credentials) {
        this.credentials = credentials;
    }

    public Map<String, String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Map<String, String> capabilities) {
        this.capabilities = capabilities;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<NodeClusterEntity> getClusters() {
        return clusters;
    }

    public void setClusters(List<NodeClusterEntity> clusters) {
        this.clusters = clusters;
    }

    public List<NodeEntity> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeEntity> nodes) {
        this.nodes = nodes;
    }

    // Helper methods for HATEOAS link generation
    public boolean hasActiveResources() {
        return clusters.stream().anyMatch(c -> "ACTIVE".equals(c.getStatus())) ||
               nodes.stream().anyMatch(n -> "ACTIVE".equals(n.getStatus()));
    }

    public long getNodeCount() {
        return nodes.size();
    }

    public long getVmCount() {
        // Implementation depends on VM entity relationship
        return 0; // Placeholder
    }
}
