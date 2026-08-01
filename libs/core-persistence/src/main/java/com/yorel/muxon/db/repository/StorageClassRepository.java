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

import com.yorel.muxon.db.model.StorageClassEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for storage class entity operations.
 *
 * <p>Storage classes define user-facing storage capabilities and features. They are mapped to
 * provider-specific backends via ProviderStorageMappingEntity.
 */
public interface StorageClassRepository extends JpaRepository<StorageClassEntity, String> {

  Optional<StorageClassEntity> findByName(String name);

  List<StorageClassEntity> findByType(String type);

  List<StorageClassEntity> findByTier(String tier);

  @Query(
      value =
          "SELECT * FROM storage_classes sc WHERE sc.allowed_providers IS NOT NULL "
              + "AND sc.allowed_providers @> jsonb_build_array(CAST(:providerId AS text))",
      nativeQuery = true)
  List<StorageClassEntity> findByAllowedProvider(@Param("providerId") String providerId);
}
