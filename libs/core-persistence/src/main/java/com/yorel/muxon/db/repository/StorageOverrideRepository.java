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

import com.yorel.muxon.db.model.StorageOverrideEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for storage override entity operations.
 *
 * <p>Manages manual storage class to provider storage mappings that bypass the automatic scheduler.
 */
public interface StorageOverrideRepository extends JpaRepository<StorageOverrideEntity, UUID> {

  List<StorageOverrideEntity> findByStorageClassName(String storageClassName);

  List<StorageOverrideEntity> findByProviderType(String providerType);

  Optional<StorageOverrideEntity> findByStorageClassNameAndProviderType(
      String storageClassName, String providerType);

  /**
   * Check if an override exists for a storage class and provider type.
   *
   * @param storageClassName storage class name
   * @param providerType provider type
   * @return true if override exists
   */
  boolean existsByStorageClassNameAndProviderType(String storageClassName, String providerType);

  void deleteByStorageClassName(String storageClassName);
}
