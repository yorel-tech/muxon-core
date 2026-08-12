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
package com.yorel.muxon.db.repository;

import com.yorel.muxon.db.model.VolumeAttachmentEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository for volume attachment entity operations.
 *
 * <p>Tracks volume attachments to resources (VMs, pods, containers). Active attachments have
 * detachedAt = null. Historical attachments are preserved for audit purposes.
 */
public interface VolumeAttachmentRepository extends JpaRepository<VolumeAttachmentEntity, UUID> {

  List<VolumeAttachmentEntity> findByVolumeIdAndDetachedAtIsNull(UUID volumeId);

  Optional<VolumeAttachmentEntity> findByVolumeIdAndResourceIdAndDetachedAtIsNull(
      UUID volumeId, UUID resourceId);

  List<VolumeAttachmentEntity> findByResourceTypeAndResourceIdAndDetachedAtIsNull(
      String resourceType, UUID resourceId);

  @Query(
      "SELECT va FROM VolumeAttachmentEntity va WHERE va.volumeId = :volumeId AND va.detachedAt IS NULL")
  Optional<VolumeAttachmentEntity> findActiveAttachmentByVolumeId(UUID volumeId);

  boolean existsByVolumeIdAndDetachedAtIsNull(UUID volumeId);
}
