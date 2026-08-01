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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity representing a point-in-time snapshot of a volume.
 *
 * <p>Snapshots capture the state of a volume at a specific moment and can be used for backup,
 * restore, or clone operations. Snapshots can be marked as immutable with a retention period to
 * prevent accidental deletion (compliance/WORM).
 *
 * <p>Lifecycle states: creating → available → deleting → deleted
 */
@Entity
@Table(
    name = "snapshots",
    indexes = {
      @Index(name = "idx_snapshots_volume_id", columnList = "volume_id"),
      @Index(name = "idx_snapshots_status", columnList = "status"),
      @Index(name = "idx_snapshots_immutable", columnList = "immutable")
    })
public class SnapshotEntity {

  @Id
  @Column(name = "id", updatable = false)
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "volume_id", nullable = false)
  private UUID volumeId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "size_bytes", nullable = false)
  private Long sizeBytes;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "provider_snapshot_id", nullable = false)
  private String providerSnapshotId;

  @Column(name = "immutable", nullable = false)
  private Boolean immutable = false;

  @Column(name = "retention_until")
  private Instant retentionUntil;

  @Column(name = "backup_policy_id", length = 64)
  private String backupPolicyId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "tags", columnDefinition = "jsonb")
  private Map<String, String> tags;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Version private Long version;

  public SnapshotEntity() {}

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

  public UUID getVolumeId() {
    return volumeId;
  }

  public void setVolumeId(UUID volumeId) {
    this.volumeId = volumeId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getSizeBytes() {
    return sizeBytes;
  }

  public void setSizeBytes(Long sizeBytes) {
    this.sizeBytes = sizeBytes;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getProviderSnapshotId() {
    return providerSnapshotId;
  }

  public void setProviderSnapshotId(String providerSnapshotId) {
    this.providerSnapshotId = providerSnapshotId;
  }

  public Boolean getImmutable() {
    return immutable;
  }

  public void setImmutable(Boolean immutable) {
    this.immutable = immutable;
  }

  public Instant getRetentionUntil() {
    return retentionUntil;
  }

  public void setRetentionUntil(Instant retentionUntil) {
    this.retentionUntil = retentionUntil;
  }

  public String getBackupPolicyId() {
    return backupPolicyId;
  }

  public void setBackupPolicyId(String backupPolicyId) {
    this.backupPolicyId = backupPolicyId;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  public void setTags(Map<String, String> tags) {
    this.tags = tags;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(Instant deletedAt) {
    this.deletedAt = deletedAt;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
