package com.onetattva.infron.core.services.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onetattva.infron.db.model.StorageCapabilityMappingEntity;
import com.onetattva.infron.db.repository.StorageCapabilityMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for mapping between Infron generic capabilities and provider-specific equivalents.
 * <p>
 * This service enables translation of storage capabilities across different provider types.
 * For example, Infron's "performance: high" maps to:
 * <ul>
 *   <li>Libvirt: pool_type in ["rbd", "nvme"]</li>
 *   <li>Proxmox: storage_type in ["rbd", "zfspool"]</li>
 * </ul>
 * </p>
 */
@Service
public class CapabilityMappingService {

    private static final Logger log = LoggerFactory.getLogger(CapabilityMappingService.class);

    private final StorageCapabilityMappingRepository mappingRepository;
    private final ObjectMapper objectMapper;

    public CapabilityMappingService(
            StorageCapabilityMappingRepository mappingRepository,
            ObjectMapper objectMapper) {
        this.mappingRepository = mappingRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Translate Infron capabilities to provider-specific capabilities.
     * <p>
     * Takes generic Infron capabilities and returns provider-specific
     * capability values that can be used to filter provider storage.
     * </p>
     *
     * @param infronCapabilities Infron generic capabilities
     * @param providerType provider type (libvirt, proxmox)
     * @return provider-specific capabilities
     */
    public Map<String, Object> translateToProviderCapabilities(
            Map<String, Object> infronCapabilities, String providerType) {
        
        Map<String, Object> providerCapabilities = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : infronCapabilities.entrySet()) {
            String infronCapability = entry.getKey();
            Object infronValue = entry.getValue();
            
            List<StorageCapabilityMappingEntity> mappings = 
                mappingRepository.findByInfronCapabilityAndProviderType(infronCapability, providerType);
            
            for (StorageCapabilityMappingEntity mapping : mappings) {
                String providerCapability = mapping.getProviderCapability();
                Map<String, Object> valueMapping = mapping.getValueMapping();
                
                if (valueMapping != null && infronValue != null) {
                    Object mappedValue = translateValue(infronValue, valueMapping);
                    if (mappedValue != null) {
                        providerCapabilities.put(providerCapability, mappedValue);
                    }
                } else {
                    // Direct mapping without value translation
                    providerCapabilities.put(providerCapability, infronValue);
                }
            }
        }
        
        log.debug("Translated Infron capabilities {} to provider {} capabilities: {}", 
            infronCapabilities, providerType, providerCapabilities);
        
        return providerCapabilities;
    }

    /**
     * Translate provider-specific capabilities to Infron generic capabilities.
     * <p>
     * Takes provider-specific capabilities and returns generic Infron
     * capabilities for normalization.
     * </p>
     *
     * @param providerCapabilities provider-specific capabilities
     * @param providerType provider type (libvirt, proxmox)
     * @return Infron generic capabilities
     */
    public Map<String, Object> translateFromProviderCapabilities(
            Map<String, Object> providerCapabilities, String providerType) {
        
        Map<String, Object> infronCapabilities = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : providerCapabilities.entrySet()) {
            String providerCapability = entry.getKey();
            Object providerValue = entry.getValue();
            
            List<StorageCapabilityMappingEntity> mappings = 
                mappingRepository.findByProviderType(providerType);
            
            for (StorageCapabilityMappingEntity mapping : mappings) {
                if (mapping.getProviderCapability().equals(providerCapability)) {
                    String infronCapability = mapping.getInfronCapability();
                    Map<String, Object> valueMapping = mapping.getValueMapping();
                    
                    if (valueMapping != null && providerValue != null) {
                        Object mappedValue = reverseTranslateValue(providerValue, valueMapping);
                        if (mappedValue != null) {
                            infronCapabilities.put(infronCapability, mappedValue);
                        }
                    } else {
                        // Direct mapping without value translation
                        infronCapabilities.put(infronCapability, providerValue);
                    }
                }
            }
        }
        
        log.debug("Translated provider {} capabilities {} to Infron capabilities: {}", 
            providerType, providerCapabilities, infronCapabilities);
        
        return infronCapabilities;
    }

    /**
     * Check if a provider storage type matches Infron capability requirements.
     * <p>
     * Used during capability filtering to determine if provider storage
     * satisfies storage class requirements.
     * </p>
     *
     * @param storageType provider storage type (e.g., "rbd", "lvm-thin")
     * @param infronCapability Infron capability name
     * @param infronValue Infron capability value
     * @param providerType provider type
     * @return true if storage type matches capability
     */
    public boolean matchesCapability(
            String storageType, 
            String infronCapability, 
            Object infronValue, 
            String providerType) {
        
        List<StorageCapabilityMappingEntity> mappings = 
            mappingRepository.findByInfronCapabilityAndProviderType(infronCapability, providerType);
        
        for (StorageCapabilityMappingEntity mapping : mappings) {
            Map<String, Object> valueMapping = mapping.getValueMapping();
            if (valueMapping == null) {
                continue;
            }
            
            Object mappedValue = valueMapping.get(String.valueOf(infronValue));
            if (mappedValue instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> allowedTypes = (List<String>) mappedValue;
                if (allowedTypes.contains("*") || allowedTypes.contains(storageType)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Get all capability mappings for a provider type.
     *
     * @param providerType provider type
     * @return list of capability mappings
     */
    public List<StorageCapabilityMappingEntity> getMappingsForProvider(String providerType) {
        return mappingRepository.findByProviderType(providerType);
    }

    /**
     * Translate a single value using the value mapping.
     */
    private Object translateValue(Object infronValue, Map<String, Object> valueMapping) {
        String key = String.valueOf(infronValue);
        return valueMapping.get(key);
    }

    /**
     * Reverse translate a provider value to Infron value.
     * Searches through value mappings to find which Infron value maps to the provider value.
     */
    private Object reverseTranslateValue(Object providerValue, Map<String, Object> valueMapping) {
        String providerValueStr = String.valueOf(providerValue);
        
        for (Map.Entry<String, Object> entry : valueMapping.entrySet()) {
            Object mappedValue = entry.getValue();
            
            if (mappedValue instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> values = (List<String>) mappedValue;
                if (values.contains(providerValueStr) || values.contains("*")) {
                    return entry.getKey();
                }
            } else if (String.valueOf(mappedValue).equals(providerValueStr)) {
                return entry.getKey();
            }
        }
        
        return null;
    }

    /**
     * Normalize provider storage type to Infron capabilities.
     * <p>
     * Determines Infron capabilities based on provider storage type.
     * Used during provider storage discovery.
     * </p>
     *
     * @param storageType provider storage type
     * @param providerType provider type
     * @return normalized Infron capabilities
     */
    public Map<String, Object> normalizeStorageType(String storageType, String providerType) {
        Map<String, Object> capabilities = new HashMap<>();
        
        List<StorageCapabilityMappingEntity> mappings = 
            mappingRepository.findByProviderType(providerType);
        
        for (StorageCapabilityMappingEntity mapping : mappings) {
            Map<String, Object> valueMapping = mapping.getValueMapping();
            if (valueMapping == null) {
                continue;
            }
            
            for (Map.Entry<String, Object> entry : valueMapping.entrySet()) {
                String infronValue = entry.getKey();
                Object providerValues = entry.getValue();
                
                if (providerValues instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> types = (List<String>) providerValues;
                    if (types.contains(storageType) || types.contains("*")) {
                        capabilities.put(mapping.getInfronCapability(), infronValue);
                        break;
                    }
                }
            }
        }
        
        log.debug("Normalized storage type {} for provider {} to capabilities: {}", 
            storageType, providerType, capabilities);
        
        return capabilities;
    }
}
