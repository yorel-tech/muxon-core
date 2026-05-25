package com.sal.muxon.db.repository;

import com.sal.muxon.db.model.StorageOverrideEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for storage override entity operations.
 * <p>
 * Manages manual storage class to provider storage mappings that bypass
 * the automatic scheduler.
 * </p>
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
