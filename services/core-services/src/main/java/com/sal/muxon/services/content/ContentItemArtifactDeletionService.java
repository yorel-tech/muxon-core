package com.sal.muxon.services.content;

import com.sal.muxon.db.model.ContentItemEntity;
import com.sal.muxon.db.model.ContentLibraryEntity;
import com.sal.muxon.db.model.ContentStorageEntity;
import com.sal.muxon.db.repository.ContentLibraryRepository;
import com.sal.muxon.db.repository.ContentStorageRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Removes persisted blobs for a content item (local/NFS tree or S3 prefix under the item directory).
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

            Path root = contentStoragePathResolver.artifactRootForLibrary(libraryId).toAbsolutePath().normalize();
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
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
