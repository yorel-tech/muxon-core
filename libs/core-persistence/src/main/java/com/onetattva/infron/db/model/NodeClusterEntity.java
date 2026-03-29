/**
 * Entity for node_cluster table.
 */
package com.onetattva.infron.db.model;

import com.onetattva.infron.api.model.Node;
import com.onetattva.infron.api.model.NodeCluster;
import com.onetattva.infron.api.model.Resources;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "node_cluster")
public class NodeClusterEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private ProviderEntity provider;

    @Column(name = "external_id")
    private String externalId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", nullable = false)
    private NodeCluster.StatusEnum status;

    @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<NodeEntity> nodes = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Resources resources;

    // Constructors
    public NodeClusterEntity() {
    }

    public NodeClusterEntity(String name) {
        setName(name);
    }

    // Getters and setters
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

    public NodeCluster.StatusEnum getStatus() {
        return status;
    }

    public void setStatus(NodeCluster.StatusEnum status) {
        this.status = status;
    }

    public List<NodeEntity> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeEntity> nodes) {
        this.nodes = nodes;
    }

    public Resources getResources() {
        return resources;
    }

    public void setResources(Resources resources) {
        this.resources = resources;
    }

    // Helper methods
    public long getNodeCount() {
        return nodes.size();
    }

    public boolean hasActiveNodes() {
        return nodes.stream().anyMatch(n -> n.getStatus() == Node.StatusEnum.READY);
    }
}
