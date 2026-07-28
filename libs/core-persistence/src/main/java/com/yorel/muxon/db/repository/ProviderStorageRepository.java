package com.yorel.muxon.db.repository;

import com.yorel.muxon.db.model.ProviderStorageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for provider storage entity operations.
 * <p>
 * Manages normalized provider storage pools/classes discovered from
 * infrastructure providers (Libvirt, Proxmox).
 * </p>
 */
public interface ProviderStorageRepository extends JpaRepository<ProviderStorageEntity, UUID> {

    List<ProviderStorageEntity> findByProviderId(UUID providerId);

    List<ProviderStorageEntity> findByProviderIdAndEnabled(UUID providerId, boolean enabled);

    List<ProviderStorageEntity> findByProviderType(String providerType);

    Optional<ProviderStorageEntity> findByProviderIdAndExternalId(UUID providerId, String externalId);

    List<ProviderStorageEntity> findByProviderIdAndName(UUID providerId, String name);

    List<ProviderStorageEntity> findByDatacenterId(UUID datacenterId);

    List<ProviderStorageEntity> findByEnabled(boolean enabled);

    /**
     * Find provider storage by name across all providers of a specific type.
     *
     * @param providerType provider type (libvirt, proxmox)
     * @param name storage name
     * @return list of matching provider storage
     */
    List<ProviderStorageEntity> findByProviderTypeAndName(String providerType, String name);

    /**
     * Find enabled provider storage that matches given capabilities.
     * Uses JSONB containment operator (@>) to check if provider storage
     * capabilities contain all required capabilities.
     *
     * @param capabilities required capabilities as JSONB
     * @return list of matching provider storage
     */
    @Query(value = "SELECT * FROM provider_storages ps WHERE ps.enabled = true AND ps.capabilities @> CAST(:capabilities AS jsonb)", 
           nativeQuery = true)
    List<ProviderStorageEntity> findByCapabilitiesContaining(@Param("capabilities") String capabilities);

    /**
     * Find provider storage by storage type.
     *
     * @param storageType storage type (ceph-rbd, lvm, zfs, etc.)
     * @return list of matching provider storage
     */
    List<ProviderStorageEntity> findByStorageType(String storageType);

    /**
     * Find provider storage by provider and storage type.
     *
     * @param providerId provider ID
     * @param storageType storage type
     * @return list of matching provider storage
     */
    List<ProviderStorageEntity> findByProviderIdAndStorageType(UUID providerId, String storageType);

    /**
     * Delete all provider storage for a specific provider.
     * Used when re-syncing provider storage.
     *
     * @param providerId provider ID
     */
    void deleteByProviderId(UUID providerId);
}
