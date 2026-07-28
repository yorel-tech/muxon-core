package com.yorel.muxon.controllers;

import com.yorel.muxon.db.model.StorageOverrideEntity;
import com.yorel.muxon.db.repository.StorageOverrideRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for storage override operations.
 * <p>
 * Provides endpoints to manage manual storage class to provider storage mappings.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/storage-overrides")
public class StorageOverrideController {

    private static final Logger log = LoggerFactory.getLogger(StorageOverrideController.class);

    private final StorageOverrideRepository overrideRepository;

    public StorageOverrideController(StorageOverrideRepository overrideRepository) {
        this.overrideRepository = overrideRepository;
    }

    /**
     * Get all storage overrides.
     *
     * @return list of all storage overrides
     */
    @GetMapping
    public ResponseEntity<List<StorageOverrideEntity>> getAllOverrides() {
        log.debug("Getting all storage overrides");
        List<StorageOverrideEntity> overrides = overrideRepository.findAll();
        return ResponseEntity.ok(overrides);
    }

    /**
     * Get storage override by ID.
     *
     * @param id override ID
     * @return storage override entity
     */
    @GetMapping("/{id}")
    public ResponseEntity<StorageOverrideEntity> getOverride(@PathVariable UUID id) {
        log.debug("Getting storage override {}", id);
        Optional<StorageOverrideEntity> override = overrideRepository.findById(id);
        return override.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get overrides for a storage class.
     *
     * @param storageClassName storage class name
     * @return list of overrides for the storage class
     */
    @GetMapping("/storage-class/{storageClassName}")
    public ResponseEntity<List<StorageOverrideEntity>> getOverridesByStorageClass(
            @PathVariable String storageClassName) {
        log.debug("Getting overrides for storage class {}", storageClassName);
        List<StorageOverrideEntity> overrides = 
            overrideRepository.findByStorageClassName(storageClassName);
        return ResponseEntity.ok(overrides);
    }

    /**
     * Get overrides for a provider type.
     *
     * @param providerType provider type
     * @return list of overrides for the provider type
     */
    @GetMapping("/provider-type/{providerType}")
    public ResponseEntity<List<StorageOverrideEntity>> getOverridesByProviderType(
            @PathVariable String providerType) {
        log.debug("Getting overrides for provider type {}", providerType);
        List<StorageOverrideEntity> overrides = 
            overrideRepository.findByProviderType(providerType);
        return ResponseEntity.ok(overrides);
    }

    /**
     * Create a new storage override.
     *
     * @param override storage override to create
     * @return created storage override
     */
    @PostMapping
    public ResponseEntity<StorageOverrideEntity> createOverride(
            @RequestBody StorageOverrideEntity override) {
        log.info("Creating storage override: class={}, provider={}", 
            override.getStorageClassName(), override.getProviderType());
        
        // Check if override already exists
        if (overrideRepository.existsByStorageClassNameAndProviderType(
                override.getStorageClassName(), override.getProviderType())) {
            log.warn("Override already exists for storage class {} and provider type {}", 
                override.getStorageClassName(), override.getProviderType());
            return ResponseEntity.badRequest().build();
        }
        
        StorageOverrideEntity created = overrideRepository.save(override);
        return ResponseEntity.ok(created);
    }

    /**
     * Update an existing storage override.
     *
     * @param id override ID
     * @param override updated override data
     * @return updated storage override
     */
    @PutMapping("/{id}")
    public ResponseEntity<StorageOverrideEntity> updateOverride(
            @PathVariable UUID id,
            @RequestBody StorageOverrideEntity override) {
        log.info("Updating storage override {}", id);
        
        Optional<StorageOverrideEntity> existingOpt = overrideRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        StorageOverrideEntity existing = existingOpt.get();
        existing.setProviderStorageNames(override.getProviderStorageNames());
        existing.setPriority(override.getPriority());
        
        StorageOverrideEntity updated = overrideRepository.save(existing);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a storage override.
     *
     * @param id override ID
     * @return no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOverride(@PathVariable UUID id) {
        log.info("Deleting storage override {}", id);
        
        if (!overrideRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        overrideRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
