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
package com.yorel.muxon.services.content;

import com.yorel.muxon.api.model.ContentLibraryDistribution;
import com.yorel.muxon.api.model.ContentLibraryDistributionList;
import com.yorel.muxon.api.model.EntityReference;
import com.yorel.muxon.api.model.PublishRequest;
import com.yorel.muxon.common.EntityNotFoundException;
import com.yorel.muxon.db.model.ContentLibraryDistributionEntity;
import com.yorel.muxon.db.model.ContentLibraryEntity;
import com.yorel.muxon.db.model.DatacenterEntity;
import com.yorel.muxon.db.repository.ContentItemDistributionRepository;
import com.yorel.muxon.db.repository.ContentLibraryDistributionRepository;
import com.yorel.muxon.db.repository.ContentLibraryRepository;
import com.yorel.muxon.db.repository.DatacenterRepository;
import com.yorel.muxon.db.repository.TenantDatacenterGrantRepository;
import com.yorel.muxon.services.storage.StorageClassValidationService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ContentLibraryPublishService {

  private final ContentLibraryRepository contentLibraryRepository;
  private final ContentLibraryDistributionRepository contentLibraryDistributionRepository;
  private final ContentItemDistributionRepository contentItemDistributionRepository;
  private final DatacenterRepository datacenterRepository;
  private final TenantDatacenterGrantRepository tenantDatacenterGrantRepository;
  private final StorageClassValidationService storageClassValidationService;
  private final ContentLibraryDistributionApiConverter converter;

  public ContentLibraryPublishService(
      ContentLibraryRepository contentLibraryRepository,
      ContentLibraryDistributionRepository contentLibraryDistributionRepository,
      ContentItemDistributionRepository contentItemDistributionRepository,
      DatacenterRepository datacenterRepository,
      TenantDatacenterGrantRepository tenantDatacenterGrantRepository,
      StorageClassValidationService storageClassValidationService,
      ContentLibraryDistributionApiConverter converter) {
    this.contentLibraryRepository = contentLibraryRepository;
    this.contentLibraryDistributionRepository = contentLibraryDistributionRepository;
    this.contentItemDistributionRepository = contentItemDistributionRepository;
    this.datacenterRepository = datacenterRepository;
    this.tenantDatacenterGrantRepository = tenantDatacenterGrantRepository;
    this.storageClassValidationService = storageClassValidationService;
    this.converter = converter;
  }

  @Transactional
  public ContentLibraryDistribution publishPlatform(UUID libraryId, PublishRequest publishRequest) {
    ContentLibraryEntity library =
        contentLibraryRepository
            .findByIdAndTenantId(libraryId, ContentLibraryService.SYSTEM_TENANT_ID)
            .orElseThrow(
                () -> new EntityNotFoundException("Content library not found: " + libraryId));
    UUID datacenterId = publishRequest.getDatacenterId();
    validatePublishStorageClass(null, datacenterId, publishRequest.getStorageClassName());
    return publishInternal(library, datacenterId, publishRequest.getStorageClassName(), null);
  }

  @Transactional
  public ContentLibraryDistribution publishTenant(
      UUID tenantId, UUID libraryId, PublishRequest publishRequest) {
    ContentLibraryEntity library =
        contentLibraryRepository
            .findByIdAndTenantId(libraryId, tenantId)
            .orElseThrow(
                () -> new EntityNotFoundException("Content library not found: " + libraryId));
    UUID datacenterId = publishRequest.getDatacenterId();
    validatePublishStorageClass(tenantId, datacenterId, publishRequest.getStorageClassName());
    return publishInternal(library, datacenterId, publishRequest.getStorageClassName(), tenantId);
  }

  private void validatePublishStorageClass(
      UUID tenantId, UUID datacenterId, String storageClassName) {
    if (storageClassName == null || storageClassName.isBlank()) {
      throw new IllegalArgumentException("storageClassName is required");
    }
    String name = storageClassName.trim();
    if (!storageClassValidationService.storageClassExists(name)) {
      throw new IllegalArgumentException("Unknown storage class: " + name);
    }
    if (tenantId == null) {
      if (!storageClassValidationService.isStorageClassAvailableInDatacenter(datacenterId, name)) {
        throw new IllegalArgumentException(
            "Storage class is not available for this datacenter: " + name);
      }
    } else {
      if (!storageClassValidationService.isStorageClassAllowedForTenant(
          tenantId, datacenterId, name)) {
        throw new IllegalArgumentException(
            "Storage class is not allowed for this tenant at the selected datacenter: " + name);
      }
    }
  }

  private ContentLibraryDistribution publishInternal(
      ContentLibraryEntity library,
      UUID datacenterId,
      String storageClassName,
      UUID tenantIdForGrantCheck) {
    DatacenterEntity datacenter =
        datacenterRepository
            .findById(datacenterId)
            .orElseThrow(
                () -> new IllegalArgumentException("Datacenter not found: " + datacenterId));
    if (tenantIdForGrantCheck != null) {
      var grant =
          tenantDatacenterGrantRepository
              .findByTenant_IdAndDatacenter_Id(tenantIdForGrantCheck, datacenterId)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Tenant is not granted access to datacenter " + datacenterId));
      if (Boolean.FALSE.equals(grant.getAccess())) {
        throw new IllegalArgumentException(
            "Tenant access to datacenter " + datacenterId + " is disabled");
      }
    }
    if (contentLibraryDistributionRepository
        .findByLibraryIdAndDatacenterId(library.getId(), datacenterId)
        .isPresent()) {
      throw new IllegalArgumentException("Library already published to this datacenter");
    }
    ContentLibraryDistributionEntity entity = new ContentLibraryDistributionEntity();
    entity.setLibraryId(library.getId());
    entity.setDatacenterId(datacenterId);
    entity.setStorageClassName(storageClassName.trim());
    entity.setReplicateStatus("pending");
    entity.setProgressPercent(0);
    entity.setErrorMessage(null);
    EntityReference ref = new EntityReference();
    ref.setId(datacenter.getId());
    ref.setName(datacenter.getName());
    return converter.toApi(contentLibraryDistributionRepository.save(entity), ref);
  }

  public ContentLibraryDistributionList listForPlatformLibrary(UUID libraryId) {
    contentLibraryRepository
        .findByIdAndTenantId(libraryId, ContentLibraryService.SYSTEM_TENANT_ID)
        .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
    List<ContentLibraryDistributionEntity> rows =
        contentLibraryDistributionRepository.findByLibraryIdOrderByDatacenterId(libraryId);
    return converter.toList(rows, datacenterNames(rows));
  }

  public ContentLibraryDistributionList listForTenantLibrary(UUID tenantId, UUID libraryId) {
    contentLibraryRepository
        .findByIdAndTenantIdIn(
            libraryId, java.util.List.of(tenantId, ContentLibraryService.SYSTEM_TENANT_ID))
        .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
    List<ContentLibraryDistributionEntity> rows =
        contentLibraryDistributionRepository.findByLibraryIdOrderByDatacenterId(libraryId);
    return converter.toList(rows, datacenterNames(rows));
  }

  private Map<UUID, String> datacenterNames(List<ContentLibraryDistributionEntity> rows) {
    if (rows.isEmpty()) {
      return Map.of();
    }
    Set<UUID> ids =
        rows.stream()
            .map(ContentLibraryDistributionEntity::getDatacenterId)
            .collect(Collectors.toSet());
    return datacenterRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(DatacenterEntity::getId, DatacenterEntity::getName));
  }

  @Transactional
  public void unpublishPlatform(UUID libraryId, UUID distributionId) {
    contentLibraryRepository
        .findByIdAndTenantId(libraryId, ContentLibraryService.SYSTEM_TENANT_ID)
        .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
    ContentLibraryDistributionEntity row =
        contentLibraryDistributionRepository
            .findById(distributionId)
            .orElseThrow(() -> new EntityNotFoundException("Publish mapping not found"));
    if (!row.getLibraryId().equals(libraryId)) {
      throw new EntityNotFoundException("Publish mapping not found");
    }
    contentItemDistributionRepository.deleteByDistributionId(distributionId);
    contentLibraryDistributionRepository.deleteById(distributionId);
  }

  @Transactional
  public void unpublishTenant(UUID tenantId, UUID libraryId, UUID distributionId) {
    contentLibraryRepository
        .findByIdAndTenantId(libraryId, tenantId)
        .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
    ContentLibraryDistributionEntity row =
        contentLibraryDistributionRepository
            .findById(distributionId)
            .orElseThrow(() -> new EntityNotFoundException("Publish mapping not found"));
    if (!row.getLibraryId().equals(libraryId)) {
      throw new EntityNotFoundException("Publish mapping not found");
    }
    contentItemDistributionRepository.deleteByDistributionId(distributionId);
    contentLibraryDistributionRepository.deleteById(distributionId);
  }
}
