package com.scal.muxon.controllers;

import com.scal.muxon.api.TenantContentLibrariesApi;
import com.scal.muxon.api.model.ContentItemDistributionList;
import com.scal.muxon.api.model.ContentLibrary;
import com.scal.muxon.api.model.ContentLibraryCreate;
import com.scal.muxon.api.model.ContentLibraryDistribution;
import com.scal.muxon.api.model.ContentLibraryDistributionList;
import com.scal.muxon.api.model.ContentLibraryList;
import com.scal.muxon.api.model.ContentLibraryUpdate;
import com.scal.muxon.api.model.ContentReplicateResponse;
import com.scal.muxon.api.model.PublishRequest;
import com.scal.muxon.auth.Permission;
import com.scal.muxon.auth.RequiresPermission;
import com.scal.muxon.services.content.ContentLibraryDistributionItemService;
import com.scal.muxon.services.content.ContentLibraryPublishService;
import com.scal.muxon.services.content.ContentLibraryReplicateService;
import com.scal.muxon.services.content.ContentLibraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class TenantContentLibrariesController implements TenantContentLibrariesApi {

    private final ContentLibraryService contentLibraryService;
    private final ContentLibraryPublishService contentLibraryPublishService;
    private final ContentLibraryReplicateService contentLibraryReplicateService;
    private final ContentLibraryDistributionItemService contentLibraryDistributionItemService;

    public TenantContentLibrariesController(
            ContentLibraryService contentLibraryService,
            ContentLibraryPublishService contentLibraryPublishService,
            ContentLibraryReplicateService contentLibraryReplicateService,
            ContentLibraryDistributionItemService contentLibraryDistributionItemService) {
        this.contentLibraryService = contentLibraryService;
        this.contentLibraryPublishService = contentLibraryPublishService;
        this.contentLibraryReplicateService = contentLibraryReplicateService;
        this.contentLibraryDistributionItemService = contentLibraryDistributionItemService;
    }

    @Override
    public ResponseEntity<ContentLibrary> createTenantContentLibrary(
            UUID tenantId, ContentLibraryCreate contentLibraryCreate) {
        return ResponseEntity.status(201).body(contentLibraryService.create(contentLibraryCreate, tenantId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<Void> deleteTenantContentLibrary(UUID tenantId, UUID libraryId) {
        contentLibraryService.requireWriteAccess(tenantId, libraryId);
        contentLibraryService.delete(libraryId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentLibrary> getTenantContentLibrary(UUID tenantId, UUID libraryId) {
        contentLibraryService.requireReadAccess(tenantId, libraryId);
        return ResponseEntity.ok(contentLibraryService.get(libraryId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentLibraryList> listTenantContentLibraries(
            UUID tenantId, Integer page, Integer perPage) {
        return ResponseEntity.ok(contentLibraryService.listVisibleToTenant(tenantId, page, perPage));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentLibraryDistributionList> listTenantContentLibraryDistributions(
            UUID tenantId, UUID libraryId) {
        contentLibraryService.requireReadAccess(tenantId, libraryId);
        return ResponseEntity.ok(contentLibraryPublishService.listForTenantLibrary(tenantId, libraryId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentItemDistributionList> listTenantContentLibraryDistributionItems(
            UUID tenantId, UUID libraryId, UUID distributionId, String status, Integer page, Integer perPage) {
        contentLibraryService.requireReadAccess(tenantId, libraryId);
        return ResponseEntity.ok(contentLibraryDistributionItemService.listForTenantLibrary(
                tenantId, libraryId, distributionId, status, page, perPage));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentLibraryDistribution> publishTenantContentLibrary(
            UUID tenantId, UUID libraryId, PublishRequest publishRequest) {
        return ResponseEntity.status(201)
                .body(contentLibraryPublishService.publishTenant(tenantId, libraryId, publishRequest));
    }

    @Override
    public ResponseEntity<ContentLibrary> replaceTenantContentLibrary(
            UUID tenantId, UUID libraryId, ContentLibraryUpdate contentLibraryUpdate) {
        contentLibraryService.requireWriteAccess(tenantId, libraryId);
        return ResponseEntity.ok(contentLibraryService.replace(libraryId, contentLibraryUpdate));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentReplicateResponse> replicateTenantContentLibraryDistribution(
            UUID tenantId, UUID libraryId, UUID distributionId) {
        contentLibraryService.requireWriteAccess(tenantId, libraryId);
        return ResponseEntity.accepted()
                .body(contentLibraryReplicateService.replicateToProviderStorage(libraryId, distributionId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<Void> unpublishTenantContentLibraryDistribution(
            UUID tenantId, UUID libraryId, UUID distributionId) {
        contentLibraryPublishService.unpublishTenant(tenantId, libraryId, distributionId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentLibrary> updateTenantContentLibrary(
            UUID tenantId, UUID libraryId, ContentLibraryUpdate contentLibraryUpdate) {
        contentLibraryService.requireWriteAccess(tenantId, libraryId);
        return ResponseEntity.ok(contentLibraryService.update(libraryId, contentLibraryUpdate));
    }
}
