package com.onetattva.infron.core.controllers;

import com.onetattva.infron.core.services.storage.ProviderStorageDiscoveryService;
import com.onetattva.infron.db.model.ProviderStorageEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for provider storage operations.
 * <p>
 * Provides endpoints to view discovered provider storage and trigger storage sync.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/provider-storage")
public class ProviderStorageController {

    private static final Logger log = LoggerFactory.getLogger(ProviderStorageController.class);

    private final ProviderStorageDiscoveryService discoveryService;

    public ProviderStorageController(ProviderStorageDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    /**
     * Get all storage for a specific provider.
     *
     * @param providerId provider ID
     * @return list of provider storage
     */
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<ProviderStorageEntity>> getStorageByProvider(
            @PathVariable UUID providerId) {
        log.debug("Getting storage for provider {}", providerId);
        List<ProviderStorageEntity> storage = discoveryService.getProviderStorage(providerId);
        return ResponseEntity.ok(storage);
    }

    /**
     * Get enabled storage for a specific provider.
     *
     * @param providerId provider ID
     * @return list of enabled provider storage
     */
    @GetMapping("/provider/{providerId}/enabled")
    public ResponseEntity<List<ProviderStorageEntity>> getEnabledStorageByProvider(
            @PathVariable UUID providerId) {
        log.debug("Getting enabled storage for provider {}", providerId);
        List<ProviderStorageEntity> storage = discoveryService.getEnabledProviderStorage(providerId);
        return ResponseEntity.ok(storage);
    }

    /**
     * Trigger storage discovery and sync for a provider.
     *
     * @param providerId provider ID
     * @return sync result with count of discovered storage
     */
    @PostMapping("/provider/{providerId}/sync")
    public ResponseEntity<Map<String, Object>> syncProviderStorage(@PathVariable UUID providerId) {
        log.info("Triggering storage sync for provider {}", providerId);
        
        try {
            int count = discoveryService.discoverAndSyncStorage(providerId);
            
            Map<String, Object> result = Map.of(
                "success", true,
                "providerId", providerId,
                "storageCount", count,
                "message", "Successfully synced " + count + " storage entries"
            );
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to sync storage for provider {}: {}", providerId, e.getMessage(), e);
            
            Map<String, Object> result = Map.of(
                "success", false,
                "providerId", providerId,
                "error", e.getMessage()
            );
            
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Trigger storage discovery for all providers.
     *
     * @return sync results for all providers
     */
    @PostMapping("/sync-all")
    public ResponseEntity<Map<String, Object>> syncAllProviders() {
        log.info("Triggering storage sync for all providers");
        
        try {
            Map<UUID, Integer> results = discoveryService.discoverAllProviders();
            
            int totalCount = results.values().stream().mapToInt(Integer::intValue).sum();
            
            Map<String, Object> result = Map.of(
                "success", true,
                "providerCount", results.size(),
                "totalStorageCount", totalCount,
                "results", results
            );
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to sync all providers: {}", e.getMessage(), e);
            
            Map<String, Object> result = Map.of(
                "success", false,
                "error", e.getMessage()
            );
            
            return ResponseEntity.status(500).body(result);
        }
    }
}
