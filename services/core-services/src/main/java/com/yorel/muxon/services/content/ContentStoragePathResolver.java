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
package com.yorel.muxon.services.content;

import com.yorel.muxon.common.EntityNotFoundException;
import com.yorel.muxon.config.MuxonProperties;
import com.yorel.muxon.db.model.ContentLibraryEntity;
import com.yorel.muxon.db.model.ContentStorageEntity;
import com.yorel.muxon.db.repository.ContentLibraryRepository;
import com.yorel.muxon.db.repository.ContentStorageRepository;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ContentStoragePathResolver {

  private final ContentLibraryRepository contentLibraryRepository;
  private final ContentStorageRepository contentStorageRepository;
  private final MuxonProperties muxonProperties;

  public ContentStoragePathResolver(
      ContentLibraryRepository contentLibraryRepository,
      ContentStorageRepository contentStorageRepository,
      MuxonProperties muxonProperties) {
    this.contentLibraryRepository = contentLibraryRepository;
    this.contentStorageRepository = contentStorageRepository;
    this.muxonProperties = muxonProperties;
  }

  public Path artifactRootForLibrary(UUID libraryId) {
    ContentLibraryEntity library =
        contentLibraryRepository
            .findById(libraryId)
            .orElseThrow(
                () -> new EntityNotFoundException("Content library not found: " + libraryId));
    return artifactRootForStorage(library.getContentStorageId());
  }

  public Path artifactRootForStorage(UUID contentStorageId) {
    ContentStorageEntity storage =
        contentStorageRepository
            .findById(contentStorageId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException("Content storage not found: " + contentStorageId));
    return artifactRootForStorage(storage);
  }

  public Path artifactRootForStorage(ContentStorageEntity storage) {
    String st = storage.getStorageType().toLowerCase(Locale.ROOT);
    Map<String, Object> cfg = storage.getConfig() != null ? storage.getConfig() : Map.of();
    if ("local".equals(st)) {
      Object p = cfg.get("path");
      String pathStr = p != null ? p.toString() : "";
      if (pathStr.isBlank()) {
        return legacyConfiguredRoot();
      }
      return Path.of(pathStr);
    }
    if ("nfs".equals(st)) {
      Object m = cfg.get("mountPath");
      if (m == null || m.toString().isBlank()) {
        throw new IllegalStateException(
            "NFS content storage " + storage.getId() + " has no mountPath");
      }
      return Path.of(m.toString());
    }
    if ("s3".equals(st)) {
      throw new UnsupportedOperationException(
          "S3 content storage does not expose a filesystem root; use object storage upload/download");
    }
    throw new IllegalStateException("Unknown content storage type: " + st);
  }

  private Path legacyConfiguredRoot() {
    String raw = muxonProperties.getContentLibraries().getUploadArtifactRoot();
    if (raw == null || raw.isBlank()) {
      return Path.of(System.getProperty("java.io.tmpdir"), "muxon-content-libraries");
    }
    return Path.of(raw);
  }
}
