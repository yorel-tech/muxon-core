package com.sal.muxon.db.repository;

import com.sal.muxon.db.model.StorageCapabilityMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for storage capability mapping entity operations.
 * <p>
 * Manages mappings between Muxon generic capabilities and provider-specific
 * capability terminology.
 * </p>
 */
public interface StorageCapabilityMappingRepository extends JpaRepository<StorageCapabilityMappingEntity, UUID> {

    List<StorageCapabilityMappingEntity> findByMuxonCapability(String muxonCapability);

    List<StorageCapabilityMappingEntity> findByProviderType(String providerType);

    List<StorageCapabilityMappingEntity> findByMuxonCapabilityAndProviderType(
        String muxonCapability, String providerType);

    Optional<StorageCapabilityMappingEntity> findByMuxonCapabilityAndProviderTypeAndProviderCapability(
        String muxonCapability, String providerType, String providerCapability);
}
