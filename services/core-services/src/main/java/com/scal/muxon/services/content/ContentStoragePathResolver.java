package com.scal.muxon.services.content;

import com.scal.muxon.common.EntityNotFoundException;
import com.scal.muxon.config.MuxonProperties;
import com.scal.muxon.db.model.ContentLibraryEntity;
import com.scal.muxon.db.model.ContentStorageEntity;
import com.scal.muxon.db.repository.ContentLibraryRepository;
import com.scal.muxon.db.repository.ContentStorageRepository;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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
        ContentLibraryEntity library = contentLibraryRepository
                .findById(libraryId)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
        return artifactRootForStorage(library.getContentStorageId());
    }

    public Path artifactRootForStorage(UUID contentStorageId) {
        ContentStorageEntity storage = contentStorageRepository
                .findById(contentStorageId)
                .orElseThrow(() -> new EntityNotFoundException("Content storage not found: " + contentStorageId));
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
                throw new IllegalStateException("NFS content storage " + storage.getId() + " has no mountPath");
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
