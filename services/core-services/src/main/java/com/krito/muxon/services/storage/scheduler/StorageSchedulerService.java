package com.krito.muxon.services.storage.scheduler;

import com.krito.muxon.db.model.ProviderStorageEntity;
import com.krito.muxon.db.model.StorageClassEntity;
import com.krito.muxon.db.repository.ProviderStorageRepository;
import com.krito.muxon.db.repository.StorageClassRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Storage scheduler service for capability-based storage selection.
 * <p>
 * This service implements the storage scheduling workflow:
 * <ol>
 *   <li>Check for manual overrides</li>
 *   <li>Filter candidates by capabilities and constraints</li>
 *   <li>Score candidates using scoring algorithm</li>
 *   <li>Select best candidate</li>
 * </ol>
 * </p>
 */
@Service
public class StorageSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(StorageSchedulerService.class);

    private final StorageClassRepository storageClassRepository;
    private final ProviderStorageRepository providerStorageRepository;
    private final StorageOverrideResolver overrideResolver;
    private final CapabilityFilter capabilityFilter;
    private final SimpleStorageScorer storageScorer;

    public StorageSchedulerService(
            StorageClassRepository storageClassRepository,
            ProviderStorageRepository providerStorageRepository,
            StorageOverrideResolver overrideResolver,
            CapabilityFilter capabilityFilter,
            SimpleStorageScorer storageScorer) {
        this.storageClassRepository = storageClassRepository;
        this.providerStorageRepository = providerStorageRepository;
        this.overrideResolver = overrideResolver;
        this.capabilityFilter = capabilityFilter;
        this.storageScorer = storageScorer;
    }

    /**
     * Schedule storage for a volume request.
     * <p>
     * Selects the best provider storage based on storage class requirements.
     * </p>
     *
     * @param storageClassName storage class name
     * @param providerId provider identifier
     * @param sizeBytes volume size in bytes
     * @return scheduling result with selected storage and metadata
     */
    public SchedulingResult scheduleStorage(
            String storageClassName, 
            UUID providerId, 
            long sizeBytes) {
        
        log.info("Scheduling storage: class={}, provider={}, size={}GB", 
            storageClassName, providerId, sizeBytes / (1024L * 1024L * 1024L));
        
        // Get storage class
        Optional<StorageClassEntity> storageClassOpt = 
            storageClassRepository.findByName(storageClassName);
        
        if (storageClassOpt.isEmpty()) {
            return SchedulingResult.failure("Storage class not found: " + storageClassName);
        }
        
        StorageClassEntity storageClass = storageClassOpt.get();
        
        // Build scheduler context
        SchedulerContext context = SchedulerContext.builder()
            .storageClassName(storageClassName)
            .providerId(providerId)
            .sizeBytes(sizeBytes)
            .capabilities(storageClass.getCapabilities())
            .constraints(storageClass.getConstraints())
            .build();
        
        // Step 1: Check for overrides
        List<ProviderStorageEntity> candidates = checkOverrides(context);
        
        // Step 2: If no overrides, get all provider storage
        if (candidates.isEmpty()) {
            candidates = providerStorageRepository.findByProviderIdAndEnabled(providerId, true);
            log.debug("Found {} enabled storage candidates for provider {}", 
                candidates.size(), providerId);
        } else {
            log.info("Using {} storage entries from override", candidates.size());
        }
        
        if (candidates.isEmpty()) {
            return SchedulingResult.failure("No storage available for provider");
        }
        
        // Step 3: Filter by capabilities and constraints
        List<ProviderStorageEntity> filtered = capabilityFilter.filter(candidates, context);
        
        if (filtered.isEmpty()) {
            return SchedulingResult.failure(
                "No storage matches capabilities and constraints for storage class " + storageClassName);
        }
        
        log.debug("Filtered to {} matching storage entries", filtered.size());
        
        // Step 4: Score and rank
        List<SimpleStorageScorer.ScoredStorage> scored = storageScorer.scoreAndRank(filtered, context);
        
        if (scored.isEmpty()) {
            return SchedulingResult.failure("No storage could be scored");
        }
        
        // Step 5: Select best
        SimpleStorageScorer.ScoredStorage best = scored.get(0);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("score", best.getScore());
        metadata.put("candidates_total", candidates.size());
        metadata.put("candidates_filtered", filtered.size());
        metadata.put("candidates_scored", scored.size());
        metadata.put("storage_type", best.getStorage().getStorageType());
        metadata.put("storage_name", best.getStorage().getName());
        
        log.info("Selected storage: name={}, type={}, score={}", 
            best.getStorage().getName(), 
            best.getStorage().getStorageType(), 
            best.getScore());
        
        return SchedulingResult.success(best.getStorage(), metadata);
    }

    /**
     * Check for storage overrides.
     */
    private List<ProviderStorageEntity> checkOverrides(SchedulerContext context) {
        // Get provider type from provider storage
        List<ProviderStorageEntity> providerStorage = 
            providerStorageRepository.findByProviderId(context.getProviderId());
        
        if (providerStorage.isEmpty()) {
            return List.of();
        }
        
        String providerType = providerStorage.get(0).getProviderType();
        
        if (overrideResolver.hasOverride(context.getStorageClassName(), providerType)) {
            return overrideResolver.resolveOverride(context.getStorageClassName(), providerType);
        }
        
        return List.of();
    }

    /**
     * All enabled provider pools for {@code providerId} that match {@code storageClassName} (capabilities,
     * constraints, and optional storage overrides). Used when replicating content into every eligible pool.
     */
    public List<ProviderStorageEntity> findMatchingStoragePoolsForProvider(
            String storageClassName, UUID providerId) {
        Optional<StorageClassEntity> storageClassOpt = storageClassRepository.findByName(storageClassName);
        if (storageClassOpt.isEmpty()) {
            return List.of();
        }
        StorageClassEntity storageClass = storageClassOpt.get();
        SchedulerContext context = SchedulerContext.builder()
                .storageClassName(storageClassName)
                .providerId(providerId)
                .sizeBytes(0L)
                .capabilities(storageClass.getCapabilities())
                .constraints(storageClass.getConstraints())
                .build();
        List<ProviderStorageEntity> candidates = checkOverrides(context);
        if (candidates.isEmpty()) {
            candidates = new ArrayList<>(providerStorageRepository.findByProviderIdAndEnabled(providerId, true));
        }
        if (candidates.isEmpty()) {
            return List.of();
        }
        return capabilityFilter.filter(candidates, context);
    }

    /**
     * Get all available storage for a storage class across all providers.
     *
     * @param storageClassName storage class name
     * @return list of matching provider storage
     */
    public List<ProviderStorageEntity> getAvailableStorage(String storageClassName) {
        Optional<StorageClassEntity> storageClassOpt = 
            storageClassRepository.findByName(storageClassName);
        
        if (storageClassOpt.isEmpty()) {
            return List.of();
        }
        
        StorageClassEntity storageClass = storageClassOpt.get();
        List<ProviderStorageEntity> allStorage = providerStorageRepository.findByEnabled(true);
        
        SchedulerContext context = SchedulerContext.builder()
            .storageClassName(storageClassName)
            .capabilities(storageClass.getCapabilities())
            .constraints(storageClass.getConstraints())
            .sizeBytes(0) // No size constraint for listing
            .build();
        
        return capabilityFilter.filter(allStorage, context);
    }

    /**
     * Scheduling result containing selected storage and metadata.
     */
    public static class SchedulingResult {
        private final boolean success;
        private final ProviderStorageEntity selectedStorage;
        private final Map<String, Object> metadata;
        private final String errorMessage;

        private SchedulingResult(boolean success, ProviderStorageEntity selectedStorage, 
                                Map<String, Object> metadata, String errorMessage) {
            this.success = success;
            this.selectedStorage = selectedStorage;
            this.metadata = metadata;
            this.errorMessage = errorMessage;
        }

        public static SchedulingResult success(ProviderStorageEntity storage, Map<String, Object> metadata) {
            return new SchedulingResult(true, storage, metadata, null);
        }

        public static SchedulingResult failure(String errorMessage) {
            return new SchedulingResult(false, null, Map.of(), errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public ProviderStorageEntity getSelectedStorage() {
            return selectedStorage;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
