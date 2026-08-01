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
package com.yorel.muxon.services.storage.scheduler;

import com.yorel.muxon.db.model.ProviderStorageEntity;
import com.yorel.muxon.db.model.StorageOverrideEntity;
import com.yorel.muxon.db.repository.ProviderStorageRepository;
import com.yorel.muxon.db.repository.StorageOverrideRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Resolves storage overrides for manual storage class mappings.
 *
 * <p>Checks if a storage class has manual overrides that bypass the scheduler.
 */
@Component
public class StorageOverrideResolver {

  private static final Logger log = LoggerFactory.getLogger(StorageOverrideResolver.class);

  private final StorageOverrideRepository overrideRepository;
  private final ProviderStorageRepository providerStorageRepository;

  public StorageOverrideResolver(
      StorageOverrideRepository overrideRepository,
      ProviderStorageRepository providerStorageRepository) {
    this.overrideRepository = overrideRepository;
    this.providerStorageRepository = providerStorageRepository;
  }

  /**
   * Check if an override exists for a storage class and provider type.
   *
   * @param storageClassName storage class name
   * @param providerType provider type
   * @return true if override exists
   */
  public boolean hasOverride(String storageClassName, String providerType) {
    return overrideRepository.existsByStorageClassNameAndProviderType(
        storageClassName, providerType);
  }

  /**
   * Resolve storage override for a storage class and provider type.
   *
   * <p>Returns provider storage entries that match the override configuration.
   *
   * @param storageClassName storage class name
   * @param providerType provider type
   * @return list of provider storage entries from override
   */
  public List<ProviderStorageEntity> resolveOverride(String storageClassName, String providerType) {

    Optional<StorageOverrideEntity> overrideOpt =
        overrideRepository.findByStorageClassNameAndProviderType(storageClassName, providerType);

    if (overrideOpt.isEmpty()) {
      log.debug(
          "No override found for storage class {} and provider type {}",
          storageClassName,
          providerType);
      return List.of();
    }

    StorageOverrideEntity override = overrideOpt.get();
    List<String> storageNames = override.getProviderStorageNames();

    if (storageNames == null || storageNames.isEmpty()) {
      log.warn("Override for storage class {} has no storage names", storageClassName);
      return List.of();
    }

    List<ProviderStorageEntity> matchedStorage = new ArrayList<>();

    for (String storageName : storageNames) {
      List<ProviderStorageEntity> found =
          providerStorageRepository.findByProviderTypeAndName(providerType, storageName);

      for (ProviderStorageEntity storage : found) {
        if (storage.getEnabled()) {
          matchedStorage.add(storage);
        }
      }
    }

    log.info(
        "Resolved override for storage class {} to {} storage entries: {}",
        storageClassName,
        matchedStorage.size(),
        storageNames);

    return matchedStorage;
  }

  /**
   * Get all overrides for a storage class.
   *
   * @param storageClassName storage class name
   * @return list of storage overrides
   */
  public List<StorageOverrideEntity> getOverrides(String storageClassName) {
    return overrideRepository.findByStorageClassName(storageClassName);
  }
}
