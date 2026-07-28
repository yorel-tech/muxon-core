package com.scal.muxon.db.repository;

import com.scal.muxon.db.model.StorageClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query(
            value =
                    "SELECT * FROM storage_classes sc WHERE sc.allowed_providers IS NOT NULL "
                            + "AND sc.allowed_providers @> jsonb_build_array(CAST(:providerId AS text))",
            nativeQuery = true)
    List<StorageClassEntity> findByAllowedProvider(@Param("providerId") String providerId);
}
