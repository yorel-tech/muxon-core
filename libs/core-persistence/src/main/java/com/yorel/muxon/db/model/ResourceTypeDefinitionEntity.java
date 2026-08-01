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

import com.yorel.muxon.api.enums.ResourceTypeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "resource_type_definitions")
public class ResourceTypeDefinitionEntity {

  @Id @UuidGenerator private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "plugin_id", nullable = false)
  private PluginEntity plugin;

  @Column(nullable = false, unique = true)
  private String kind;

  @Column(name = "plural_kind", nullable = false)
  private String pluralKind;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb", nullable = false)
  private Map<String, Object> schema;

  @Column(name = "supported_operations", columnDefinition = "text[]")
  private List<String> supportedOperations;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "quota_dimensions", columnDefinition = "jsonb")
  private Map<String, Object> quotaDimensions;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false)
  private ResourceTypeStatus status = ResourceTypeStatus.ACTIVE;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public ResourceTypeDefinitionEntity() {}

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public PluginEntity getPlugin() {
    return plugin;
  }

  public void setPlugin(PluginEntity plugin) {
    this.plugin = plugin;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getPluralKind() {
    return pluralKind;
  }

  public void setPluralKind(String pluralKind) {
    this.pluralKind = pluralKind;
  }

  public Map<String, Object> getSchema() {
    return schema;
  }

  public void setSchema(Map<String, Object> schema) {
    this.schema = schema;
  }

  public List<String> getSupportedOperations() {
    return supportedOperations;
  }

  public void setSupportedOperations(List<String> supportedOperations) {
    this.supportedOperations = supportedOperations;
  }

  public Map<String, Object> getQuotaDimensions() {
    return quotaDimensions;
  }

  public void setQuotaDimensions(Map<String, Object> quotaDimensions) {
    this.quotaDimensions = quotaDimensions;
  }

  public ResourceTypeStatus getStatus() {
    return status;
  }

  public void setStatus(ResourceTypeStatus status) {
    this.status = status;
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
