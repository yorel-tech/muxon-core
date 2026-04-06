package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.TenantContentLibrariesApi;
import com.onetattva.infron.api.model.ContentLibrary;
import com.onetattva.infron.api.model.ContentLibraryCreate;
import com.onetattva.infron.api.model.ContentLibraryDatacenter;
import com.onetattva.infron.api.model.ContentLibraryDatacenterList;
import com.onetattva.infron.api.model.ContentLibraryList;
import com.onetattva.infron.api.model.ContentLibraryUpdate;
import com.onetattva.infron.api.model.ContentReplicateResponse;
import com.onetattva.infron.api.model.PublishRequest;
import com.onetattva.infron.core.auth.Permission;
import com.onetattva.infron.core.auth.RequiresPermission;
import com.onetattva.infron.core.services.content.ContentLibraryPublishService;
import com.onetattva.infron.core.services.content.ContentLibraryReplicateService;
import com.onetattva.infron.core.services.content.ContentLibraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class TenantContentLibrariesController implements TenantContentLibrariesApi {

    private final ContentLibraryService contentLibraryService;
    private final ContentLibraryPublishService contentLibraryPublishService;
    private final ContentLibraryReplicateService contentLibraryReplicateService;

    public TenantContentLibrariesController(
            ContentLibraryService contentLibraryService,
            ContentLibraryPublishService contentLibraryPublishService,
            ContentLibraryReplicateService contentLibraryReplicateService) {
        this.contentLibraryService = contentLibraryService;
        this.contentLibraryPublishService = contentLibraryPublishService;
        this.contentLibraryReplicateService = contentLibraryReplicateService;
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
    public ResponseEntity<ContentLibraryDatacenterList> listTenantContentLibraryDatacenters(
            UUID tenantId, UUID libraryId) {
        contentLibraryService.requireReadAccess(tenantId, libraryId);
        return ResponseEntity.ok(contentLibraryPublishService.listForTenantLibrary(tenantId, libraryId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentLibraryDatacenter> publishTenantContentLibrary(
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
    public ResponseEntity<ContentReplicateResponse> replicateTenantContentLibraryDatacenter(
            UUID tenantId, UUID libraryId, UUID datacenterId) {
        contentLibraryService.requireWriteAccess(tenantId, libraryId);
        return ResponseEntity.accepted()
                .body(contentLibraryReplicateService.replicateToProviderStorage(libraryId, datacenterId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<Void> unpublishTenantContentLibraryDatacenter(
            UUID tenantId, UUID libraryId, UUID datacenterId) {
        contentLibraryPublishService.unpublishTenant(tenantId, libraryId, datacenterId);
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
