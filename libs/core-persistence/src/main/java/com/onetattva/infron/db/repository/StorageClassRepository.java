package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.StorageClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository for storage class entity operations.
 * <p>
 * Storage classes define user-facing storage capabilities and features.
 * They are mapped to provider-specific backends via ProviderStorageMappingEntity.
 * </p>
 */
public interface StorageClassRepository extends JpaRepository<StorageClassEntity, String> {

    Optional<StorageClassEntity> findByName(String name);

    List<StorageClassEntity> findByType(String type);

    List<StorageClassEntity> findByTier(String tier);

    @Query("SELECT sc FROM StorageClassEntity sc WHERE :providerId MEMBER OF sc.allowedProviders")
    List<StorageClassEntity> findByAllowedProvider(String providerId);
}
