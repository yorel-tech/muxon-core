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

import com.yorel.muxon.api.model.DatacenterCapacity;
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
import java.util.UUID;
import org.hibernate.annotations.Type;

/** Entity for datacenter table. */
@Entity
@Table(name = "datacenters")
public class DatacenterEntity {

  @Id
  // @GeneratedValue(strategy = GenerationType.AUTO) // For UUID
  @Column(name = "id")
  private UUID id;

  @Column(nullable = false)
  private String name;

  private String description;

  /**
   * Node cluster that provides resources to this datacenter. Once set, this reference cannot be
   * changed.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "node_cluster_id", nullable = false)
  private NodeClusterEntity nodeCluster;

  @Type(value = JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private DatacenterCapacity capacity;

  @Type(value = JsonBinaryType.class)
  @Column(columnDefinition = "jsonb")
  private DatacenterSettings settings;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  // Constructors
  public DatacenterEntity() {}

  public DatacenterEntity(UUID id, String name) {
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public NodeClusterEntity getNodeCluster() {
    return nodeCluster;
  }

  public void setNodeCluster(NodeClusterEntity nodeCluster) {
    this.nodeCluster = nodeCluster;
  }

  public DatacenterCapacity getCapacity() {
    return capacity;
  }

  public void setCapacity(DatacenterCapacity capacity) {
    this.capacity = capacity;
  }

  public DatacenterSettings getSettings() {
    return settings;
  }

  public void setSettings(DatacenterSettings settings) {
    this.settings = settings;
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
