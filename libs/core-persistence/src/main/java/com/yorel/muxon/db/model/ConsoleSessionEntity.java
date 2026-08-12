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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

@Entity
@Table(
    name = "console_sessions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_console_sessions_vm_user",
            columnNames = {"vm_id", "user_id"}))
public class ConsoleSessionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false)
  private UUID id;

  @Column(name = "vm_id", nullable = false)
  private UUID vmId;

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "token", nullable = false, unique = true)
  private String token;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "console_type", nullable = false, columnDefinition = "vm_console_type")
  private ConsoleSessionConsoleType consoleType;

  @Column(name = "hypervisor_host", nullable = false)
  private String hypervisorHost;

  @Column(name = "hypervisor_port", nullable = false)
  private int hypervisorPort;

  @Column(name = "hypervisor_password")
  private String hypervisorPassword;

  @Column(name = "tls", nullable = false)
  private boolean tls;

  @Column(name = "upstream_ws_url")
  private String upstreamWsUrl;

  @Column(name = "upstream_ws_cookie")
  private String upstreamWsCookie;

  @Column(name = "upstream_ws_csrf")
  private String upstreamWsCsrf;

  @Column(name = "upstream_ws_authorization")
  private String upstreamWsAuthorization;

  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "status", nullable = false, columnDefinition = "console_session_status")
  private ConsoleSessionStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "last_activity_at", nullable = false)
  private Instant lastActivityAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  @PrePersist
  void prePersist() {
    Instant now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (lastActivityAt == null) {
      lastActivityAt = now;
    }
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getVmId() {
    return vmId;
  }

  public void setVmId(UUID vmId) {
    this.vmId = vmId;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public ConsoleSessionConsoleType getConsoleType() {
    return consoleType;
  }

  public void setConsoleType(ConsoleSessionConsoleType consoleType) {
    this.consoleType = consoleType;
  }

  public String getHypervisorHost() {
    return hypervisorHost;
  }

  public void setHypervisorHost(String hypervisorHost) {
    this.hypervisorHost = hypervisorHost;
  }

  public int getHypervisorPort() {
    return hypervisorPort;
  }

  public void setHypervisorPort(int hypervisorPort) {
    this.hypervisorPort = hypervisorPort;
  }

  public String getHypervisorPassword() {
    return hypervisorPassword;
  }

  public void setHypervisorPassword(String hypervisorPassword) {
    this.hypervisorPassword = hypervisorPassword;
  }

  public boolean isTls() {
    return tls;
  }

  public void setTls(boolean tls) {
    this.tls = tls;
  }

  public String getUpstreamWsUrl() {
    return upstreamWsUrl;
  }

  public void setUpstreamWsUrl(String upstreamWsUrl) {
    this.upstreamWsUrl = upstreamWsUrl;
  }

  public String getUpstreamWsCookie() {
    return upstreamWsCookie;
  }

  public void setUpstreamWsCookie(String upstreamWsCookie) {
    this.upstreamWsCookie = upstreamWsCookie;
  }

  public String getUpstreamWsCsrf() {
    return upstreamWsCsrf;
  }

  public void setUpstreamWsCsrf(String upstreamWsCsrf) {
    this.upstreamWsCsrf = upstreamWsCsrf;
  }

  public String getUpstreamWsAuthorization() {
    return upstreamWsAuthorization;
  }

  public void setUpstreamWsAuthorization(String upstreamWsAuthorization) {
    this.upstreamWsAuthorization = upstreamWsAuthorization;
  }

  public ConsoleSessionStatus getStatus() {
    return status;
  }

  public void setStatus(ConsoleSessionStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getLastActivityAt() {
    return lastActivityAt;
  }

  public void setLastActivityAt(Instant lastActivityAt) {
    this.lastActivityAt = lastActivityAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(Instant closedAt) {
    this.closedAt = closedAt;
  }
}
