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
@Table(name = "subnet")
public class SubnetEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vpc_id", nullable = false)
  private VpcEntity vpc;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "datacenter_id", nullable = false)
  private DatacenterEntity datacenter;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "fabric_network_id")
  private FabricNetworkEntity fabricNetwork;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String cidr;

  @Column(name = "cidr_v6")
  private String cidrV6;

  @Column(name = "gateway_ip")
  private String gatewayIp;

  @Column(name = "dns_servers", columnDefinition = "TEXT[]")
  @JdbcTypeCode(SqlTypes.ARRAY)
  private List<String> dnsServers;

  @Column(name = "public", nullable = false)
  private boolean isPublic = false;

  @Column(name = "availability_zone")
  private String availabilityZone;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "route_table_id")
  private RouteTableEntity routeTable;

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(nullable = false)
  private SubnetStatus status = SubnetStatus.PENDING;

  @Column(name = "provider_handle")
  private String providerHandle;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public SubnetEntity() {}

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

  public VpcEntity getVpc() {
    return vpc;
  }

  public void setVpc(VpcEntity vpc) {
    this.vpc = vpc;
  }

  public DatacenterEntity getDatacenter() {
    return datacenter;
  }

  public void setDatacenter(DatacenterEntity datacenter) {
    this.datacenter = datacenter;
  }

  public FabricNetworkEntity getFabricNetwork() {
    return fabricNetwork;
  }

  public void setFabricNetwork(FabricNetworkEntity fabricNetwork) {
    this.fabricNetwork = fabricNetwork;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCidr() {
    return cidr;
  }

  public void setCidr(String cidr) {
    this.cidr = cidr;
  }

  public String getCidrV6() {
    return cidrV6;
  }

  public void setCidrV6(String cidrV6) {
    this.cidrV6 = cidrV6;
  }

  public String getGatewayIp() {
    return gatewayIp;
  }

  public void setGatewayIp(String gatewayIp) {
    this.gatewayIp = gatewayIp;
  }

  public List<String> getDnsServers() {
    return dnsServers;
  }

  public void setDnsServers(List<String> dnsServers) {
    this.dnsServers = dnsServers;
  }

  public boolean isPublic() {
    return isPublic;
  }

  public void setPublic(boolean aPublic) {
    isPublic = aPublic;
  }

  public String getAvailabilityZone() {
    return availabilityZone;
  }

  public void setAvailabilityZone(String availabilityZone) {
    this.availabilityZone = availabilityZone;
  }

  public RouteTableEntity getRouteTable() {
    return routeTable;
  }

  public void setRouteTable(RouteTableEntity routeTable) {
    this.routeTable = routeTable;
  }

  public SubnetStatus getStatus() {
    return status;
  }

  public void setStatus(SubnetStatus status) {
    this.status = status;
  }

  public String getProviderHandle() {
    return providerHandle;
  }

  public void setProviderHandle(String providerHandle) {
    this.providerHandle = providerHandle;
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

  public enum SubnetStatus {
    PENDING,
    ACTIVE,
    ERROR,
    DELETING
  }
}
