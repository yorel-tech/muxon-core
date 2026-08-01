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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity tracking volume attachments to resources (VMs, pods, containers).
 *
 * <p>This entity maintains a history of all volume attachments, including both active and
 * historical attachments. Active attachments have detached_at = null.
 *
 * <p>A volume can only be attached to one resource at a time. The unique constraint ensures this
 * while allowing historical tracking of previous attachments.
 */
@Entity
@Table(
    name = "volume_attachments",
    indexes = {
      @Index(name = "idx_va_volume_id", columnList = "volume_id"),
      @Index(name = "idx_resource", columnList = "resource_type,resource_id")
    },
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_volume_resource_detached",
          columnNames = {"volume_id", "resource_id", "detached_at"})
    })
public class VolumeAttachmentEntity {

  @Id
  @Column(name = "id", updatable = false)
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "volume_id", nullable = false)
  private UUID volumeId;

  @Column(name = "resource_type", nullable = false, length = 32)
  private String resourceType;

  @Column(name = "resource_id", nullable = false)
  private UUID resourceId;

  @Column(name = "device", length = 64)
  private String device;

  @Column(name = "attached_at", nullable = false)
  private Instant attachedAt;

  @Column(name = "detached_at")
  private Instant detachedAt;

  @Version private Long version;

  public VolumeAttachmentEntity() {}

  @PrePersist
  protected void onCreate() {
    this.attachedAt = Instant.now();
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

  public String getResourceType() {
    return resourceType;
  }

  public void setResourceType(String resourceType) {
    this.resourceType = resourceType;
  }

  public UUID getResourceId() {
    return resourceId;
  }

  public void setResourceId(UUID resourceId) {
    this.resourceId = resourceId;
  }

  public String getDevice() {
    return device;
  }

  public void setDevice(String device) {
    this.device = device;
  }

  public Instant getAttachedAt() {
    return attachedAt;
  }

  public void setAttachedAt(Instant attachedAt) {
    this.attachedAt = attachedAt;
  }

  public Instant getDetachedAt() {
    return detachedAt;
  }

  public void setDetachedAt(Instant detachedAt) {
    this.detachedAt = detachedAt;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
