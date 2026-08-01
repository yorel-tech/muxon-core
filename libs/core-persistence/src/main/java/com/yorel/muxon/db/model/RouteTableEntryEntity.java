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
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "route_table_entry")
public class RouteTableEntryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "route_table_id", nullable = false)
  private RouteTableEntity routeTable;

  @Column(name = "destination_cidr", nullable = false)
  private String destinationCidr;

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "target_type", nullable = false)
  private RouteTargetType targetType;

  @Column(name = "target_id")
  private UUID targetId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public RouteTableEntryEntity() {}

  @PrePersist
  protected void onCreate() {
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public RouteTableEntity getRouteTable() {
    return routeTable;
  }

  public void setRouteTable(RouteTableEntity routeTable) {
    this.routeTable = routeTable;
  }

  public String getDestinationCidr() {
    return destinationCidr;
  }

  public void setDestinationCidr(String destinationCidr) {
    this.destinationCidr = destinationCidr;
  }

  public RouteTargetType getTargetType() {
    return targetType;
  }

  public void setTargetType(RouteTargetType targetType) {
    this.targetType = targetType;
  }

  public UUID getTargetId() {
    return targetId;
  }

  public void setTargetId(UUID targetId) {
    this.targetId = targetId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public enum RouteTargetType {
    INTERNET_GATEWAY,
    NAT_GATEWAY,
    LOCAL,
    VPC_PEERING,
    INSTANCE
  }
}
