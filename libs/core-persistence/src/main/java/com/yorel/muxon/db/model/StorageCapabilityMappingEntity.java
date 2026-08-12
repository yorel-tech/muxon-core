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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity mapping Muxon generic capabilities to provider-specific equivalents.
 *
 * <p>This entity enables translation between Muxon's generic storage capability model and
 * provider-specific terminology. For example:
 *
 * <ul>
 *   <li>Muxon "performance: high" → Libvirt pool_type: ["rbd", "nvme"]
 *   <li>Muxon "performance: high" → Proxmox storage_type: ["rbd", "zfspool"]
 * </ul>
 */
@Entity
@Table(
    name = "storage_capability_mappings",
    indexes = {
      @Index(name = "idx_scm_muxon_capability", columnList = "muxon_capability"),
      @Index(name = "idx_scm_provider_type", columnList = "provider_type")
    },
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_capability_provider",
          columnNames = {"muxon_capability", "provider_type", "provider_capability"})
    })
public class StorageCapabilityMappingEntity {

  @Id
  @Column(name = "id", updatable = false)
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "muxon_capability", nullable = false, length = 64)
  private String muxonCapability;

  @Column(name = "provider_type", nullable = false, length = 32)
  private String providerType;

  @Column(name = "provider_capability", nullable = false, length = 64)
  private String providerCapability;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "value_mapping", columnDefinition = "jsonb")
  private Map<String, Object> valueMapping;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = Instant.now();
  }

  public StorageCapabilityMappingEntity() {}

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getMuxonCapability() {
    return muxonCapability;
  }

  public void setMuxonCapability(String muxonCapability) {
    this.muxonCapability = muxonCapability;
  }

  public String getProviderType() {
    return providerType;
  }

  public void setProviderType(String providerType) {
    this.providerType = providerType;
  }

  public String getProviderCapability() {
    return providerCapability;
  }

  public void setProviderCapability(String providerCapability) {
    this.providerCapability = providerCapability;
  }

  public Map<String, Object> getValueMapping() {
    return valueMapping;
  }

  public void setValueMapping(Map<String, Object> valueMapping) {
    this.valueMapping = valueMapping;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
