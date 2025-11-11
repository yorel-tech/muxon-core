/**
 * Entity for node table.
 */
package com.onetattva.infron.db.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "node")
public class NodeEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cluster_id")
    private NodeClusterEntity cluster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id")
    private DatacenterEntity datacenter;

    @Column(nullable = false)
    private String name;

    @Column(name = "provider_type")
    private String providerType;

    private String endpoint;

    private Integer port;

    @Column(columnDefinition = "jsonb")
    private String auth; // JSONB as String

    @Column(name = "ip_addresses")
    private String[] ipAddresses; // TEXT[]

    @Column(columnDefinition = "jsonb")
    private String metadata; // JSONB as String

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public NodeEntity() {
    }

    public NodeEntity(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public NodeClusterEntity getCluster() {
        return cluster;
    }

    public void setCluster(NodeClusterEntity cluster) {
        this.cluster = cluster;
    }

    public DatacenterEntity getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(DatacenterEntity datacenter) {
        this.datacenter = datacenter;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public String[] getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(String[] ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
