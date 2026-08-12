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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_network_policy")
public class TenantNetworkPolicyEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false, unique = true)
  private TenantEntity tenant;

  @Column(name = "max_vpcs", nullable = false)
  private int maxVpcs = 10;

  @Column(name = "max_public_ips", nullable = false)
  private int maxPublicIps = 5;

  @Column(name = "max_subnets_per_vpc", nullable = false)
  private int maxSubnetsPerVpc = 10;

  @Column(name = "vpn_allowed", nullable = false)
  private boolean vpnAllowed = false;

  @Column(name = "peering_allowed", nullable = false)
  private boolean peeringAllowed = false;

  @Column(name = "ha_networking_allowed", nullable = false)
  private boolean haNetworkingAllowed = false;

  @Column(name = "bandwidth_limit_mbps")
  private Integer bandwidthLimitMbps;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public TenantNetworkPolicyEntity() {}

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

  public TenantEntity getTenant() {
    return tenant;
  }

  public void setTenant(TenantEntity tenant) {
    this.tenant = tenant;
  }

  public int getMaxVpcs() {
    return maxVpcs;
  }

  public void setMaxVpcs(int maxVpcs) {
    this.maxVpcs = maxVpcs;
  }

  public int getMaxPublicIps() {
    return maxPublicIps;
  }

  public void setMaxPublicIps(int maxPublicIps) {
    this.maxPublicIps = maxPublicIps;
  }

  public int getMaxSubnetsPerVpc() {
    return maxSubnetsPerVpc;
  }

  public void setMaxSubnetsPerVpc(int maxSubnetsPerVpc) {
    this.maxSubnetsPerVpc = maxSubnetsPerVpc;
  }

  public boolean isVpnAllowed() {
    return vpnAllowed;
  }

  public void setVpnAllowed(boolean vpnAllowed) {
    this.vpnAllowed = vpnAllowed;
  }

  public boolean isPeeringAllowed() {
    return peeringAllowed;
  }

  public void setPeeringAllowed(boolean peeringAllowed) {
    this.peeringAllowed = peeringAllowed;
  }

  public boolean isHaNetworkingAllowed() {
    return haNetworkingAllowed;
  }

  public void setHaNetworkingAllowed(boolean haNetworkingAllowed) {
    this.haNetworkingAllowed = haNetworkingAllowed;
  }

  public Integer getBandwidthLimitMbps() {
    return bandwidthLimitMbps;
  }

  public void setBandwidthLimitMbps(Integer bandwidthLimitMbps) {
    this.bandwidthLimitMbps = bandwidthLimitMbps;
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
