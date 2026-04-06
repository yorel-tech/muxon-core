package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.PlatformContentLibrariesApi;
import com.onetattva.infron.api.model.ContentLibrary;
import com.onetattva.infron.api.model.ContentLibraryCreate;
import com.onetattva.infron.api.model.ContentLibraryDatacenter;
import com.onetattva.infron.api.model.ContentLibraryDatacenterList;
import com.onetattva.infron.api.model.ContentLibraryList;
import com.onetattva.infron.api.model.ContentLibraryUpdate;
import com.onetattva.infron.api.model.ContentReplicateResponse;
import com.onetattva.infron.api.model.ContentSyncResponse;
import com.onetattva.infron.api.model.PublishRequest;
import com.onetattva.infron.core.auth.Permission;
import com.onetattva.infron.core.auth.RequiresPermission;
import com.onetattva.infron.core.services.content.ContentLibraryPublishService;
import com.onetattva.infron.core.services.content.ContentLibraryReplicateService;
import com.onetattva.infron.core.services.content.ContentLibraryService;
import com.onetattva.infron.core.services.content.ContentLibrarySyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class PlatformContentLibrariesController implements PlatformContentLibrariesApi {

    private final ContentLibraryService contentLibraryService;
    private final ContentLibrarySyncService contentLibrarySyncService;
    private final ContentLibraryReplicateService contentLibraryReplicateService;
    private final ContentLibraryPublishService contentLibraryPublishService;

    public PlatformContentLibrariesController(
            ContentLibraryService contentLibraryService,
            ContentLibrarySyncService contentLibrarySyncService,
            ContentLibraryReplicateService contentLibraryReplicateService,
            ContentLibraryPublishService contentLibraryPublishService) {
        this.contentLibraryService = contentLibraryService;
        this.contentLibrarySyncService = contentLibrarySyncService;
        this.contentLibraryReplicateService = contentLibraryReplicateService;
        this.contentLibraryPublishService = contentLibraryPublishService;
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
    public ResponseEntity<ContentLibraryDatacenterList> listPlatformContentLibraryDatacenters(UUID libraryId) {
        return ResponseEntity.ok(contentLibraryPublishService.listForPlatformLibrary(libraryId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentLibraryDatacenter> publishPlatformContentLibrary(
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
    public ResponseEntity<ContentReplicateResponse> replicatePlatformContentLibraryDatacenter(
            UUID libraryId, UUID datacenterId) {
        contentLibraryService.requirePlatformLibrary(libraryId);
        return ResponseEntity.accepted()
                .body(contentLibraryReplicateService.replicateToProviderStorage(libraryId, datacenterId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentSyncResponse> syncPlatformContentLibrary(UUID libraryId) {
        contentLibraryService.requirePlatformLibrary(libraryId);
        return ResponseEntity.accepted().body(contentLibrarySyncService.enqueueSync(libraryId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<Void> unpublishPlatformContentLibraryDatacenter(UUID libraryId, UUID datacenterId) {
        contentLibraryPublishService.unpublishPlatform(libraryId, datacenterId);
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
