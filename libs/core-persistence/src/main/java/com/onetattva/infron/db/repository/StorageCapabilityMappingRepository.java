package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.StorageCapabilityMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for storage capability mapping entity operations.
 * <p>
 * Manages mappings between Infron generic capabilities and provider-specific
 * capability terminology.
 * </p>
 */
public interface StorageCapabilityMappingRepository extends JpaRepository<StorageCapabilityMappingEntity, UUID> {

    List<StorageCapabilityMappingEntity> findByInfronCapability(String infronCapability);

    List<StorageCapabilityMappingEntity> findByProviderType(String providerType);

    List<StorageCapabilityMappingEntity> findByInfronCapabilityAndProviderType(
        String infronCapability, String providerType);

    Optional<StorageCapabilityMappingEntity> findByInfronCapabilityAndProviderTypeAndProviderCapability(
        String infronCapability, String providerType, String providerCapability);
}
