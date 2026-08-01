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

import com.yorel.muxon.db.model.ContentItemEntity;
import com.yorel.muxon.db.model.ContentLibraryEntity;
import com.yorel.muxon.db.model.ContentStorageEntity;
import com.yorel.muxon.db.repository.ContentLibraryRepository;
import com.yorel.muxon.db.repository.ContentStorageRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * Removes persisted blobs for a content item (local/NFS tree or S3 prefix under the item
 * directory).
 */
@Service
public class ContentItemArtifactDeletionService {

  private final ContentLibraryRepository contentLibraryRepository;
  private final ContentStorageRepository contentStorageRepository;
  private final ContentStoragePathResolver contentStoragePathResolver;
  private final ContentStorageS3Uploader contentStorageS3Uploader;
  private final ContentLibraryProviderPathBuilder pathBuilder;

  public ContentItemArtifactDeletionService(
      ContentLibraryRepository contentLibraryRepository,
      ContentStorageRepository contentStorageRepository,
      ContentStoragePathResolver contentStoragePathResolver,
      ContentStorageS3Uploader contentStorageS3Uploader,
      ContentLibraryProviderPathBuilder pathBuilder) {
    this.contentLibraryRepository = contentLibraryRepository;
    this.contentStorageRepository = contentStorageRepository;
    this.contentStoragePathResolver = contentStoragePathResolver;
    this.contentStorageS3Uploader = contentStorageS3Uploader;
    this.pathBuilder = pathBuilder;
  }

  public void deleteStoredArtifacts(ContentItemEntity item) {
    String relative = item.getProviderRelativePath();
    if (relative == null || relative.isBlank()) {
      pathBuilder.applyProviderPaths(item);
      relative = item.getProviderRelativePath();
    }
    if (relative == null || relative.isBlank()) {
      return;
    }

    UUID libraryId = item.getLibraryId();
    ContentLibraryEntity library = contentLibraryRepository.findById(libraryId).orElse(null);
    if (library == null || library.getContentStorageId() == null) {
      return;
    }
    ContentStorageEntity storage =
        contentStorageRepository.findById(library.getContentStorageId()).orElse(null);
    if (storage == null) {
      return;
    }

    String normalizedRelative = relative.replace('\\', '/');
    Path relPath = Path.of(normalizedRelative);
    Path parent = relPath.getParent();

    try {
      if (contentStorageS3Uploader.isS3(storage)) {
        if (parent == null || parent.toString().isBlank()) {
          contentStorageS3Uploader.deleteRelativeObject(storage, normalizedRelative);
        } else {
          String itemDir = parent.toString().replace('\\', '/');
          contentStorageS3Uploader.deleteAllUnderRelativePrefix(storage, itemDir);
        }
        return;
      }

      Path root =
          contentStoragePathResolver.artifactRootForLibrary(libraryId).toAbsolutePath().normalize();
      if (parent == null || parent.toString().isBlank()) {
        Path file = root.resolve(normalizedRelative).normalize();
        if (!file.startsWith(root)) {
          throw new IllegalStateException("Refusing to delete outside artifact root");
        }
        Files.deleteIfExists(file);
        return;
      }
      Path dir = root.resolve(parent.toString().replace('\\', '/')).normalize();
      if (!dir.startsWith(root)) {
        throw new IllegalStateException("Refusing to delete outside artifact root");
      }
      deleteDirectoryRecursive(dir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void deleteDirectoryRecursive(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    try (Stream<Path> walk = Files.walk(dir)) {
      walk.sorted(Comparator.reverseOrder())
          .forEach(
              p -> {
                try {
                  Files.deleteIfExists(p);
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });
    }
  }
}
