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
package com.yorel.muxon.services;

import com.yorel.muxon.api.model.Datacenter;
import com.yorel.muxon.api.model.DatacenterCapacity;
import com.yorel.muxon.api.model.DatacenterCreate;
import com.yorel.muxon.api.model.DatacenterList;
import com.yorel.muxon.api.model.DatacenterSettings;
import com.yorel.muxon.api.model.DatacenterUpdate;
import com.yorel.muxon.api.model.EntityReference;
import com.yorel.muxon.api.model.ProviderType;
import com.yorel.muxon.api.model.StorageClassValidationResult;
import com.yorel.muxon.db.model.DatacenterEntity;
import com.yorel.muxon.db.model.NodeClusterEntity;
import com.yorel.muxon.db.model.ProviderEntity;
import com.yorel.muxon.db.repository.DatacenterRepository;
import com.yorel.muxon.db.repository.NodeClusterRepository;
import com.yorel.muxon.db.repository.ProviderRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DatacentersService {

  @Autowired private DatacenterRepository datacenterRepository;

  @Autowired private NodeClusterRepository nodeClusterRepository;

  @Autowired private ProviderRepository providerRepository;

  @Autowired
  private com.yorel.muxon.services.storage.StorageClassValidationService
      storageClassValidationService;

  @Autowired(required = false)
  private DatacenterNetworkCapabilitiesService datacenterNetworkCapabilitiesService;

  @Autowired(required = false)
  private com.yorel.muxon.db.repository.FabricNetworkRepository fabricNetworkRepository;

  @Autowired(required = false)
  private com.yorel.muxon.db.repository.PublicIpPoolRepository publicIpPoolRepository;

  @Autowired(required = false)
  private com.yorel.muxon.db.repository.DatacenterNetworkCapabilitiesRepository
      dcNetworkCapabilitiesRepository;

  /** Create a new datacenter. Provider type is derived from the node cluster's provider. */
  public Datacenter createDatacenter(DatacenterCreate datacenterCreate) {
    // Validate datacenter name is unique
    if (datacenterRepository.existsByName(datacenterCreate.getName())) {
      throw new RuntimeException(
          "DATACENTER_ALREADY_EXISTS: Datacenter with name '"
              + datacenterCreate.getName()
              + "' already exists");
    }

    // Validate node cluster exists
    NodeClusterEntity nodeCluster =
        nodeClusterRepository
            .findById(datacenterCreate.getNodeClusterId())
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "NODE_CLUSTER_NOT_FOUND: Node cluster with ID "
                            + datacenterCreate.getNodeClusterId()
                            + " not found"));

    // Check node cluster is not already linked to another datacenter
    datacenterRepository
        .findByNodeClusterId(datacenterCreate.getNodeClusterId())
        .ifPresent(
            dc -> {
              throw new RuntimeException(
                  "NODE_CLUSTER_ALREADY_LINKED: Node cluster is already linked to datacenter "
                      + dc.getId());
            });

    // Get provider to derive provider type
    ProviderEntity provider =
        providerRepository
            .findById(nodeCluster.getProvider().getId())
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "PROVIDER_NOT_FOUND: Provider with ID "
                            + nodeCluster.getProvider().getId()
                            + " not found"));

    // Create datacenter entity
    DatacenterEntity entity = new DatacenterEntity();
    entity.setId(UUID.randomUUID());
    entity.setName(datacenterCreate.getName());
    entity.setDescription(datacenterCreate.getDescription());
    entity.setNodeCluster(nodeCluster);

    // Set capacity
    DatacenterCapacity capacity = datacenterCreate.getCapacity();
    if (capacity != null) {
      // Initialize calculated fields
      if (capacity.getAvailableCpus() == null) {
        capacity.setAvailableCpus(capacity.getTotalCpus());
      }
      if (capacity.getAvailableMemoryGb() == null) {
        capacity.setAvailableMemoryGb(capacity.getTotalMemoryGb());
      }
      if (capacity.getAvailableStorageGb() == null) {
        capacity.setAvailableStorageGb(capacity.getTotalStorageGb());
      }
      if (capacity.getUsedCpus() == null) {
        capacity.setUsedCpus(0);
      }
      if (capacity.getUsedMemoryGb() == null) {
        capacity.setUsedMemoryGb(0);
      }
      if (capacity.getUsedStorageGb() == null) {
        capacity.setUsedStorageGb(0);
      }
    }
    entity.setCapacity(capacity);

    // Set settings with derived provider type
    DatacenterSettings settings = datacenterCreate.getSettings();
    if (settings == null) {
      settings = new DatacenterSettings();
    }
    // Derive provider type from provider
    settings.setProviderType(provider.getType());
    entity.setSettings(settings);

    Instant now = Instant.now();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    DatacenterEntity saved = datacenterRepository.save(entity);

    // Auto-create default network capabilities record
    if (datacenterNetworkCapabilitiesService != null) {
      datacenterNetworkCapabilitiesService.createDefaults(saved);
    }

    return mapEntityToApi(saved);
  }

  public void deleteDatacenter(UUID datacenterId) {
    datacenterRepository.deleteById(datacenterId);
  }

  public Datacenter getDatacenter(UUID datacenterId) {
    DatacenterEntity entity =
        datacenterRepository
            .findById(datacenterId)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "DATACENTER_NOT_FOUND: Datacenter with ID " + datacenterId + " not found"));
    return mapEntityToApi(entity);
  }

  public DatacenterSettings getDatacenterSettings(UUID datacenterId) {
    DatacenterEntity entity =
        datacenterRepository
            .findById(datacenterId)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "DATACENTER_NOT_FOUND: Datacenter with ID " + datacenterId + " not found"));
    return entity.getSettings();
  }

  /** List datacenters with optional filtering by provider type. */
  public DatacenterList listDatacenters(Integer page, Integer perPage, ProviderType providerType) {
    Pageable pageable = PageRequest.of(page - 1, perPage);
    Page<DatacenterEntity> entityPage;

    if (providerType != null) {
      // Filter by provider type
      List<DatacenterEntity> filtered = datacenterRepository.findByProviderType(providerType);
      // Apply pagination manually for filtered results
      int start = (page - 1) * perPage;
      int end = Math.min(start + perPage, filtered.size());
      List<DatacenterEntity> pageContent =
          start < filtered.size() ? filtered.subList(start, end) : new ArrayList<>();
      entityPage =
          new org.springframework.data.domain.PageImpl<>(pageContent, pageable, filtered.size());
    } else {
      entityPage = datacenterRepository.findAll(pageable);
    }

    List<Datacenter> apiDatacenters =
        entityPage.getContent().stream().map(this::mapEntityToApi).toList();

    DatacenterList datacenterList = new DatacenterList();
    datacenterList.setTotal((int) entityPage.getTotalElements());
    datacenterList.setPage(page);
    datacenterList.setPerPage(perPage);
    datacenterList.setItems(apiDatacenters);
    return datacenterList;
  }

  /** Replace datacenter (full update). Note: nodeClusterId cannot be modified after creation. */
  public Datacenter replaceDatacenter(UUID datacenterId, DatacenterUpdate datacenterUpdate) {
    DatacenterEntity entity =
        datacenterRepository
            .findById(datacenterId)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "DATACENTER_NOT_FOUND: Datacenter with ID " + datacenterId + " not found"));

    entity.setName(datacenterUpdate.getName());
    entity.setDescription(datacenterUpdate.getDescription());

    // Update capacity if provided
    if (datacenterUpdate.getCapacity() != null) {
      DatacenterCapacity capacity = datacenterUpdate.getCapacity();
      entity.setCapacity(capacity);
    }

    // Update settings if provided (but provider type is read-only)
    if (datacenterUpdate.getSettings() != null) {
      DatacenterSettings currentSettings = entity.getSettings();
      DatacenterSettings newSettings = datacenterUpdate.getSettings();

      // Preserve provider type (cannot be modified)
      newSettings.setProviderType(currentSettings.getProviderType());
      entity.setSettings(newSettings);
    }

    entity.setUpdatedAt(Instant.now());
    DatacenterEntity saved = datacenterRepository.save(entity);
    return mapEntityToApi(saved);
  }

  /** Update datacenter (partial update). */
  public Datacenter updateDatacenter(UUID datacenterId, DatacenterUpdate datacenterUpdate) {
    DatacenterEntity entity =
        datacenterRepository
            .findById(datacenterId)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "DATACENTER_NOT_FOUND: Datacenter with ID " + datacenterId + " not found"));

    if (datacenterUpdate.getName() != null) {
      entity.setName(datacenterUpdate.getName());
    }
    if (datacenterUpdate.getDescription() != null) {
      entity.setDescription(datacenterUpdate.getDescription());
    }

    // Update capacity if provided
    if (datacenterUpdate.getCapacity() != null) {
      entity.setCapacity(datacenterUpdate.getCapacity());
    }

    // Update settings if provided (but provider type is read-only)
    if (datacenterUpdate.getSettings() != null) {
      DatacenterSettings currentSettings = entity.getSettings();
      DatacenterSettings newSettings = datacenterUpdate.getSettings();

      // Preserve provider type (cannot be modified)
      if (currentSettings != null) {
        newSettings.setProviderType(currentSettings.getProviderType());
      }
      entity.setSettings(newSettings);
    }

    entity.setUpdatedAt(Instant.now());
    DatacenterEntity saved = datacenterRepository.save(entity);
    return mapEntityToApi(saved);
  }

  /** Replace datacenter settings. Note: providerType is read-only and cannot be modified. */
  public DatacenterSettings replaceDatacenterSettings(
      UUID datacenterId, DatacenterSettings datacenterSettings) {
    DatacenterEntity entity =
        datacenterRepository
            .findById(datacenterId)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "DATACENTER_NOT_FOUND: Datacenter with ID " + datacenterId + " not found"));

    DatacenterSettings currentSettings = entity.getSettings();
    // Preserve provider type (cannot be modified)
    datacenterSettings.setProviderType(
        currentSettings != null ? currentSettings.getProviderType() : null);

    entity.setSettings(datacenterSettings);
    entity.setUpdatedAt(Instant.now());
    datacenterRepository.save(entity);
    return datacenterSettings;
  }

  /**
   * Update datacenter settings (partial update). Note: providerType is read-only and cannot be
   * modified.
   */
  public DatacenterSettings updateDatacenterSettings(
      UUID datacenterId, DatacenterSettings datacenterSettings) {
    DatacenterEntity entity =
        datacenterRepository
            .findById(datacenterId)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "DATACENTER_NOT_FOUND: Datacenter with ID " + datacenterId + " not found"));

    DatacenterSettings currentSettings = entity.getSettings();
    DatacenterSettings mergedSettings =
        currentSettings != null ? currentSettings : new DatacenterSettings();

    // Merge settings (but preserve provider type). Overcommit ratios are enterprise-only, not in
    // core.
    if (datacenterSettings.getVmClasses() != null) {
      mergedSettings.setVmClasses(datacenterSettings.getVmClasses());
    }
    if (datacenterSettings.getStorageClasses() != null) {
      mergedSettings.setStorageClasses(datacenterSettings.getStorageClasses());
    }
    if (datacenterSettings.getNetworkDomains() != null) {
      mergedSettings.setNetworkDomains(datacenterSettings.getNetworkDomains());
    }
    if (datacenterSettings.getProviderSpecificSettings() != null) {
      mergedSettings.setProviderSpecificSettings(datacenterSettings.getProviderSpecificSettings());
    }

    entity.setSettings(mergedSettings);
    entity.setUpdatedAt(Instant.now());
    datacenterRepository.save(entity);
    return mergedSettings;
  }

  /** Update datacenter capacity. */
  public DatacenterCapacity updateDatacenterCapacity(
      UUID datacenterId, DatacenterCapacity capacity) {
    DatacenterEntity entity =
        datacenterRepository
            .findById(datacenterId)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "DATACENTER_NOT_FOUND: Datacenter with ID " + datacenterId + " not found"));

    entity.setCapacity(capacity);
    entity.setUpdatedAt(Instant.now());
    DatacenterEntity saved = datacenterRepository.save(entity);
    return saved.getCapacity();
  }

  private Datacenter mapEntityToApi(DatacenterEntity entity) {
    Datacenter api = new Datacenter();
    api.setId(entity.getId());
    api.setName(entity.getName());
    api.setDescription(entity.getDescription());

    // Map node cluster
    if (entity.getNodeCluster() != null) {
      EntityReference apiNodeCluster = new EntityReference();
      apiNodeCluster.setId(entity.getNodeCluster().getId());
      apiNodeCluster.setName(entity.getNodeCluster().getName());
      api.setNodeCluster(apiNodeCluster);
    }

    // Map capacity
    api.setCapacity(entity.getCapacity());

    // Map settings
    api.setSettings(entity.getSettings());

    if (entity.getCreatedAt() != null) {
      api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
    if (entity.getUpdatedAt() != null) {
      api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }
    return api;
  }

  public List<String> getAvailableStorageClasses(UUID datacenterId) {
    return storageClassValidationService.getDatacenterStorageClasses(datacenterId);
  }

  public StorageClassValidationResult validateDatacenterStorageClasses(
      UUID datacenterId, List<String> storageClasses) {
    return storageClassValidationService.validateDatacenterStorageClasses(
        datacenterId, storageClasses);
  }
}
