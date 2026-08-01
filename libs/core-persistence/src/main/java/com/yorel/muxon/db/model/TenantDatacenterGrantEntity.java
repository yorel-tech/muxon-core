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

import com.yorel.muxon.api.model.DatacenterSettings;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tenant_datacenter_grants")
public class TenantDatacenterGrantEntity {

  @Id
  @Column(name = "id")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private TenantEntity tenant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "datacenter_id", nullable = false)
  private DatacenterEntity datacenter;

  @Column(nullable = false, columnDefinition = "boolean default true")
  private Boolean access;

  @Type(value = JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> limits;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "enabled_features", columnDefinition = "text[]")
  private List<String> enabledFeatures; // TEXT[]

  @Type(value = JsonBinaryType.class)
  @Column(name = "override_settings", columnDefinition = "jsonb")
  private DatacenterSettings overrideSettings;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  // Constructors
  public TenantDatacenterGrantEntity() {}

  // Getters and setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public TenantEntity getTenant() {
    return tenant;
  }

  public void setTenant(TenantEntity tenant) {
    this.tenant = tenant;
  }

  public DatacenterEntity getDatacenter() {
    return datacenter;
  }

  public void setDatacenter(DatacenterEntity datacenter) {
    this.datacenter = datacenter;
  }

  public Boolean getAccess() {
    return access;
  }

  public void setAccess(Boolean access) {
    this.access = access;
  }

  public Map<String, Object> getLimits() {
    return limits;
  }

  public void setLimits(Map<String, Object> limits) {
    this.limits = limits;
  }

  public List<String> getEnabledFeatures() {
    return enabledFeatures;
  }

  public void setEnabledFeatures(List<String> enabledFeatures) {
    this.enabledFeatures = enabledFeatures;
  }

  public DatacenterSettings getOverrideSettings() {
    return overrideSettings;
  }

  public void setOverrideSettings(DatacenterSettings overrideSettings) {
    this.overrideSettings = overrideSettings;
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
