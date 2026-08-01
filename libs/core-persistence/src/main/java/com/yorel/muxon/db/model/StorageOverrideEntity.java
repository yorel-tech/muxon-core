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
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity representing manual storage class to provider storage mappings.
 *
 * <p>Storage overrides allow administrators to bypass the automatic scheduler and manually map
 * storage classes to specific provider storage pools/classes. This is useful for:
 *
 * <ul>
 *   <li>Enforcing specific storage backends for compliance
 *   <li>Testing new storage configurations
 *   <li>Troubleshooting scheduler issues
 * </ul>
 *
 * <p>Example: Map "fast-ssd" storage class to specific Ceph pools:
 *
 * <pre>
 * storage_class_name: "fast-ssd"
 * provider_type: "libvirt"
 * provider_storage_names: ["ssd_pool", "nvme_pool"]
 * </pre>
 */
@Entity
@Table(
    name = "storage_overrides",
    indexes = {
      @Index(name = "idx_storage_overrides_class", columnList = "storage_class_name"),
      @Index(name = "idx_storage_overrides_provider", columnList = "provider_type")
    },
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_storage_class_provider",
          columnNames = {"storage_class_name", "provider_type"})
    })
public class StorageOverrideEntity {

  @Id
  @Column(name = "id", updatable = false)
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "storage_class_name", nullable = false, length = 64)
  private String storageClassName;

  @Column(name = "provider_type", nullable = false, length = 32)
  private String providerType;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "provider_storage_names", nullable = false, columnDefinition = "text[]")
  private List<String> providerStorageNames;

  @Column(name = "priority")
  private Integer priority = 100;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

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

  public StorageOverrideEntity() {}

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getStorageClassName() {
    return storageClassName;
  }

  public void setStorageClassName(String storageClassName) {
    this.storageClassName = storageClassName;
  }

  public String getProviderType() {
    return providerType;
  }

  public void setProviderType(String providerType) {
    this.providerType = providerType;
  }

  public List<String> getProviderStorageNames() {
    return providerStorageNames;
  }

  public void setProviderStorageNames(List<String> providerStorageNames) {
    this.providerStorageNames = providerStorageNames;
  }

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
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

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
