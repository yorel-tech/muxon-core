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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "subnet_rbac")
public class SubnetRbacEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "subnet_id", nullable = false)
  private SubnetEntity subnet;

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "principal_type", nullable = false)
  private SubnetPrincipalType principalType;

  @Column(name = "principal_id", nullable = false)
  private UUID principalId;

  @Column(name = "permissions", columnDefinition = "TEXT[]", nullable = false)
  @JdbcTypeCode(SqlTypes.ARRAY)
  private List<String> permissions;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public SubnetRbacEntity() {}

  @PrePersist
  protected void onCreate() {
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public SubnetEntity getSubnet() {
    return subnet;
  }

  public void setSubnet(SubnetEntity subnet) {
    this.subnet = subnet;
  }

  public SubnetPrincipalType getPrincipalType() {
    return principalType;
  }

  public void setPrincipalType(SubnetPrincipalType principalType) {
    this.principalType = principalType;
  }

  public UUID getPrincipalId() {
    return principalId;
  }

  public void setPrincipalId(UUID principalId) {
    this.principalId = principalId;
  }

  public List<String> getPermissions() {
    return permissions;
  }

  public void setPermissions(List<String> permissions) {
    this.permissions = permissions;
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

  public enum SubnetPrincipalType {
    USER,
    STACK,
    ROLE
  }
}
