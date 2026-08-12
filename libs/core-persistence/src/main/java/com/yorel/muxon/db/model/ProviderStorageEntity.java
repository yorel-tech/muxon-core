/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity representing normalized provider storage pools/classes.
 *
 * <p>This entity stores discovered storage from providers (Libvirt pools, Proxmox storage)
 * normalized into a common capability model. The scheduler uses this to match storage class
 * requirements to available provider storage.
 *
 * <p>Example: A Ceph RBD pool from Libvirt becomes:
 *
 * <ul>
 *   <li>external_id: "ssd_pool"
 *   <li>storage_type: "ceph-rbd"
 *   <li>capabilities: {"performance": "high", "media": "ssd", "shared": true, "redundancy":
 *       "replicated"}
 *   <li>metrics: {"free_gb": 500, "total_gb": 1000, "iops": 50000}
 * </ul>
 */
@Entity
@Table(
    name = "provider_storages",
    indexes = {
      @Index(name = "idx_provider_storage_provider", columnList = "provider_id"),
      @Index(name = "idx_provider_storage_type", columnList = "provider_type"),
      @Index(name = "idx_provider_storage_datacenter", columnList = "datacenter_id"),
      @Index(name = "idx_provider_storage_enabled", columnList = "enabled"),
      @Index(name = "idx_provider_storage_name", columnList = "name")
    },
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_provider_external_id",
          columnNames = {"provider_id", "external_id"})
    })
public class ProviderStorageEntity {

  @Id
  @Column(name = "id", updatable = false)
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "provider_id", nullable = false)
  private UUID providerId;

  @Column(name = "provider_type", nullable = false, length = 32)
  private String providerType;

  @Column(name = "external_id", nullable = false, length = 255)
  private String externalId;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "storage_type", nullable = false, length = 64)
  private String storageType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "capabilities", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> capabilities;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metrics", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> metrics;

  @Column(name = "node_id", length = 255)
  private String nodeId;

  @Column(name = "datacenter_id")
  private UUID datacenterId;

  @Column(name = "enabled")
  private Boolean enabled = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "synced_at")
  private Instant syncedAt;

  @Version private Long version;

  @PrePersist
  protected void onCreate() {
    createdAt = Instant.now();
    updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = Instant.now();
  }

  public ProviderStorageEntity() {}

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getProviderId() {
    return providerId;
  }

  public void setProviderId(UUID providerId) {
    this.providerId = providerId;
  }

  public String getProviderType() {
    return providerType;
  }

  public void setProviderType(String providerType) {
    this.providerType = providerType;
  }

  public String getExternalId() {
    return externalId;
  }

  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStorageType() {
    return storageType;
  }

  public void setStorageType(String storageType) {
    this.storageType = storageType;
  }

  public Map<String, Object> getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(Map<String, Object> capabilities) {
    this.capabilities = capabilities;
  }

  public Map<String, Object> getMetrics() {
    return metrics;
  }

  public void setMetrics(Map<String, Object> metrics) {
    this.metrics = metrics;
  }

  public String getNodeId() {
    return nodeId;
  }

  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  public UUID getDatacenterId() {
    return datacenterId;
  }

  public void setDatacenterId(UUID datacenterId) {
    this.datacenterId = datacenterId;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
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

  public Instant getSyncedAt() {
    return syncedAt;
  }

  public void setSyncedAt(Instant syncedAt) {
    this.syncedAt = syncedAt;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
