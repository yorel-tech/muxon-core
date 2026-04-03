package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.TenantContentLibrariesApi;
import com.onetattva.infron.api.model.ContentLibrary;
import com.onetattva.infron.api.model.ContentLibraryCreate;
import com.onetattva.infron.api.model.ContentLibraryList;
import com.onetattva.infron.api.model.ContentLibraryUpdate;
import com.onetattva.infron.api.model.ContentSyncResponse;
import com.onetattva.infron.core.auth.Permission;
import com.onetattva.infron.core.auth.RequiresPermission;
import com.onetattva.infron.core.services.content.ContentLibraryService;
import com.onetattva.infron.core.services.content.ContentLibrarySyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class TenantContentLibrariesController implements TenantContentLibrariesApi {

    private static final String TENANT_SCOPE = "tenant";

    private final ContentLibraryService contentLibraryService;
    private final ContentLibrarySyncService contentLibrarySyncService;

    public TenantContentLibrariesController(
            ContentLibraryService contentLibraryService,
            ContentLibrarySyncService contentLibrarySyncService) {
        this.contentLibraryService = contentLibraryService;
        this.contentLibrarySyncService = contentLibrarySyncService;
    }

    @Override
    public ResponseEntity<ContentLibrary> createTenantContentLibrary(
            UUID tenantId, ContentLibraryCreate contentLibraryCreate) {
        return ResponseEntity.status(201)
                .body(contentLibraryService.create(contentLibraryCreate, TENANT_SCOPE, tenantId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<Void> deleteTenantContentLibrary(UUID tenantId, UUID libraryId) {
        contentLibraryService.requireTenantLibrary(tenantId, libraryId);
        contentLibraryService.delete(libraryId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ContentSyncResponse> ensureTenantContentLibrary(UUID tenantId, UUID libraryId) {
        contentLibraryService.requireTenantLibrary(tenantId, libraryId);
        return ResponseEntity.accepted().body(contentLibrarySyncService.enqueueSync(libraryId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentLibrary> getTenantContentLibrary(UUID tenantId, UUID libraryId) {
        contentLibraryService.requireTenantLibrary(tenantId, libraryId);
        return ResponseEntity.ok(contentLibraryService.get(libraryId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentLibraryList> listTenantContentLibraries(
            UUID tenantId, Integer page, Integer perPage) {
        return ResponseEntity.ok(contentLibraryService.listVisibleToTenant(tenantId, page, perPage));
    }

    @Override
    public ResponseEntity<ContentLibrary> replaceTenantContentLibrary(
            UUID tenantId, UUID libraryId, ContentLibraryUpdate contentLibraryUpdate) {
        contentLibraryService.requireTenantLibrary(tenantId, libraryId);
        return ResponseEntity.ok(contentLibraryService.replace(libraryId, contentLibraryUpdate));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentSyncResponse> syncTenantContentLibrary(UUID tenantId, UUID libraryId) {
        contentLibraryService.requireTenantLibrary(tenantId, libraryId);
        return ResponseEntity.accepted().body(contentLibrarySyncService.enqueueSync(libraryId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentLibrary> updateTenantContentLibrary(
            UUID tenantId, UUID libraryId, ContentLibraryUpdate contentLibraryUpdate) {
        contentLibraryService.requireTenantLibrary(tenantId, libraryId);
        return ResponseEntity.ok(contentLibraryService.update(libraryId, contentLibraryUpdate));
    }
}
