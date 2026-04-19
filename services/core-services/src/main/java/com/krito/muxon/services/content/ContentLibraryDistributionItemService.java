package com.krito.muxon.services.content;

import com.krito.muxon.api.model.ContentItemDistribution;
import com.krito.muxon.api.model.ContentItemDistributionList;
import com.krito.muxon.common.EntityNotFoundException;
import com.krito.muxon.db.model.ContentItemDistributionEntity;
import com.krito.muxon.db.model.ContentItemEntity;
import com.krito.muxon.db.repository.ContentItemDistributionRepository;
import com.krito.muxon.db.repository.ContentItemRepository;
import com.krito.muxon.db.repository.ContentLibraryDistributionRepository;
import com.krito.muxon.db.repository.ContentLibraryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class ContentLibraryDistributionItemService {

    private final ContentLibraryRepository contentLibraryRepository;
    private final ContentLibraryDistributionRepository distributionRepository;
    private final ContentItemDistributionRepository itemDistributionRepository;
    private final ContentItemRepository contentItemRepository;

    public ContentLibraryDistributionItemService(
            ContentLibraryRepository contentLibraryRepository,
            ContentLibraryDistributionRepository distributionRepository,
            ContentItemDistributionRepository itemDistributionRepository,
            ContentItemRepository contentItemRepository) {
        this.contentLibraryRepository = contentLibraryRepository;
        this.distributionRepository = distributionRepository;
        this.itemDistributionRepository = itemDistributionRepository;
        this.contentItemRepository = contentItemRepository;
    }

    public ContentItemDistributionList listForPlatformLibrary(UUID libraryId, UUID distributionId, String status, Integer page, Integer perPage) {
        contentLibraryRepository
                .findByIdAndTenantId(libraryId, ContentLibraryService.SYSTEM_TENANT_ID)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
        return listInternal(libraryId, distributionId, status, page, perPage);
    }

    public ContentItemDistributionList listForTenantLibrary(
            UUID tenantId, UUID libraryId, UUID distributionId, String status, Integer page, Integer perPage) {
        contentLibraryRepository
                .findByIdAndTenantIdIn(libraryId, java.util.List.of(tenantId, ContentLibraryService.SYSTEM_TENANT_ID))
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
        return listInternal(libraryId, distributionId, status, page, perPage);
    }

    private ContentItemDistributionList listInternal(UUID libraryId, UUID distributionId, String status, Integer page, Integer perPage) {
        var dist = distributionRepository
                .findById(distributionId)
                .orElseThrow(() -> new EntityNotFoundException("Distribution not found: " + distributionId));
        if (!dist.getLibraryId().equals(libraryId)) {
            throw new EntityNotFoundException("Distribution not found for this library");
        }

        int p = page == null || page < 1 ? 1 : page;
        int size = perPage == null || perPage < 1 ? 20 : Math.min(perPage, 200);
        var pageable = PageRequest.of(p - 1, size, Sort.by("contentItemId"));

        Page<ContentItemDistributionEntity> result =
                (status != null && !status.isBlank())
                        ? itemDistributionRepository.findByDistributionIdAndStatus(
                                distributionId, status.toUpperCase(), pageable)
                        : itemDistributionRepository.findByDistributionId(distributionId, pageable);

        ContentItemDistributionList out = new ContentItemDistributionList();
        out.setPage(p);
        out.setPerPage(size);
        out.setTotal(result.getTotalElements());
        out.setItems(result.getContent().stream().map(this::toApi).toList());
        return out;
    }

    private ContentItemDistribution toApi(ContentItemDistributionEntity e) {
        ContentItemDistribution api = new ContentItemDistribution();
        api.setId(e.getId());
        api.setContentItemId(e.getContentItemId());
        api.setDistributionId(e.getDistributionId());
        api.setStatus(ContentItemDistribution.StatusEnum.fromValue(e.getStatus()));
        api.setChecksumVerified(e.isChecksumVerified());
        api.setSizeBytes(e.getSizeBytes());
        api.setErrorMessage(e.getErrorMessage());
        api.setRetryCount(e.getRetryCount());
        api.setLastUpdatedAt(e.getLastUpdatedAt().atOffset(ZoneOffset.UTC));
        contentItemRepository.findById(e.getContentItemId()).ifPresent(item -> {
            api.setName(item.getName());
            api.setContentType(item.getContentType());
        });
        return api;
    }
}
