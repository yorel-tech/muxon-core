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

import com.yorel.muxon.api.PlatformContentLibrariesApi;
import com.yorel.muxon.api.model.ContentItemDistributionList;
import com.yorel.muxon.api.model.ContentLibrary;
import com.yorel.muxon.api.model.ContentLibraryCreate;
import com.yorel.muxon.api.model.ContentLibraryDistribution;
import com.yorel.muxon.api.model.ContentLibraryDistributionList;
import com.yorel.muxon.api.model.ContentLibraryList;
import com.yorel.muxon.api.model.ContentLibraryUpdate;
import com.yorel.muxon.api.model.ContentReplicateResponse;
import com.yorel.muxon.api.model.ContentSyncResponse;
import com.yorel.muxon.api.model.PublishRequest;
import com.yorel.muxon.auth.Permission;
import com.yorel.muxon.auth.RequiresPermission;
import com.yorel.muxon.services.content.ContentLibraryDistributionItemService;
import com.yorel.muxon.services.content.ContentLibraryPublishService;
import com.yorel.muxon.services.content.ContentLibraryReplicateService;
import com.yorel.muxon.services.content.ContentLibraryService;
import com.yorel.muxon.services.content.ContentLibrarySyncService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

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
  public ResponseEntity<ContentLibrary> createPlatformContentLibrary(
      ContentLibraryCreate contentLibraryCreate) {
    return ResponseEntity.status(201)
        .body(
            contentLibraryService.create(
                contentLibraryCreate, ContentLibraryService.SYSTEM_TENANT_ID));
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
  public ResponseEntity<ContentLibraryList> listPlatformContentLibraries(
      Integer page, Integer perPage) {
    return ResponseEntity.ok(contentLibraryService.listPlatform(page, perPage));
  }

  @Override
  @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
  public ResponseEntity<ContentLibraryDistributionList> listPlatformContentLibraryDistributions(
      UUID libraryId) {
    return ResponseEntity.ok(contentLibraryPublishService.listForPlatformLibrary(libraryId));
  }

  @Override
  @RequiresPermission(Permission.CONTENT_LIBRARY_READ)
  public ResponseEntity<ContentItemDistributionList> listPlatformContentLibraryDistributionItems(
      UUID libraryId, UUID distributionId, String status, Integer page, Integer perPage) {
    return ResponseEntity.ok(
        contentLibraryDistributionItemService.listForPlatformLibrary(
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
  public ResponseEntity<Void> unpublishPlatformContentLibraryDistribution(
      UUID libraryId, UUID distributionId) {
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
