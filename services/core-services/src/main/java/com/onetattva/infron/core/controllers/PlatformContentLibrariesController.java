package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.PlatformContentLibrariesApi;
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
public class PlatformContentLibrariesController implements PlatformContentLibrariesApi {

    private static final String PLATFORM_SCOPE = "platform";

    private final ContentLibraryService contentLibraryService;
    private final ContentLibrarySyncService contentLibrarySyncService;

    public PlatformContentLibrariesController(
            ContentLibraryService contentLibraryService,
            ContentLibrarySyncService contentLibrarySyncService) {
        this.contentLibraryService = contentLibraryService;
        this.contentLibrarySyncService = contentLibrarySyncService;
    }

    @Override
    public ResponseEntity<ContentLibrary> createPlatformContentLibrary(ContentLibraryCreate contentLibraryCreate) {
        return ResponseEntity.status(201)
                .body(contentLibraryService.create(contentLibraryCreate, PLATFORM_SCOPE, null));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<Void> deletePlatformContentLibrary(UUID libraryId) {
        contentLibraryService.getByScope(libraryId, PLATFORM_SCOPE);
        contentLibraryService.delete(libraryId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentSyncResponse> ensurePlatformContentLibrary(UUID libraryId) {
        contentLibraryService.getByScope(libraryId, PLATFORM_SCOPE);
        return ResponseEntity.accepted().body(contentLibrarySyncService.enqueueSync(libraryId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentLibrary> getPlatformContentLibrary(UUID libraryId) {
        return ResponseEntity.ok(contentLibraryService.getByScope(libraryId, PLATFORM_SCOPE));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentLibraryList> listPlatformContentLibraries(Integer page, Integer perPage) {
        return ResponseEntity.ok(contentLibraryService.listByScope(PLATFORM_SCOPE, page, perPage));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentLibrary> replacePlatformContentLibrary(
            UUID libraryId,
            ContentLibraryUpdate contentLibraryUpdate) {
        contentLibraryService.getByScope(libraryId, PLATFORM_SCOPE);
        return ResponseEntity.ok(contentLibraryService.replace(libraryId, contentLibraryUpdate));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentSyncResponse> syncPlatformContentLibrary(UUID libraryId) {
        contentLibraryService.getByScope(libraryId, PLATFORM_SCOPE);
        return ResponseEntity.accepted().body(contentLibrarySyncService.enqueueSync(libraryId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentLibrary> updatePlatformContentLibrary(
            UUID libraryId,
            ContentLibraryUpdate contentLibraryUpdate) {
        contentLibraryService.getByScope(libraryId, PLATFORM_SCOPE);
        return ResponseEntity.ok(contentLibraryService.update(libraryId, contentLibraryUpdate));
    }
}
