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

import com.yorel.muxon.db.model.VolumeEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository for volume entity operations.
 *
 * <p>Provides CRUD operations and queries for volumes with soft delete support. All query methods
 * exclude soft-deleted volumes (deletedAt IS NULL).
 */
public interface VolumeRepository extends JpaRepository<VolumeEntity, UUID> {

  Optional<VolumeEntity> findByIdAndDeletedAtIsNull(UUID id);

  Page<VolumeEntity> findByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId, Pageable pageable);

  Page<VolumeEntity> findByWorkspaceIdAndStorageClassAndDeletedAtIsNull(
      UUID workspaceId, String storageClass, Pageable pageable);

  Page<VolumeEntity> findByProviderIdAndDeletedAtIsNull(UUID providerId, Pageable pageable);

  Page<VolumeEntity> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);

  Optional<VolumeEntity> findByProviderIdAndProviderVolumeId(
      UUID providerId, String providerVolumeId);

  List<VolumeEntity> findByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);

  long countByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);

  long countByProviderIdAndDeletedAtIsNull(UUID providerId);

  /**
   * Calculate total storage size allocated to a workspace.
   *
   * <p>Sums the size of all non-deleted volumes in a workspace. Useful for quota enforcement and
   * capacity reporting.
   *
   * @param workspaceId workspace identifier
   * @return total size in bytes, or null if no volumes exist
   */
  @Query(
      "SELECT SUM(v.sizeBytes) FROM VolumeEntity v WHERE v.workspaceId = :workspaceId AND v.deletedAt IS NULL")
  Long sumSizeByWorkspaceId(UUID workspaceId);
}
