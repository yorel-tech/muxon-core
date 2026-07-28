package com.scal.muxon.services.content;

import com.scal.muxon.api.model.ContentItemDownloadLink;
import com.scal.muxon.api.model.ContentItemUploadInitiate;
import com.scal.muxon.api.model.ContentItemUploadSession;
import com.scal.muxon.common.EntityNotFoundException;
import com.scal.muxon.db.model.ContentItemEntity;
import com.scal.muxon.db.model.ContentLibraryEntity;
import com.scal.muxon.db.model.ContentStorageEntity;
import com.scal.muxon.db.repository.ContentItemRepository;
import com.scal.muxon.db.repository.ContentLibraryRepository;
import com.scal.muxon.db.repository.ContentStorageRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.net.URI;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ContentItemUploadService {

    private static final long MAX_UPLOAD_BYTES = 50L * 1024 * 1024 * 1024;
    private static final int SESSION_TTL_HOURS = 24;
    private static final Pattern CONTENT_RANGE =
            Pattern.compile("^bytes\\s+(\\d+)-(\\d+)/(\\d+)\\s*$", Pattern.CASE_INSENSITIVE);

    private final ContentLibraryService contentLibraryService;
    private final ContentItemService contentItemService;
    private final ContentItemRepository contentItemRepository;
    private final ContentLibraryProviderPathBuilder pathBuilder;
    private final ContentStoragePathResolver contentStoragePathResolver;
    private final ContentLibraryRepository contentLibraryRepository;
    private final ContentStorageRepository contentStorageRepository;
    private final ContentStorageS3Uploader contentStorageS3Uploader;
    private final ContentItemUploadCompletionHandler completionHandler;
    private final ConcurrentHashMap<UUID, UploadSession> sessions = new ConcurrentHashMap<>();

    public ContentItemUploadService(
            ContentLibraryService contentLibraryService,
            ContentItemService contentItemService,
            ContentItemRepository contentItemRepository,
            ContentLibraryProviderPathBuilder pathBuilder,
            ContentStoragePathResolver contentStoragePathResolver,
            ContentLibraryRepository contentLibraryRepository,
            ContentStorageRepository contentStorageRepository,
            ContentStorageS3Uploader contentStorageS3Uploader,
            ContentItemUploadCompletionHandler completionHandler) {
        this.contentLibraryService = contentLibraryService;
        this.contentItemService = contentItemService;
        this.contentItemRepository = contentItemRepository;
        this.pathBuilder = pathBuilder;
        this.contentStoragePathResolver = contentStoragePathResolver;
        this.contentLibraryRepository = contentLibraryRepository;
        this.contentStorageRepository = contentStorageRepository;
        this.contentStorageS3Uploader = contentStorageS3Uploader;
        this.completionHandler = completionHandler;
    }

    /**
     * Returns a file URI to the stored artifact (client may use as download hint).
     */
    public ContentItemDownloadLink buildDownloadLink(UUID libraryId, UUID itemId) {
        ContentItemEntity item = contentItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Content item not found: " + itemId));
        if (!item.getLibraryId().equals(libraryId)) {
            throw new EntityNotFoundException("Content item not found: " + itemId);
        }
        if (!"available".equalsIgnoreCase(item.getContentStatus())) {
            throw new IllegalArgumentException("Content is not available for download");
        }
        String relative = item.getProviderRelativePath();
        if (relative == null || relative.isBlank()) {
            pathBuilder.applyProviderPaths(item);
            relative = item.getProviderRelativePath();
        }
        if (relative == null || relative.isBlank()) {
            throw new IllegalStateException("Could not resolve storage path for content item");
        }
        ContentLibraryEntity library = contentLibraryRepository
                .findById(libraryId)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
        ContentStorageEntity storage = contentStorageRepository
                .findById(library.getContentStorageId())
                .orElseThrow(() -> new EntityNotFoundException("Content storage not found for library: " + libraryId));

        if (contentStorageS3Uploader.isS3(storage)) {
            URI uri = contentStorageS3Uploader.objectUri(storage, relative);
            ContentItemDownloadLink link = new ContentItemDownloadLink();
            link.setUrl(uri);
            return link;
        }

        Path root = contentStoragePathResolver.artifactRootForLibrary(libraryId).toAbsolutePath().normalize();
        Path dest = root.resolve(relative).normalize();
        if (!dest.startsWith(root)) {
            throw new IllegalStateException("Refusing to expose path outside artifact root");
        }
        URI uri = dest.toUri();
        ContentItemDownloadLink link = new ContentItemDownloadLink();
        link.setUrl(uri);
        return link;
    }

    public ContentItemUploadSession initiatePlatform(UUID libraryId, UUID itemId, ContentItemUploadInitiate body) {
        contentLibraryService.requirePlatformLibrary(libraryId);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return initiate(libraryId, itemId, null, body);
    }

    public ContentItemUploadSession initiateTenant(UUID tenantId, UUID libraryId, UUID itemId, ContentItemUploadInitiate body) {
        contentLibraryService.requireWriteAccess(tenantId, libraryId);
        contentItemService.assertItemInLibrary(libraryId, itemId);
        return initiate(libraryId, itemId, tenantId, body);
    }

    private ContentItemUploadSession initiate(
            UUID libraryId, UUID itemId, UUID tenantId, ContentItemUploadInitiate body) {
        validateInitiate(body);
        evictIfExpired();
        UUID uploadId = UUID.randomUUID();
        Path tempFile;
        try {
            tempFile = Files.createTempFile("infron-upload-" + uploadId + "-", ".part");
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create temp file for upload", e);
        }
        long chunkSize = body.getChunkSizeHint() != null && body.getChunkSizeHint() > 0
                ? body.getChunkSizeHint()
                : 64L * 1024 * 1024;
        Instant expiresAt = Instant.now().plus(SESSION_TTL_HOURS, ChronoUnit.HOURS);
        UploadSession session = new UploadSession(
                uploadId, libraryId, itemId, tenantId, body.getTotalSize(), body.getExpectedChecksum(),
                body.getChecksumAlgorithm().trim().toLowerCase(), chunkSize, tempFile, expiresAt);
        sessions.put(uploadId, session);
        return toDto(session);
    }

    private void validateInitiate(ContentItemUploadInitiate body) {
        if (body.getTotalSize() == null || body.getTotalSize() < 1) {
            throw new IllegalArgumentException("totalSize must be positive");
        }
        if (body.getTotalSize() > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("totalSize exceeds maximum allowed upload size");
        }
        if (body.getChecksumAlgorithm() == null || body.getChecksumAlgorithm().isBlank()) {
            throw new IllegalArgumentException("checksumAlgorithm is required");
        }
        if (!"sha256".equalsIgnoreCase(body.getChecksumAlgorithm().trim())) {
            throw new IllegalArgumentException("Only sha256 checksumAlgorithm is supported");
        }
        if (body.getExpectedChecksum() == null || body.getExpectedChecksum().isBlank()) {
            throw new IllegalArgumentException("expectedChecksum is required");
        }
        String hex = body.getExpectedChecksum().trim().toLowerCase();
        if (!hex.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("expectedChecksum must be 64 hex characters (sha256)");
        }
    }

    public ContentItemUploadSession getPlatform(UUID libraryId, UUID itemId, UUID uploadId) {
        return getSession(libraryId, itemId, null, uploadId);
    }

    public ContentItemUploadSession getTenant(UUID tenantId, UUID libraryId, UUID itemId, UUID uploadId) {
        return getSession(libraryId, itemId, tenantId, uploadId);
    }

    private ContentItemUploadSession getSession(UUID libraryId, UUID itemId, UUID tenantId, UUID uploadId) {
        evictIfExpired();
        UploadSession s = sessions.get(uploadId);
        if (s == null) {
            throw new EntityNotFoundException("Upload session not found: " + uploadId);
        }
        assertSessionPath(s, libraryId, itemId, tenantId);
        return toDto(s);
    }

    /**
     * @return HTTP status 200 (chunk stored) or 308 (resume — client should re-read session)
     */
    public int putChunkPlatform(
            UUID libraryId, UUID itemId, UUID uploadId, String contentRange, Resource body) {
        return putChunk(libraryId, itemId, null, uploadId, contentRange, body);
    }

    public int putChunkTenant(
            UUID tenantId, UUID libraryId, UUID itemId, UUID uploadId, String contentRange, Resource body) {
        return putChunk(libraryId, itemId, tenantId, uploadId, contentRange, body);
    }

    private int putChunk(
            UUID libraryId, UUID itemId, UUID tenantId, UUID uploadId, String contentRange, Resource body) {
        evictIfExpired();
        UploadSession s = sessions.get(uploadId);
        if (s == null) {
            throw new EntityNotFoundException("Upload session not found: " + uploadId);
        }
        assertSessionPath(s, libraryId, itemId, tenantId);
        if (contentRange == null || contentRange.isBlank()) {
            throw new IllegalArgumentException("Content-Range header is required");
        }
        Matcher m = CONTENT_RANGE.matcher(contentRange.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid Content-Range header");
        }
        long start = Long.parseLong(m.group(1));
        long end = Long.parseLong(m.group(2));
        long total = Long.parseLong(m.group(3));
        if (total != s.totalSize) {
            throw new IllegalArgumentException("Content-Range total does not match session totalSize");
        }
        if (start > end || end >= s.totalSize) {
            throw new IllegalArgumentException("Invalid Content-Range byte interval");
        }
        long chunkLen = end - start + 1;

        synchronized (s) {
            if (start < s.uploadedBytes) {
                if (end < s.uploadedBytes) {
                    return 200;
                }
                throw new IllegalArgumentException("Content-Range overlaps already uploaded data; resume from "
                        + s.uploadedBytes);
            }
            if (start > s.uploadedBytes) {
                return 308;
            }
            try (InputStream in = body.getInputStream();
                    RandomAccessFile raf = new RandomAccessFile(s.tempFile.toFile(), "rw")) {
                raf.seek(start);
                byte[] buf = new byte[8192];
                long remaining = chunkLen;
                while (remaining > 0) {
                    int n = in.read(buf, 0, (int) Math.min(buf.length, remaining));
                    if (n < 0) {
                        throw new IllegalArgumentException("Unexpected end of chunk body");
                    }
                    raf.write(buf, 0, n);
                    remaining -= n;
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to store upload chunk", e);
            }
            s.uploadedBytes = end + 1;
            return 200;
        }
    }

    @Transactional
    public void completePlatform(UUID libraryId, UUID itemId, UUID uploadId) {
        complete(libraryId, itemId, null, uploadId);
    }

    @Transactional
    public void completeTenant(UUID tenantId, UUID libraryId, UUID itemId, UUID uploadId) {
        complete(libraryId, itemId, tenantId, uploadId);
    }

    private void complete(UUID libraryId, UUID itemId, UUID tenantId, UUID uploadId) {
        evictIfExpired();
        UploadSession s = sessions.get(uploadId);
        if (s == null) {
            throw new EntityNotFoundException("Upload session not found: " + uploadId);
        }
        assertSessionPath(s, libraryId, itemId, tenantId);
        synchronized (s) {
            if (s.uploadedBytes != s.totalSize) {
                throw new IllegalArgumentException(
                        "Upload incomplete: received " + s.uploadedBytes + " of " + s.totalSize + " bytes");
            }
        }

        String actualHex;
        try {
            actualHex = sha256Hex(s.tempFile);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to verify checksum", e);
        }
        if (!actualHex.equalsIgnoreCase(s.expectedChecksum)) {
            abandonSession(s);
            throw new IllegalArgumentException("Checksum mismatch after upload");
        }

        ContentItemEntity item = contentItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Content item not found: " + itemId));
        if (!item.getLibraryId().equals(libraryId)) {
            throw new IllegalArgumentException("Item does not belong to library");
        }

        pathBuilder.applyProviderPaths(item);
        String relative = item.getProviderRelativePath();
        if (relative == null || relative.isBlank()) {
            throw new IllegalStateException("Could not resolve provider path for item");
        }

        ContentLibraryEntity library = contentLibraryRepository
                .findById(libraryId)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
        ContentStorageEntity storage = contentStorageRepository
                .findById(library.getContentStorageId())
                .orElseThrow(() -> new EntityNotFoundException("Content storage not found for library: " + libraryId));

        if ("vm_template".equalsIgnoreCase(item.getContentType())) {
            try {
                completionHandler.finalizeVmTemplateDiskUpload(
                        library,
                        storage,
                        item,
                        s.tempFile,
                        s.totalSize,
                        actualHex.toLowerCase(Locale.ROOT));
                sessions.remove(uploadId);
            } finally {
                cleanupTemp(s.tempFile);
            }
            return;
        }

        if (contentStorageS3Uploader.isS3(storage)) {
            try {
                contentStorageS3Uploader.uploadObject(storage, s.tempFile, relative);
                item.setSizeBytes(s.totalSize);
                item.setChecksum(s.expectedChecksum.toLowerCase(Locale.ROOT));
                item.setChecksumAlgorithm("sha256");
                item.setContentStatus("available");
                item.setLastReplicatedAt(Instant.now());
                contentItemRepository.save(item);
                sessions.remove(uploadId);
            } catch (Exception e) {
                if (e instanceof RuntimeException re) {
                    throw re;
                }
                throw new IllegalStateException("Failed to finalize content item upload to S3", e);
            } finally {
                cleanupTemp(s.tempFile);
            }
            return;
        }

        Path root = contentStoragePathResolver.artifactRootForLibrary(libraryId).toAbsolutePath().normalize();
        Path dest = root.resolve(relative).normalize();
        if (!dest.startsWith(root)) {
            throw new IllegalStateException("Refusing to write outside artifact root");
        }
        try {
            Files.createDirectories(dest.getParent());
            moveUploadToDest(s.tempFile, dest);
            item.setSizeBytes(s.totalSize);
            item.setChecksum(s.expectedChecksum.toLowerCase(Locale.ROOT));
            item.setChecksumAlgorithm("sha256");
            item.setContentStatus("available");
            item.setLastReplicatedAt(Instant.now());
            contentItemRepository.save(item);
            sessions.remove(uploadId);
        } catch (Exception e) {
            if (Files.exists(dest)) {
                try {
                    moveUploadToDest(dest, s.tempFile);
                } catch (Exception restoreEx) {
                    e.addSuppressed(restoreEx);
                }
            }
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("Failed to finalize content item upload", e);
        }
    }

    private static void moveUploadToDest(Path from, Path to) throws Exception {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void abandonSession(UploadSession s) {
        sessions.remove(s.id);
        cleanupTemp(s.tempFile);
    }

    private static void assertSessionPath(UploadSession s, UUID libraryId, UUID itemId, UUID tenantId) {
        if (!s.libraryId.equals(libraryId) || !s.itemId.equals(itemId)) {
            throw new EntityNotFoundException("Upload session not found: " + s.id);
        }
        if (!Objects.equals(s.tenantId, tenantId)) {
            throw new EntityNotFoundException("Upload session not found: " + s.id);
        }
    }

    private static ContentItemUploadSession toDto(UploadSession s) {
        ContentItemUploadSession dto = new ContentItemUploadSession(
                s.id,
                "active",
                s.totalSize,
                s.uploadedBytes,
                s.checksumAlgorithm,
                s.expectedChecksum);
        dto.setItemId(s.itemId);
        dto.setChunkSize(s.chunkSize);
        dto.setExpiresAt(OffsetDateTime.ofInstant(s.expiresAt, ZoneOffset.UTC));
        return dto;
    }

    private void evictIfExpired() {
        Instant now = Instant.now();
        for (UUID id : sessions.keySet()) {
            UploadSession s = sessions.get(id);
            if (s != null && s.expiresAt.isBefore(now)) {
                UploadSession removed = sessions.remove(id);
                if (removed != null) {
                    cleanupTemp(removed.tempFile);
                }
            }
        }
    }

    private static void cleanupTemp(Path tempFile) {
        try {
            Files.deleteIfExists(tempFile);
        } catch (Exception ignored) {
            // best effort
        }
    }

    private static String sha256Hex(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file);
                DigestInputStream din = new DigestInputStream(in, md)) {
            byte[] buf = new byte[8192];
            while (din.read(buf) >= 0) {
                // digest only
            }
        }
        return HexFormat.of().formatHex(md.digest());
    }

    private static final class UploadSession {
        final UUID id;
        final UUID libraryId;
        final UUID itemId;
        final UUID tenantId;
        final long totalSize;
        final String expectedChecksum;
        final String checksumAlgorithm;
        final long chunkSize;
        final Path tempFile;
        final Instant expiresAt;
        long uploadedBytes;

        UploadSession(
                UUID id,
                UUID libraryId,
                UUID itemId,
                UUID tenantId,
                long totalSize,
                String expectedChecksum,
                String checksumAlgorithm,
                long chunkSize,
                Path tempFile,
                Instant expiresAt) {
            this.id = id;
            this.libraryId = libraryId;
            this.itemId = itemId;
            this.tenantId = tenantId;
            this.totalSize = totalSize;
            this.expectedChecksum = expectedChecksum.trim().toLowerCase();
            this.checksumAlgorithm = checksumAlgorithm;
            this.chunkSize = chunkSize;
            this.tempFile = tempFile;
            this.expiresAt = expiresAt;
            this.uploadedBytes = 0;
        }
    }
}
