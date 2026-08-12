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
@Table(name = "security_group_rule")
public class SecurityGroupRuleEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "security_group_id", nullable = false)
  private SecurityGroupEntity securityGroup;

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false)
  private SgDirection direction;

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false)
  private SgProtocol protocol = SgProtocol.ALL;

  @Column(name = "port_range_min")
  private Integer portRangeMin;

  @Column(name = "port_range_max")
  private Integer portRangeMax;

  private String cidr;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_security_group_id")
  private SecurityGroupEntity sourceSecurityGroup;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public SecurityGroupRuleEntity() {}

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

  public SecurityGroupEntity getSecurityGroup() {
    return securityGroup;
  }

  public void setSecurityGroup(SecurityGroupEntity securityGroup) {
    this.securityGroup = securityGroup;
  }

  public SgDirection getDirection() {
    return direction;
  }

  public void setDirection(SgDirection direction) {
    this.direction = direction;
  }

  public SgProtocol getProtocol() {
    return protocol;
  }

  public void setProtocol(SgProtocol protocol) {
    this.protocol = protocol;
  }

  public Integer getPortRangeMin() {
    return portRangeMin;
  }

  public void setPortRangeMin(Integer portRangeMin) {
    this.portRangeMin = portRangeMin;
  }

  public Integer getPortRangeMax() {
    return portRangeMax;
  }

  public void setPortRangeMax(Integer portRangeMax) {
    this.portRangeMax = portRangeMax;
  }

  public String getCidr() {
    return cidr;
  }

  public void setCidr(String cidr) {
    this.cidr = cidr;
  }

  public SecurityGroupEntity getSourceSecurityGroup() {
    return sourceSecurityGroup;
  }

  public void setSourceSecurityGroup(SecurityGroupEntity sourceSecurityGroup) {
    this.sourceSecurityGroup = sourceSecurityGroup;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public enum SgDirection {
    INGRESS,
    EGRESS
  }

  public enum SgProtocol {
    TCP,
    UDP,
    ICMP,
    ALL
  }
}
