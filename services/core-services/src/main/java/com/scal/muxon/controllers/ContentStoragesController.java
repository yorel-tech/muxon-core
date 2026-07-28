package com.scal.muxon.controllers;

import com.scal.muxon.api.ContentStoragesApi;
import com.scal.muxon.api.model.ContentStorage;
import com.scal.muxon.api.model.ContentStorageCreate;
import com.scal.muxon.api.model.ContentStorageList;
import com.scal.muxon.api.model.ContentStorageUpdate;
import com.scal.muxon.auth.Permission;
import com.scal.muxon.auth.RequiresPermission;
import com.scal.muxon.services.content.ContentStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ContentStoragesController implements ContentStoragesApi {

    private final ContentStorageService contentStorageService;

    public ContentStoragesController(ContentStorageService contentStorageService) {
        this.contentStorageService = contentStorageService;
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentStorage> createContentStorage(ContentStorageCreate contentStorageCreate) {
        return ResponseEntity.status(201).body(contentStorageService.create(contentStorageCreate));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<Void> deleteContentStorage(UUID contentStorageId) {
        contentStorageService.delete(contentStorageId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentStorage> getContentStorage(UUID contentStorageId) {
        return ResponseEntity.ok(contentStorageService.get(contentStorageId));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
    public ResponseEntity<ContentStorageList> listContentStorages(Integer page, Integer perPage) {
        return ResponseEntity.ok(contentStorageService.list(page, perPage));
    }

    @Override
    @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
    public ResponseEntity<ContentStorage> updateContentStorage(
            UUID contentStorageId, ContentStorageUpdate contentStorageUpdate) {
        return ResponseEntity.ok(contentStorageService.update(contentStorageId, contentStorageUpdate));
    }
}
