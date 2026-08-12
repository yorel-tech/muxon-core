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

import com.yorel.muxon.api.enums.PluginSource;
import com.yorel.muxon.api.enums.PluginStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "plugins")
public class PluginEntity {

  @Id @UuidGenerator private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String version;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false)
  private PluginSource source = PluginSource.EXTERNAL;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false)
  private PluginStatus status = PluginStatus.REGISTERED;

  @Column(name = "grpc_address")
  private String grpcAddress;

  @Column(name = "health_endpoint")
  private String healthEndpoint;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> manifest;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public PluginEntity() {}

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public PluginSource getSource() {
    return source;
  }

  public void setSource(PluginSource source) {
    this.source = source;
  }

  public PluginStatus getStatus() {
    return status;
  }

  public void setStatus(PluginStatus status) {
    this.status = status;
  }

  public String getGrpcAddress() {
    return grpcAddress;
  }

  public void setGrpcAddress(String grpcAddress) {
    this.grpcAddress = grpcAddress;
  }

  public String getHealthEndpoint() {
    return healthEndpoint;
  }

  public void setHealthEndpoint(String healthEndpoint) {
    this.healthEndpoint = healthEndpoint;
  }

  public Map<String, Object> getManifest() {
    return manifest;
  }

  public void setManifest(Map<String, Object> manifest) {
    this.manifest = manifest;
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
