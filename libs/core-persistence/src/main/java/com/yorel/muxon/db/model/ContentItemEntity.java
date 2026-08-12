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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "content_items")
public class ContentItemEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "library_id", nullable = false)
  private UUID libraryId;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "content_type", nullable = false, length = 64)
  private String contentType;

  @Column(name = "version_label", length = 128)
  private String versionLabel;

  @Column(name = "size_bytes")
  private Long sizeBytes;

  @Column(name = "checksum", length = 256)
  private String checksum;

  @Column(name = "checksum_algorithm", length = 32)
  private String checksumAlgorithm;

  @Column(name = "source_url")
  private String sourceUrl;

  @Column(name = "source_item_id")
  private UUID sourceItemId;

  @Column(name = "content_status", nullable = false, length = 32)
  private String contentStatus;

  @Column(name = "last_replicated_at")
  private Instant lastReplicatedAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private Map<String, String> metadata;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "template_spec", columnDefinition = "jsonb")
  private Map<String, Object> templateSpec;

  @Column(name = "provider_relative_path")
  private String providerRelativePath;

  @Column(name = "muxon_instance_segment", length = 64)
  private String muxonInstanceSegment;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private Long version;

  @PrePersist
  protected void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getLibraryId() {
    return libraryId;
  }

  public void setLibraryId(UUID libraryId) {
    this.libraryId = libraryId;
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

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getVersionLabel() {
    return versionLabel;
  }

  public void setVersionLabel(String versionLabel) {
    this.versionLabel = versionLabel;
  }

  public Long getSizeBytes() {
    return sizeBytes;
  }

  public void setSizeBytes(Long sizeBytes) {
    this.sizeBytes = sizeBytes;
  }

  public String getChecksum() {
    return checksum;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  public String getChecksumAlgorithm() {
    return checksumAlgorithm;
  }

  public void setChecksumAlgorithm(String checksumAlgorithm) {
    this.checksumAlgorithm = checksumAlgorithm;
  }

  public String getSourceUrl() {
    return sourceUrl;
  }

  public void setSourceUrl(String sourceUrl) {
    this.sourceUrl = sourceUrl;
  }

  public UUID getSourceItemId() {
    return sourceItemId;
  }

  public void setSourceItemId(UUID sourceItemId) {
    this.sourceItemId = sourceItemId;
  }

  public String getContentStatus() {
    return contentStatus;
  }

  public void setContentStatus(String contentStatus) {
    this.contentStatus = contentStatus;
  }

  public Instant getLastReplicatedAt() {
    return lastReplicatedAt;
  }

  public void setLastReplicatedAt(Instant lastReplicatedAt) {
    this.lastReplicatedAt = lastReplicatedAt;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  public Map<String, Object> getTemplateSpec() {
    return templateSpec;
  }

  public void setTemplateSpec(Map<String, Object> templateSpec) {
    this.templateSpec = templateSpec;
  }

  public String getProviderRelativePath() {
    return providerRelativePath;
  }

  public void setProviderRelativePath(String providerRelativePath) {
    this.providerRelativePath = providerRelativePath;
  }

  public String getMuxonInstanceSegment() {
    return muxonInstanceSegment;
  }

  public void setMuxonInstanceSegment(String muxonInstanceSegment) {
    this.muxonInstanceSegment = muxonInstanceSegment;
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

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
