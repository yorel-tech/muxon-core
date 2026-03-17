package com.onetattva.infron.core.services.storage;

import com.onetattva.infron.db.model.ProviderStorageMappingEntity;
import com.onetattva.infron.db.repository.ProviderStorageMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing provider storage mappings.
 * <p>
 * Handles the configuration of storage class mappings to provider-specific
 * storage backends. Administrators use this service to define how storage
 * classes (e.g., "fast-ssd") map to actual storage implementations on each
 * provider (e.g., Ceph RBD pool, LVM volume group, ZFS dataset).
 * </p>
 * <p>
 * Example mapping:
 * <pre>
 * Storage Class: "fast-ssd"
 * Provider: libvirt-node-01
 * Backend Type: "ceph-rbd"
 * Backend Config: {
 *   "pool": "ssd_pool",
 *   "monitors": ["192.168.1.10:6789"],
 *   "user": "admin"
 * }
 * </pre>
 * </p>
 */
@Service
public class ProviderStorageMappingService {

    private static final Logger log = LoggerFactory.getLogger(ProviderStorageMappingService.class);

    private final ProviderStorageMappingRepository mappingRepository;

    public ProviderStorageMappingService(ProviderStorageMappingRepository mappingRepository) {
        this.mappingRepository = mappingRepository;
    }

    /**
     * Create a new provider storage mapping.
     * <p>
     * Defines how a storage class maps to a specific provider's storage backend.
     * The mapping includes backend type (e.g., "ceph-rbd", "lvm", "zfs") and
     * provider-specific configuration parameters.
     * </p>
     *
     * @param storageClass storage class name
     * @param providerId provider identifier
     * @param backendType backend type (e.g., "ceph-rbd", "lvm", "zfs")
     * @param backendConfig provider-specific configuration
     * @param priority mapping priority (higher = preferred)
     * @return created mapping entity
     */
    @Transactional
    public ProviderStorageMappingEntity createMapping(
            String storageClass,
            UUID providerId,
            String backendType,
            Map<String, Object> backendConfig,
            Integer priority) {
        
        log.info("Creating storage mapping: storageClass={}, providerId={}, backendType={}",
            storageClass, providerId, backendType);

        Optional<ProviderStorageMappingEntity> existing = 
            mappingRepository.findByStorageClassAndProviderId(storageClass, providerId);
        
        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                "Mapping already exists for storage class " + storageClass + 
                " and provider " + providerId);
        }

        ProviderStorageMappingEntity mapping = new ProviderStorageMappingEntity();
        mapping.setStorageClass(storageClass);
        mapping.setProviderId(providerId);
        mapping.setBackendType(backendType);
        mapping.setBackendConfig(backendConfig);
        mapping.setPriority(priority != null ? priority : 100);
        mapping.setEnabled(true);

        return mappingRepository.save(mapping);
    }

    /**
     * Update an existing provider storage mapping.
     *
     * @param mappingId mapping identifier
     * @param backendConfig updated backend configuration
     * @param priority updated priority
     * @param enabled whether the mapping is enabled
     * @return updated mapping entity
     */
    @Transactional
    public ProviderStorageMappingEntity updateMapping(
            UUID mappingId,
            Map<String, Object> backendConfig,
            Integer priority,
            Boolean enabled) {
        
        ProviderStorageMappingEntity mapping = mappingRepository.findById(mappingId)
            .orElseThrow(() -> new IllegalArgumentException("Mapping not found: " + mappingId));

        if (backendConfig != null) {
            mapping.setBackendConfig(backendConfig);
        }
        if (priority != null) {
            mapping.setPriority(priority);
        }
        if (enabled != null) {
            mapping.setEnabled(enabled);
        }

        log.info("Updated storage mapping: {}", mappingId);
        return mappingRepository.save(mapping);
    }

    /**
     * Delete a provider storage mapping.
     *
     * @param mappingId mapping identifier
     */
    @Transactional
    public void deleteMapping(UUID mappingId) {
        ProviderStorageMappingEntity mapping = mappingRepository.findById(mappingId)
            .orElseThrow(() -> new IllegalArgumentException("Mapping not found: " + mappingId));

        mappingRepository.delete(mapping);
        log.info("Deleted storage mapping: {}", mappingId);
    }

    /**
     * Enable or disable a mapping without deleting it.
     *
     * @param mappingId mapping identifier
     * @param enabled whether to enable or disable
     */
    @Transactional
    public void setMappingEnabled(UUID mappingId, boolean enabled) {
        ProviderStorageMappingEntity mapping = mappingRepository.findById(mappingId)
            .orElseThrow(() -> new IllegalArgumentException("Mapping not found: " + mappingId));

        mapping.setEnabled(enabled);
        mappingRepository.save(mapping);
        
        log.info("Set mapping {} enabled={}", mappingId, enabled);
    }

    /**
     * Get all mappings for a provider.
     *
     * @param providerId provider identifier
     * @return list of mappings
     */
    public List<ProviderStorageMappingEntity> getMappingsByProvider(UUID providerId) {
        return mappingRepository.findByProviderId(providerId);
    }

    /**
     * Get all enabled mappings for a provider.
     *
     * @param providerId provider identifier
     * @return list of enabled mappings
     */
    public List<ProviderStorageMappingEntity> getEnabledMappingsByProvider(UUID providerId) {
        return mappingRepository.findByProviderIdAndEnabled(providerId, true);
    }

    /**
     * Get all mappings for a storage class.
     *
     * @param storageClass storage class name
     * @return list of mappings
     */
    public List<ProviderStorageMappingEntity> getMappingsByStorageClass(String storageClass) {
        return mappingRepository.findByStorageClass(storageClass);
    }

    /**
     * Get all enabled mappings for a storage class, ordered by priority.
     *
     * @param storageClass storage class name
     * @return list of enabled mappings ordered by priority (highest first)
     */
    public List<ProviderStorageMappingEntity> getEnabledMappingsByStorageClass(String storageClass) {
        return mappingRepository.findEnabledMappingsByStorageClass(storageClass);
    }

    /**
     * Get a specific mapping by storage class and provider.
     *
     * @param storageClass storage class name
     * @param providerId provider identifier
     * @return optional mapping
     */
    public Optional<ProviderStorageMappingEntity> getMapping(String storageClass, UUID providerId) {
        return mappingRepository.findByStorageClassAndProviderId(storageClass, providerId);
    }

    /**
     * Get an enabled mapping by storage class and provider.
     *
     * @param storageClass storage class name
     * @param providerId provider identifier
     * @return optional enabled mapping
     */
    public Optional<ProviderStorageMappingEntity> getEnabledMapping(String storageClass, UUID providerId) {
        return mappingRepository.findEnabledMapping(storageClass, providerId);
    }

    /**
     * Create a Ceph RBD storage mapping.
     *
     * @param storageClass storage class name
     * @param providerId provider identifier
     * @param poolName Ceph pool name
     * @param monitors list of Ceph monitor addresses
     * @param user Ceph user name
     * @param priority mapping priority
     * @return created mapping
     */
    @Transactional
    public ProviderStorageMappingEntity createCephRbdMapping(
            String storageClass,
            UUID providerId,
            String poolName,
            List<String> monitors,
            String user,
            Integer priority) {
        
        Map<String, Object> config = Map.of(
            "pool", poolName,
            "monitors", monitors,
            "user", user
        );

        return createMapping(storageClass, providerId, "ceph-rbd", config, priority);
    }

    /**
     * Create an LVM storage mapping.
     *
     * @param storageClass storage class name
     * @param providerId provider identifier
     * @param volumeGroup LVM volume group name
     * @param thinPool optional thin pool name
     * @param priority mapping priority
     * @return created mapping
     */
    @Transactional
    public ProviderStorageMappingEntity createLvmMapping(
            String storageClass,
            UUID providerId,
            String volumeGroup,
            String thinPool,
            Integer priority) {
        
        Map<String, Object> config = thinPool != null
            ? Map.of("volumeGroup", volumeGroup, "thinPool", thinPool)
            : Map.of("volumeGroup", volumeGroup);

        return createMapping(storageClass, providerId, "lvm", config, priority);
    }

    /**
     * Create a ZFS storage mapping.
     *
     * @param storageClass storage class name
     * @param providerId provider identifier
     * @param poolName ZFS pool name
     * @param compression ZFS compression algorithm (e.g., "lz4", "zstd")
     * @param dedup whether to enable deduplication
     * @param priority mapping priority
     * @return created mapping
     */
    @Transactional
    public ProviderStorageMappingEntity createZfsMapping(
            String storageClass,
            UUID providerId,
            String poolName,
            String compression,
            Boolean dedup,
            Integer priority) {
        
        Map<String, Object> config = Map.of(
            "pool", poolName,
            "compression", compression != null ? compression : "lz4",
            "dedup", dedup != null ? dedup : false
        );

        return createMapping(storageClass, providerId, "zfs", config, priority);
    }
}
