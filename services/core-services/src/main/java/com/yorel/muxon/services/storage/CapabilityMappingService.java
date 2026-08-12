/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.services.storage;

import com.yorel.muxon.db.model.StorageCapabilityMappingEntity;
import com.yorel.muxon.db.repository.StorageCapabilityMappingRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Service for mapping between Muxon generic capabilities and provider-specific equivalents.
 *
 * <p>This service enables translation of storage capabilities across different provider types. For
 * example, Muxon's "performance: high" maps to:
 *
 * <ul>
 *   <li>Libvirt: pool_type in ["rbd", "nvme"]
 *   <li>Proxmox: storage_type in ["rbd", "zfspool"]
 * </ul>
 */
@Service
public class CapabilityMappingService {

  private static final Logger log = LoggerFactory.getLogger(CapabilityMappingService.class);

  private final StorageCapabilityMappingRepository mappingRepository;
  private final ObjectMapper objectMapper;

  public CapabilityMappingService(
      StorageCapabilityMappingRepository mappingRepository, ObjectMapper objectMapper) {
    this.mappingRepository = mappingRepository;
    this.objectMapper = objectMapper;
  }

  /**
   * Translate Muxon capabilities to provider-specific capabilities.
   *
   * <p>Takes generic Muxon capabilities and returns provider-specific capability values that can be
   * used to filter provider storage.
   *
   * @param muxonCapabilities Muxon generic capabilities
   * @param providerType provider type (libvirt, proxmox)
   * @return provider-specific capabilities
   */
  public Map<String, Object> translateToProviderCapabilities(
      Map<String, Object> muxonCapabilities, String providerType) {

    Map<String, Object> providerCapabilities = new HashMap<>();

    for (Map.Entry<String, Object> entry : muxonCapabilities.entrySet()) {
      String muxonCapability = entry.getKey();
      Object muxonValue = entry.getValue();

      List<StorageCapabilityMappingEntity> mappings =
          mappingRepository.findByMuxonCapabilityAndProviderType(muxonCapability, providerType);

      for (StorageCapabilityMappingEntity mapping : mappings) {
        String providerCapability = mapping.getProviderCapability();
        Map<String, Object> valueMapping = mapping.getValueMapping();

        if (valueMapping != null && muxonValue != null) {
          Object mappedValue = translateValue(muxonValue, valueMapping);
          if (mappedValue != null) {
            providerCapabilities.put(providerCapability, mappedValue);
          }
        } else {
          // Direct mapping without value translation
          providerCapabilities.put(providerCapability, muxonValue);
        }
      }
    }

    log.debug(
        "Translated Muxon capabilities {} to provider {} capabilities: {}",
        muxonCapabilities,
        providerType,
        providerCapabilities);

    return providerCapabilities;
  }

  /**
   * Translate provider-specific capabilities to Muxon generic capabilities.
   *
   * <p>Takes provider-specific capabilities and returns generic Muxon capabilities for
   * normalization.
   *
   * @param providerCapabilities provider-specific capabilities
   * @param providerType provider type (libvirt, proxmox)
   * @return Muxon generic capabilities
   */
  public Map<String, Object> translateFromProviderCapabilities(
      Map<String, Object> providerCapabilities, String providerType) {

    Map<String, Object> muxonCapabilities = new HashMap<>();

    for (Map.Entry<String, Object> entry : providerCapabilities.entrySet()) {
      String providerCapability = entry.getKey();
      Object providerValue = entry.getValue();

      List<StorageCapabilityMappingEntity> mappings =
          mappingRepository.findByProviderType(providerType);

      for (StorageCapabilityMappingEntity mapping : mappings) {
        if (mapping.getProviderCapability().equals(providerCapability)) {
          String muxonCapability = mapping.getMuxonCapability();
          Map<String, Object> valueMapping = mapping.getValueMapping();

          if (valueMapping != null && providerValue != null) {
            Object mappedValue = reverseTranslateValue(providerValue, valueMapping);
            if (mappedValue != null) {
              muxonCapabilities.put(muxonCapability, mappedValue);
            }
          } else {
            // Direct mapping without value translation
            muxonCapabilities.put(muxonCapability, providerValue);
          }
        }
      }
    }

    log.debug(
        "Translated provider {} capabilities {} to Muxon capabilities: {}",
        providerType,
        providerCapabilities,
        muxonCapabilities);

    return muxonCapabilities;
  }

  /**
   * Check if a provider storage type matches Muxon capability requirements.
   *
   * <p>Used during capability filtering to determine if provider storage satisfies storage class
   * requirements.
   *
   * @param storageType provider storage type (e.g., "rbd", "lvm-thin")
   * @param muxonCapability Muxon capability name
   * @param muxonValue Muxon capability value
   * @param providerType provider type
   * @return true if storage type matches capability
   */
  public boolean matchesCapability(
      String storageType, String muxonCapability, Object muxonValue, String providerType) {

    List<StorageCapabilityMappingEntity> mappings =
        mappingRepository.findByMuxonCapabilityAndProviderType(muxonCapability, providerType);

    for (StorageCapabilityMappingEntity mapping : mappings) {
      Map<String, Object> valueMapping = mapping.getValueMapping();
      if (valueMapping == null) {
        continue;
      }

      Object mappedValue = valueMapping.get(String.valueOf(muxonValue));
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

  /** Translate a single value using the value mapping. */
  private Object translateValue(Object muxonValue, Map<String, Object> valueMapping) {
    String key = String.valueOf(muxonValue);
    return valueMapping.get(key);
  }

  /**
   * Reverse translate a provider value to Muxon value. Searches through value mappings to find
   * which Muxon value maps to the provider value.
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
   * Normalize provider storage type to Muxon capabilities.
   *
   * <p>Determines Muxon capabilities based on provider storage type. Used during provider storage
   * discovery.
   *
   * @param storageType provider storage type
   * @param providerType provider type
   * @return normalized Muxon capabilities
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
        String muxonValue = entry.getKey();
        Object providerValues = entry.getValue();

        if (providerValues instanceof List) {
          @SuppressWarnings("unchecked")
          List<String> types = (List<String>) providerValues;
          if (types.contains(storageType) || types.contains("*")) {
            capabilities.put(mapping.getMuxonCapability(), muxonValue);
            break;
          }
        }
      }
    }

    log.debug(
        "Normalized storage type {} for provider {} to capabilities: {}",
        storageType,
        providerType,
        capabilities);

    return capabilities;
  }
}
