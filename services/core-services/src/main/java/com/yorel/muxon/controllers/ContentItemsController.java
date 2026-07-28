package com.yorel.muxon.controllers;

import com.yorel.muxon.api.ContentItemsApi;
import com.yorel.muxon.api.model.ContentItem;
import com.yorel.muxon.api.model.ContentItemCreate;
import com.yorel.muxon.api.model.ContentItemDownloadLink;
import com.yorel.muxon.api.model.ContentItemList;
import com.yorel.muxon.api.model.ContentItemUpdate;
import com.yorel.muxon.api.model.ContentItemUploadInitiate;
import com.yorel.muxon.api.model.ContentItemUploadSession;
import com.yorel.muxon.auth.Permission;
import com.yorel.muxon.auth.RequiresPermission;
import com.yorel.muxon.services.content.ContentItemService;
import com.yorel.muxon.services.content.ContentItemUploadService;
import com.yorel.muxon.services.content.ContentLibraryService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ContentItemsController implements ContentItemsApi {

    private final ContentItemService contentItemService;
    private final ContentLibraryService contentLibraryService;
    private final ContentItemUploadService contentItemUploadService;

    public ContentItemsController(
            ContentItemService contentItemService,
            ContentLibraryService contentLibraryService,
            ContentItemUploadService contentItemUploadService) {
        this.contentItemService = contentItemService;
        this.contentLibraryService = contentLibraryService;
        this.contentItemUploadService = contentItemUploadService;
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentItem> createPlatformContentItem(UUID libraryId, ContentItemCreate contentItemCreate) {
        contentLibraryService.requirePlatformLibrary(libraryId);
        return ResponseEntity.status(201).body(contentItemService.create(libraryId, contentItemCreate));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentItem> createTenantContentItem(
            UUID tenantId, UUID libraryId, ContentItemCreate contentItemCreate) {
        contentLibraryService.requireWriteAccess(tenantId, libraryId);
        return ResponseEntity.status(201).body(contentItemService.create(libraryId, contentItemCreate));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<Void> deletePlatformContentItem(UUID libraryId, UUID itemId) {
        contentLibraryService.requirePlatformLibrary(libraryId);
        contentItemService.deleteInLibrary(libraryId, itemId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<Void> deleteTenantContentItem(UUID tenantId, UUID libraryId, UUID itemId) {
        contentLibraryService.requireWriteAccess(tenantId, libraryId);
        contentItemService.deleteInLibrary(libraryId, itemId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentItemDownloadLink> downloadPlatformContentItem(UUID libraryId, UUID itemId) {
        contentLibraryService.requirePlatformLibrary(libraryId);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return ResponseEntity.ok(contentItemUploadService.buildDownloadLink(libraryId, itemId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentItemDownloadLink> downloadTenantContentItem(
            UUID tenantId, UUID libraryId, UUID itemId) {
        contentLibraryService.requireReadAccess(tenantId, libraryId);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return ResponseEntity.ok(contentItemUploadService.buildDownloadLink(libraryId, itemId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentItemUploadSession> initiatePlatformContentItemUpload(
            UUID libraryId, UUID itemId, ContentItemUploadInitiate contentItemUploadInitiate) {
        return ResponseEntity.status(201)
                .body(contentItemUploadService.initiatePlatform(libraryId, itemId, contentItemUploadInitiate));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentItemUploadSession> getPlatformContentItemUploadSession(
            UUID libraryId, UUID itemId, UUID uploadId) {
        return ResponseEntity.ok(contentItemUploadService.getPlatform(libraryId, itemId, uploadId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<Void> putPlatformContentItemUploadChunk(
            UUID libraryId,
            UUID itemId,
            UUID uploadId,
            String contentRange,
            Resource body) {
        int status = contentItemUploadService.putChunkPlatform(libraryId, itemId, uploadId, contentRange, body);
        return ResponseEntity.status(status).build();
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<Void> completePlatformContentItemUpload(UUID libraryId, UUID itemId, UUID uploadId) {
        contentItemUploadService.completePlatform(libraryId, itemId, uploadId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentItemUploadSession> initiateTenantContentItemUpload(
            UUID tenantId,
            UUID libraryId,
            UUID itemId,
            ContentItemUploadInitiate contentItemUploadInitiate) {
        return ResponseEntity.status(201)
                .body(contentItemUploadService.initiateTenant(tenantId, libraryId, itemId, contentItemUploadInitiate));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentItemUploadSession> getTenantContentItemUploadSession(
            UUID tenantId, UUID libraryId, UUID itemId, UUID uploadId) {
        return ResponseEntity.ok(contentItemUploadService.getTenant(tenantId, libraryId, itemId, uploadId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<Void> putTenantContentItemUploadChunk(
            UUID tenantId,
            UUID libraryId,
            UUID itemId,
            UUID uploadId,
            String contentRange,
            Resource body) {
        int status = contentItemUploadService.putChunkTenant(
                tenantId, libraryId, itemId, uploadId, contentRange, body);
        return ResponseEntity.status(status).build();
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<Void> completeTenantContentItemUpload(
            UUID tenantId, UUID libraryId, UUID itemId, UUID uploadId) {
        contentItemUploadService.completeTenant(tenantId, libraryId, itemId, uploadId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ContentItem> getPlatformContentItem(UUID libraryId, UUID itemId) {
        contentLibraryService.requirePlatformLibrary(libraryId);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return ResponseEntity.ok(contentItemService.get(itemId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentItem> getTenantContentItem(UUID tenantId, UUID libraryId, UUID itemId) {
        contentLibraryService.requireReadAccess(tenantId, libraryId);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return ResponseEntity.ok(contentItemService.get(itemId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentItemList> listPlatformContentItemsByLibrary(
            UUID libraryId, Integer page, Integer perPage) {
        contentLibraryService.requirePlatformLibrary(libraryId);
        return ResponseEntity.ok(contentItemService.listByLibrary(libraryId, page, perPage));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentItemList> listTenantContentItemsByLibrary(
            UUID tenantId, UUID libraryId, Integer page, Integer perPage) {
        contentLibraryService.requireReadAccess(tenantId, libraryId);
        return ResponseEntity.ok(contentItemService.listByLibrary(libraryId, page, perPage));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentItem> replacePlatformContentItem(
            UUID libraryId, UUID itemId, ContentItemUpdate contentItemUpdate) {
        contentLibraryService.requirePlatformLibrary(libraryId);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return ResponseEntity.ok(contentItemService.replace(itemId, contentItemUpdate));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentItem> replaceTenantContentItem(
            UUID tenantId, UUID libraryId, UUID itemId, ContentItemUpdate contentItemUpdate) {
        contentLibraryService.requireWriteAccess(tenantId, libraryId);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return ResponseEntity.ok(contentItemService.replace(itemId, contentItemUpdate));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentItem> updatePlatformContentItem(
            UUID libraryId, UUID itemId, ContentItemUpdate contentItemUpdate) {
        contentLibraryService.requirePlatformLibrary(libraryId);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return ResponseEntity.ok(contentItemService.update(itemId, contentItemUpdate));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentItem> updateTenantContentItem(
            UUID tenantId, UUID libraryId, UUID itemId, ContentItemUpdate contentItemUpdate) {
        contentLibraryService.requireWriteAccess(tenantId, libraryId);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return ResponseEntity.ok(contentItemService.update(itemId, contentItemUpdate));
    }
}
