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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity mapping storage classes to provider-specific storage backends.
 *
 * <p>This entity bridges the gap between user-facing storage classes and provider-specific storage
 * implementations. For example, the "fast-ssd" storage class might map to:
 *
 * <ul>
 *   <li>Libvirt: Ceph RBD pool "ssd_pool"
 *   <li>Proxmox: ZFS dataset "tank/ssd"
 * </ul>
 *
 * <p>Multiple providers can support the same storage class, with priority determining selection
 * order. Mappings can be enabled/disabled without deletion.
 */
@Entity
@Table(
    name = "provider_storage_mappings",
    indexes = {
      @Index(name = "idx_psm_storage_class", columnList = "storage_class"),
      @Index(name = "idx_psm_provider_id", columnList = "provider_id")
    },
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_storage_class_provider",
          columnNames = {"storage_class", "provider_id"})
    })
public class ProviderStorageMappingEntity {

  @Id
  @Column(name = "id", updatable = false)
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "storage_class", nullable = false, length = 64)
  private String storageClass;

  @Column(name = "provider_id", nullable = false)
  private UUID providerId;

  @Column(name = "backend_type", nullable = false, length = 64)
  private String backendType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "backend_config", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> backendConfig;

  @Column(name = "priority")
  private Integer priority = 100;

  @Column(name = "enabled")
  private Boolean enabled = true;

  @Version private Long version;

  public ProviderStorageMappingEntity() {}

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getStorageClass() {
    return storageClass;
  }

  public void setStorageClass(String storageClass) {
    this.storageClass = storageClass;
  }

  public UUID getProviderId() {
    return providerId;
  }

  public void setProviderId(UUID providerId) {
    this.providerId = providerId;
  }

  public String getBackendType() {
    return backendType;
  }

  public void setBackendType(String backendType) {
    this.backendType = backendType;
  }

  public Map<String, Object> getBackendConfig() {
    return backendConfig;
  }

  public void setBackendConfig(Map<String, Object> backendConfig) {
    this.backendConfig = backendConfig;
  }

  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
