package com.yorel.muxon.db.repository;

import com.yorel.muxon.db.model.ProviderStorageMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for provider storage mapping entity operations.
 * <p>
 * Manages mappings between storage classes and provider-specific backends.
 * Supports priority-based selection and enable/disable without deletion.
 * </p>
 */
public interface ProviderStorageMappingRepository extends JpaRepository<ProviderStorageMappingEntity, UUID> {

    List<ProviderStorageMappingEntity> findByStorageClass(String storageClass);

    List<ProviderStorageMappingEntity> findByProviderId(UUID providerId);

    Optional<ProviderStorageMappingEntity> findByStorageClassAndProviderId(
        String storageClass, UUID providerId);

    /**
     * Find all enabled mappings for a storage class, ordered by priority.
     * <p>
     * Returns mappings in descending priority order (highest priority first).
     * Used for provider selection when multiple providers support the same storage class.
     * </p>
     *
     * @param storageClass storage class name
     * @return list of enabled mappings ordered by priority (descending)
     */
    @Query("SELECT psm FROM ProviderStorageMappingEntity psm WHERE psm.storageClass = :storageClass AND psm.enabled = true ORDER BY psm.priority DESC")
    List<ProviderStorageMappingEntity> findEnabledMappingsByStorageClass(String storageClass);

    @Query("SELECT psm FROM ProviderStorageMappingEntity psm WHERE psm.storageClass = :storageClass AND psm.providerId = :providerId AND psm.enabled = true")
    Optional<ProviderStorageMappingEntity> findEnabledMapping(String storageClass, UUID providerId);

    List<ProviderStorageMappingEntity> findByProviderIdAndEnabled(UUID providerId, boolean enabled);
}
