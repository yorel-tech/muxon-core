package com.onetattva.infron.core.services.content;

import com.onetattva.infron.api.model.ContentLibraryDatacenter;
import com.onetattva.infron.api.model.ContentLibraryDatacenterList;
import com.onetattva.infron.api.model.EntityReference;
import com.onetattva.infron.api.model.PublishRequest;
import com.onetattva.infron.core.common.EntityNotFoundException;
import com.onetattva.infron.core.services.storage.StorageClassValidationService;
import com.onetattva.infron.db.model.ContentLibraryDatacenterEntity;
import com.onetattva.infron.db.model.ContentLibraryEntity;
import com.onetattva.infron.db.model.DatacenterEntity;
import com.onetattva.infron.db.repository.ContentLibraryDatacenterRepository;
import com.onetattva.infron.db.repository.ContentLibraryRepository;
import com.onetattva.infron.db.repository.DatacenterRepository;
import com.onetattva.infron.db.repository.TenantDatacenterGrantRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ContentLibraryPublishService {

    private final ContentLibraryRepository contentLibraryRepository;
    private final ContentLibraryDatacenterRepository contentLibraryDatacenterRepository;
    private final DatacenterRepository datacenterRepository;
    private final TenantDatacenterGrantRepository tenantDatacenterGrantRepository;
    private final StorageClassValidationService storageClassValidationService;
    private final ContentLibraryDatacenterApiConverter converter;

    public ContentLibraryPublishService(
            ContentLibraryRepository contentLibraryRepository,
            ContentLibraryDatacenterRepository contentLibraryDatacenterRepository,
            DatacenterRepository datacenterRepository,
            TenantDatacenterGrantRepository tenantDatacenterGrantRepository,
            StorageClassValidationService storageClassValidationService,
            ContentLibraryDatacenterApiConverter converter) {
        this.contentLibraryRepository = contentLibraryRepository;
        this.contentLibraryDatacenterRepository = contentLibraryDatacenterRepository;
        this.datacenterRepository = datacenterRepository;
        this.tenantDatacenterGrantRepository = tenantDatacenterGrantRepository;
        this.storageClassValidationService = storageClassValidationService;
        this.converter = converter;
    }

    @Transactional
    public ContentLibraryDatacenter publishPlatform(UUID libraryId, PublishRequest publishRequest) {
        ContentLibraryEntity library = contentLibraryRepository
                .findByIdAndTenantId(libraryId, ContentLibraryService.SYSTEM_TENANT_ID)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
        UUID datacenterId = publishRequest.getDatacenterId();
        validatePublishStorageClass(null, datacenterId, publishRequest.getStorageClassName());
        return publishInternal(library, datacenterId, publishRequest.getStorageClassName(), null);
    }

    @Transactional
    public ContentLibraryDatacenter publishTenant(UUID tenantId, UUID libraryId, PublishRequest publishRequest) {
        ContentLibraryEntity library = contentLibraryRepository
                .findByIdAndTenantId(libraryId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
        UUID datacenterId = publishRequest.getDatacenterId();
        validatePublishStorageClass(tenantId, datacenterId, publishRequest.getStorageClassName());
        return publishInternal(library, datacenterId, publishRequest.getStorageClassName(), tenantId);
    }

    private void validatePublishStorageClass(UUID tenantId, UUID datacenterId, String storageClassName) {
        if (storageClassName == null || storageClassName.isBlank()) {
            throw new IllegalArgumentException("storageClassName is required");
        }
        String name = storageClassName.trim();
        if (!storageClassValidationService.storageClassExists(name)) {
            throw new IllegalArgumentException("Unknown storage class: " + name);
        }
        if (tenantId == null) {
            if (!storageClassValidationService.isStorageClassAvailableInDatacenter(datacenterId, name)) {
                throw new IllegalArgumentException("Storage class is not available for this datacenter: " + name);
            }
        } else {
            if (!storageClassValidationService.isStorageClassAllowedForTenant(tenantId, datacenterId, name)) {
                throw new IllegalArgumentException(
                        "Storage class is not allowed for this tenant at the selected datacenter: " + name);
            }
        }
    }

    private ContentLibraryDatacenter publishInternal(
            ContentLibraryEntity library,
            UUID datacenterId,
            String storageClassName,
            UUID tenantIdForGrantCheck) {
        DatacenterEntity datacenter = datacenterRepository
                .findById(datacenterId)
                .orElseThrow(() -> new IllegalArgumentException("Datacenter not found: " + datacenterId));
        if (tenantIdForGrantCheck != null) {
            var grant = tenantDatacenterGrantRepository
                    .findByTenant_IdAndDatacenter_Id(tenantIdForGrantCheck, datacenterId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Tenant is not granted access to datacenter " + datacenterId));
            if (Boolean.FALSE.equals(grant.getAccess())) {
                throw new IllegalArgumentException("Tenant access to datacenter " + datacenterId + " is disabled");
            }
        }
        if (contentLibraryDatacenterRepository
                .findByLibraryIdAndDatacenterId(library.getId(), datacenterId)
                .isPresent()) {
            throw new IllegalArgumentException("Library already published to this datacenter");
        }
        ContentLibraryDatacenterEntity entity = new ContentLibraryDatacenterEntity();
        entity.setLibraryId(library.getId());
        entity.setDatacenterId(datacenterId);
        entity.setStorageClassName(storageClassName.trim());
        entity.setReplicateStatus("pending");
        EntityReference ref = new EntityReference();
        ref.setId(datacenter.getId());
        ref.setName(datacenter.getName());
        return converter.toApi(contentLibraryDatacenterRepository.save(entity), ref);
    }

    public ContentLibraryDatacenterList listForPlatformLibrary(UUID libraryId) {
        contentLibraryRepository
                .findByIdAndTenantId(libraryId, ContentLibraryService.SYSTEM_TENANT_ID)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
        List<ContentLibraryDatacenterEntity> rows =
                contentLibraryDatacenterRepository.findByLibraryIdOrderByDatacenterId(libraryId);
        return converter.toList(rows, datacenterNames(rows));
    }

    public ContentLibraryDatacenterList listForTenantLibrary(UUID tenantId, UUID libraryId) {
        contentLibraryRepository
                .findByIdAndTenantIdIn(libraryId, java.util.List.of(tenantId, ContentLibraryService.SYSTEM_TENANT_ID))
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
        List<ContentLibraryDatacenterEntity> rows =
                contentLibraryDatacenterRepository.findByLibraryIdOrderByDatacenterId(libraryId);
        return converter.toList(rows, datacenterNames(rows));
    }

    private Map<UUID, String> datacenterNames(List<ContentLibraryDatacenterEntity> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        Set<UUID> ids = rows.stream().map(ContentLibraryDatacenterEntity::getDatacenterId).collect(Collectors.toSet());
        return datacenterRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(DatacenterEntity::getId, DatacenterEntity::getName));
    }

    @Transactional
    public void unpublishPlatform(UUID libraryId, UUID datacenterId) {
        contentLibraryRepository
                .findByIdAndTenantId(libraryId, ContentLibraryService.SYSTEM_TENANT_ID)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
        if (!contentLibraryDatacenterRepository.findByLibraryIdAndDatacenterId(libraryId, datacenterId).isPresent()) {
            throw new EntityNotFoundException("Publish mapping not found");
        }
        contentLibraryDatacenterRepository.deleteByLibraryIdAndDatacenterId(libraryId, datacenterId);
    }

    @Transactional
    public void unpublishTenant(UUID tenantId, UUID libraryId, UUID datacenterId) {
        contentLibraryRepository
                .findByIdAndTenantId(libraryId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
        if (!contentLibraryDatacenterRepository.findByLibraryIdAndDatacenterId(libraryId, datacenterId).isPresent()) {
            throw new EntityNotFoundException("Publish mapping not found");
        }
        contentLibraryDatacenterRepository.deleteByLibraryIdAndDatacenterId(libraryId, datacenterId);
    }
}
