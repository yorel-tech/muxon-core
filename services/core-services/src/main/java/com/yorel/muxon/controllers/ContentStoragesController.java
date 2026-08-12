/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.controllers;

import com.yorel.muxon.api.ContentStoragesApi;
import com.yorel.muxon.api.model.ContentStorage;
import com.yorel.muxon.api.model.ContentStorageCreate;
import com.yorel.muxon.api.model.ContentStorageList;
import com.yorel.muxon.api.model.ContentStorageUpdate;
import com.yorel.muxon.auth.Permission;
import com.yorel.muxon.auth.RequiresPermission;
import com.yorel.muxon.services.content.ContentStorageService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContentStoragesController implements ContentStoragesApi {

  private final ContentStorageService contentStorageService;

  public ContentStoragesController(ContentStorageService contentStorageService) {
    this.contentStorageService = contentStorageService;
  }

  @Override
  @RequiresPermission(Permission.CONTENT_LIBRARY_WRITE)
  public ResponseEntity<ContentStorage> createContentStorage(
      ContentStorageCreate contentStorageCreate) {
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
