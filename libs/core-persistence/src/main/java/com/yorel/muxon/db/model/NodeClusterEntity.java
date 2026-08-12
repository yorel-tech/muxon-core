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

import com.yorel.muxon.api.model.Node;
import com.yorel.muxon.api.model.NodeCluster;
import com.yorel.muxon.api.model.Resources;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "node_clusters")
public class NodeClusterEntity extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "provider_id", nullable = false)
  private ProviderEntity provider;

  @Column(name = "external_id")
  private String externalId;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "status", nullable = false)
  private NodeCluster.StatusEnum status;

  @OneToMany(mappedBy = "cluster", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<NodeEntity> nodes = new ArrayList<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Resources resources;

  // Constructors
  public NodeClusterEntity() {}

  public NodeClusterEntity(String name) {
    setName(name);
  }

  // Getters and setters
  public ProviderEntity getProvider() {
    return provider;
  }

  public void setProvider(ProviderEntity provider) {
    this.provider = provider;
  }

  public String getExternalId() {
    return externalId;
  }

  public void setExternalId(String externalId) {
    this.externalId = externalId;
  }

  public NodeCluster.StatusEnum getStatus() {
    return status;
  }

  public void setStatus(NodeCluster.StatusEnum status) {
    this.status = status;
  }

  public List<NodeEntity> getNodes() {
    return nodes;
  }

  public void setNodes(List<NodeEntity> nodes) {
    this.nodes = nodes;
  }

  public Resources getResources() {
    return resources;
  }

  public void setResources(Resources resources) {
    this.resources = resources;
  }

  // Helper methods
  public long getNodeCount() {
    return nodes.size();
  }

  public boolean hasActiveNodes() {
    return nodes.stream().anyMatch(n -> n.getStatus() == Node.StatusEnum.READY);
  }
}
