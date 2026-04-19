package com.krito.muxon.controllers;

import com.krito.muxon.api.PlatformContentLibrariesApi;
import com.krito.muxon.api.model.ContentItemDistributionList;
import com.krito.muxon.api.model.ContentLibrary;
import com.krito.muxon.api.model.ContentLibraryCreate;
import com.krito.muxon.api.model.ContentLibraryDistribution;
import com.krito.muxon.api.model.ContentLibraryDistributionList;
import com.krito.muxon.api.model.ContentLibraryList;
import com.krito.muxon.api.model.ContentLibraryUpdate;
import com.krito.muxon.api.model.ContentReplicateResponse;
import com.krito.muxon.api.model.ContentSyncResponse;
import com.krito.muxon.api.model.PublishRequest;
import com.krito.muxon.auth.Permission;
import com.krito.muxon.auth.RequiresPermission;
import com.krito.muxon.services.content.ContentLibraryDistributionItemService;
import com.krito.muxon.services.content.ContentLibraryPublishService;
import com.krito.muxon.services.content.ContentLibraryReplicateService;
import com.krito.muxon.services.content.ContentLibraryService;
import com.krito.muxon.services.content.ContentLibrarySyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class PlatformContentLibrariesController implements PlatformContentLibrariesApi {

    private final ContentLibraryService contentLibraryService;
    private final ContentLibrarySyncService contentLibrarySyncService;
    private final ContentLibraryReplicateService contentLibraryReplicateService;
    private final ContentLibraryPublishService contentLibraryPublishService;
    private final ContentLibraryDistributionItemService contentLibraryDistributionItemService;

    public PlatformContentLibrariesController(
            ContentLibraryService contentLibraryService,
            ContentLibrarySyncService contentLibrarySyncService,
            ContentLibraryReplicateService contentLibraryReplicateService,
            ContentLibraryPublishService contentLibraryPublishService,
            ContentLibraryDistributionItemService contentLibraryDistributionItemService) {
        this.contentLibraryService = contentLibraryService;
        this.contentLibrarySyncService = contentLibrarySyncService;
        this.contentLibraryReplicateService = contentLibraryReplicateService;
        this.contentLibraryPublishService = contentLibraryPublishService;
        this.contentLibraryDistributionItemService = contentLibraryDistributionItemService;
    }

    @Override
    public ResponseEntity<ContentLibrary> createPlatformContentLibrary(ContentLibraryCreate contentLibraryCreate) {
        return ResponseEntity.status(201)
                .body(contentLibraryService.create(contentLibraryCreate, ContentLibraryService.SYSTEM_TENANT_ID));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<Void> deletePlatformContentLibrary(UUID libraryId) {
        contentLibraryService.requirePlatformLibrary(libraryId);
        contentLibraryService.delete(libraryId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentLibrary> getPlatformContentLibrary(UUID libraryId) {
        return ResponseEntity.ok(contentLibraryService.getPlatform(libraryId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentLibraryList> listPlatformContentLibraries(Integer page, Integer perPage) {
        return ResponseEntity.ok(contentLibraryService.listPlatform(page, perPage));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentLibraryDistributionList> listPlatformContentLibraryDistributions(UUID libraryId) {
        return ResponseEntity.ok(contentLibraryPublishService.listForPlatformLibrary(libraryId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentItemDistributionList> listPlatformContentLibraryDistributionItems(
            UUID libraryId, UUID distributionId, String status, Integer page, Integer perPage) {
        return ResponseEntity.ok(contentLibraryDistributionItemService.listForPlatformLibrary(
                libraryId, distributionId, status, page, perPage));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentLibraryDistribution> publishPlatformContentLibrary(
            UUID libraryId, PublishRequest publishRequest) {
        return ResponseEntity.status(201)
                .body(contentLibraryPublishService.publishPlatform(libraryId, publishRequest));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentLibrary> replacePlatformContentLibrary(
            UUID libraryId, ContentLibraryUpdate contentLibraryUpdate) {
        contentLibraryService.requirePlatformLibrary(libraryId);
        return ResponseEntity.ok(contentLibraryService.replace(libraryId, contentLibraryUpdate));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentReplicateResponse> replicatePlatformContentLibrary(UUID libraryId) {
        contentLibraryService.requirePlatformLibrary(libraryId);
        return ResponseEntity.accepted()
                .body(contentLibraryReplicateService.replicateRemoteLibraryToContentStore(libraryId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentReplicateResponse> replicatePlatformContentLibraryDistribution(
            UUID libraryId, UUID distributionId) {
        contentLibraryService.requirePlatformLibrary(libraryId);
        return ResponseEntity.accepted()
                .body(contentLibraryReplicateService.replicateToProviderStorage(libraryId, distributionId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentSyncResponse> syncPlatformContentLibrary(UUID libraryId) {
        contentLibraryService.requirePlatformLibrary(libraryId);
        return ResponseEntity.accepted().body(contentLibrarySyncService.enqueueSync(libraryId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<Void> unpublishPlatformContentLibraryDistribution(UUID libraryId, UUID distributionId) {
        contentLibraryPublishService.unpublishPlatform(libraryId, distributionId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentLibrary> updatePlatformContentLibrary(
            UUID libraryId, ContentLibraryUpdate contentLibraryUpdate) {
        contentLibraryService.requirePlatformLibrary(libraryId);
        return ResponseEntity.ok(contentLibraryService.update(libraryId, contentLibraryUpdate));
    }
}
