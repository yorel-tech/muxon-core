package com.yorel.muxon.services.storage;

import com.yorel.muxon.db.model.ProviderStorageMappingEntity;
import com.yorel.muxon.db.model.StorageClassEntity;
import com.yorel.muxon.db.repository.ProviderStorageMappingRepository;
import com.yorel.muxon.db.repository.StorageClassRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for resolving storage classes to provider-specific storage backends.
 * <p>
 * This service bridges the gap between user-facing storage class names
 * (e.g., "fast-ssd", "balanced") and provider-specific storage implementations.
 * It handles storage class validation, provider mapping resolution, and
 * storage class management.
 * </p>
 * <p>
 * Example: When a user requests a "fast-ssd" volume, this service determines
 * which provider backend to use (e.g., Ceph RBD pool "ssd_pool" for Libvirt,
 * or ZFS dataset "tank/ssd" for Proxmox).
 * </p>
 */
@Service
public class StorageClassResolutionService {

    private static final Logger log = LoggerFactory.getLogger(StorageClassResolutionService.class);

    private final StorageClassRepository storageClassRepository;
    private final ProviderStorageMappingRepository providerStorageMappingRepository;

    public StorageClassResolutionService(
            StorageClassRepository storageClassRepository,
            ProviderStorageMappingRepository providerStorageMappingRepository) {
        this.storageClassRepository = storageClassRepository;
        this.providerStorageMappingRepository = providerStorageMappingRepository;
    }

    public Optional<StorageClassEntity> resolveStorageClass(String storageClassName) {
        return storageClassRepository.findByName(storageClassName);
    }

    /**
     * Resolve the provider-specific storage mapping for a storage class.
     * <p>
     * Returns the enabled mapping that connects a storage class to a specific
     * provider's backend configuration. The mapping includes backend type
     * (e.g., "ceph-rbd", "zfs") and provider-specific configuration.
     * </p>
     *
     * @param storageClassName storage class name
     * @param providerId provider identifier
     * @return optional provider mapping if found and enabled
     */
    public Optional<ProviderStorageMappingEntity> resolveProviderMapping(
            String storageClassName, UUID providerId) {
        return providerStorageMappingRepository.findEnabledMapping(storageClassName, providerId);
    }

    /**
     * Get all available provider mappings for a storage class.
     * <p>
     * Returns all enabled mappings ordered by priority (highest first).
     * Used when multiple providers can fulfill a storage class request.
     * </p>
     *
     * @param storageClassName storage class name
     * @return list of enabled mappings ordered by priority
     */
    public List<ProviderStorageMappingEntity> getAvailableMappings(String storageClassName) {
        return providerStorageMappingRepository.findEnabledMappingsByStorageClass(storageClassName);
    }

    /**
     * Check if a storage class is supported by a specific provider.
     * <p>
     * Validates that both the storage class exists and has an enabled
     * mapping to the specified provider.
     * </p>
     *
     * @param storageClassName storage class name
     * @param providerId provider identifier
     * @return true if the provider supports the storage class
     */
    public boolean isStorageClassSupported(String storageClassName, UUID providerId) {
        Optional<StorageClassEntity> storageClass = resolveStorageClass(storageClassName);
        if (storageClass.isEmpty()) {
            log.warn("Storage class {} not found", storageClassName);
            return false;
        }

        Optional<ProviderStorageMappingEntity> mapping = 
            resolveProviderMapping(storageClassName, providerId);
        
        return mapping.isPresent();
    }

    public List<StorageClassEntity> getAllStorageClasses() {
        return storageClassRepository.findAll();
    }

    public List<StorageClassEntity> getStorageClassesByType(String type) {
        return storageClassRepository.findByType(type);
    }

    public StorageClassEntity createStorageClass(StorageClassEntity storageClass) {
        return storageClassRepository.save(storageClass);
    }

    public ProviderStorageMappingEntity createProviderMapping(
            ProviderStorageMappingEntity mapping) {
        return providerStorageMappingRepository.save(mapping);
    }
}
