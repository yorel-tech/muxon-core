package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.ContentItemsApi;
import com.onetattva.infron.api.model.ContentFetchResponse;
import com.onetattva.infron.api.model.ContentItem;
import com.onetattva.infron.api.model.ContentItemCreate;
import com.onetattva.infron.api.model.ContentItemList;
import com.onetattva.infron.api.model.ContentItemUpdate;
import com.onetattva.infron.core.auth.Permission;
import com.onetattva.infron.core.auth.RequiresPermission;
import com.onetattva.infron.core.services.content.ContentItemService;
import com.onetattva.infron.core.services.content.ContentLibraryService;
import com.onetattva.infron.core.services.content.ContentLibrarySyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ContentItemsController implements ContentItemsApi {

    private static final String PLATFORM_SCOPE = "platform";

    private final ContentItemService contentItemService;
    private final ContentLibrarySyncService contentLibrarySyncService;
    private final ContentLibraryService contentLibraryService;

    public ContentItemsController(
            ContentItemService contentItemService,
            ContentLibrarySyncService contentLibrarySyncService,
            ContentLibraryService contentLibraryService) {
        this.contentItemService = contentItemService;
        this.contentLibrarySyncService = contentLibrarySyncService;
        this.contentLibraryService = contentLibraryService;
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentItem> createPlatformContentItem(UUID libraryId, ContentItemCreate contentItemCreate) {
        contentLibraryService.getByScope(libraryId, PLATFORM_SCOPE);
        return ResponseEntity.status(201).body(contentItemService.create(libraryId, contentItemCreate));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentItem> createTenantContentItem(
            UUID tenantId, UUID libraryId, ContentItemCreate contentItemCreate) {
        contentLibraryService.requireTenantLibrary(tenantId, libraryId);
        return ResponseEntity.status(201).body(contentItemService.create(libraryId, contentItemCreate));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<Void> deletePlatformContentItem(UUID libraryId, UUID itemId) {
        contentLibraryService.getByScope(libraryId, PLATFORM_SCOPE);
        contentItemService.deleteInLibrary(libraryId, itemId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<Void> deleteTenantContentItem(UUID tenantId, UUID libraryId, UUID itemId) {
        contentLibraryService.requireTenantLibrary(tenantId, libraryId);
        contentItemService.deleteInLibrary(libraryId, itemId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ContentFetchResponse> fetchPlatformContentItem(UUID libraryId, UUID itemId) {
        contentLibraryService.getByScope(libraryId, PLATFORM_SCOPE);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return ResponseEntity.accepted().body(contentLibrarySyncService.enqueueFetch(libraryId, itemId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentFetchResponse> fetchTenantContentItem(UUID tenantId, UUID libraryId, UUID itemId) {
        contentLibraryService.requireTenantLibrary(tenantId, libraryId);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return ResponseEntity.accepted().body(contentLibrarySyncService.enqueueFetch(libraryId, itemId));
    }

    @Override
    public ResponseEntity<ContentItem> getPlatformContentItem(UUID libraryId, UUID itemId) {
        contentLibraryService.getByScope(libraryId, PLATFORM_SCOPE);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return ResponseEntity.ok(contentItemService.get(itemId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentItem> getTenantContentItem(UUID tenantId, UUID libraryId, UUID itemId) {
        contentLibraryService.requireTenantLibrary(tenantId, libraryId);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return ResponseEntity.ok(contentItemService.get(itemId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentItemList> listPlatformContentItemsByLibrary(
            UUID libraryId, Integer page, Integer perPage) {
        contentLibraryService.getByScope(libraryId, PLATFORM_SCOPE);
        return ResponseEntity.ok(contentItemService.listByLibrary(libraryId, page, perPage));
    }

    @Override
    public ResponseEntity<ContentItemList> listTenantContentItemsByLibrary(
            UUID tenantId, UUID libraryId, Integer page, Integer perPage) {
        contentLibraryService.requireTenantLibrary(tenantId, libraryId);
        return ResponseEntity.ok(contentItemService.listByLibrary(libraryId, page, perPage));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentItem> replacePlatformContentItem(
            UUID libraryId, UUID itemId, ContentItemUpdate contentItemUpdate) {
        contentLibraryService.getByScope(libraryId, PLATFORM_SCOPE);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return ResponseEntity.ok(contentItemService.replace(itemId, contentItemUpdate));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentItem> replaceTenantContentItem(
            UUID tenantId, UUID libraryId, UUID itemId, ContentItemUpdate contentItemUpdate) {
        contentLibraryService.requireTenantLibrary(tenantId, libraryId);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return ResponseEntity.ok(contentItemService.replace(itemId, contentItemUpdate));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentItem> updatePlatformContentItem(
            UUID libraryId, UUID itemId, ContentItemUpdate contentItemUpdate) {
        contentLibraryService.getByScope(libraryId, PLATFORM_SCOPE);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return ResponseEntity.ok(contentItemService.update(itemId, contentItemUpdate));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentItem> updateTenantContentItem(
            UUID tenantId, UUID libraryId, UUID itemId, ContentItemUpdate contentItemUpdate) {
        contentLibraryService.requireTenantLibrary(tenantId, libraryId);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return ResponseEntity.ok(contentItemService.update(itemId, contentItemUpdate));
    }
}
