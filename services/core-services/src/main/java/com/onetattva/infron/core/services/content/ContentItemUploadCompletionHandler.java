package com.onetattva.infron.core.services.content;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.onetattva.infron.api.model.VmTemplateDiskSpec;
import com.onetattva.infron.api.model.VmTemplateSpec;
import com.onetattva.infron.core.common.EntityNotFoundException;
import com.onetattva.infron.db.model.ContentItemEntity;
import com.onetattva.infron.db.model.ContentLibraryEntity;
import com.onetattva.infron.db.model.ContentStorageEntity;
import com.onetattva.infron.db.repository.ContentItemRepository;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class ContentItemUploadCompletionHandler {

    private final ObjectMapper objectMapper;
    private final ContentItemRepository contentItemRepository;
    private final ContentStoragePathResolver contentStoragePathResolver;
    private final ContentStorageS3Uploader contentStorageS3Uploader;

    public ContentItemUploadCompletionHandler(
            ObjectMapper objectMapper,
            ContentItemRepository contentItemRepository,
            ContentStoragePathResolver contentStoragePathResolver,
            ContentStorageS3Uploader contentStorageS3Uploader) {
        this.objectMapper = objectMapper;
        this.contentItemRepository = contentItemRepository;
        this.contentStoragePathResolver = contentStoragePathResolver;
        this.contentStorageS3Uploader = contentStorageS3Uploader;
    }

    /**
     * Finalize a single uploaded file for a vm_template content item.
     *
     * The upload API is item-scoped; each completed upload session is treated as one disk file, assigned
     * in the order uploads are completed: disk-0, disk-1, ...
     */
    public void finalizeVmTemplateDiskUpload(
            ContentLibraryEntity library,
            ContentStorageEntity storage,
            ContentItemEntity item,
            Path uploadedFile,
            long sizeBytes,
            String sha256HexLowercase) {

        if (!"vm_template".equalsIgnoreCase(item.getContentType())) {
            throw new IllegalArgumentException("Not a vm_template item");
        }
        if (item.getTemplateSpec() == null) {
            throw new IllegalArgumentException("vm_template item is missing template_spec in database");
        }

        VmTemplateSpec spec = objectMapper.convertValue(item.getTemplateSpec(), VmTemplateSpec.class);
        List<VmTemplateDiskSpec> disks = spec.getSpec() != null ? spec.getSpec().getDisks() : null;
        if (disks == null || disks.isEmpty()) {
            throw new IllegalArgumentException("templateSpec.spec.disks must have at least 1 disk");
        }

        String baseRelative = item.getProviderRelativePath();
        if (baseRelative == null || baseRelative.isBlank()) {
            throw new IllegalStateException("providerRelativePath is required to place uploaded artifacts");
        }
        String baseDirRelative = Path.of(baseRelative).getParent().toString().replace('\\', '/');
        if (baseDirRelative.isBlank()) {
            throw new IllegalStateException("Invalid providerRelativePath for vm_template");
        }

        int diskIndex = nextDiskIndex(storage, library.getId(), baseDirRelative, item.getId());
        if (diskIndex >= disks.size()) {
            throw new IllegalArgumentException(
                    "Too many disk uploads for template: expected " + disks.size() + " disks");
        }

        String diskRelPath = baseDirRelative + "/disks/disk-" + diskIndex + ".qcow2";

        if (contentStorageS3Uploader.isS3(storage)) {
            // Upload disk object
            contentStorageS3Uploader.uploadObject(storage, uploadedFile, diskRelPath);
            // Best-effort cleanup of local temp file is done by caller.
        } else {
            Path root = contentStoragePathResolver.artifactRootForLibrary(library.getId()).toAbsolutePath().normalize();
            Path diskDest = root.resolve(diskRelPath).normalize();
            if (!diskDest.startsWith(root)) {
                throw new IllegalStateException("Refusing to write outside artifact root");
            }
            try {
                Files.createDirectories(diskDest.getParent());
                Files.move(uploadedFile, diskDest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to move uploaded disk into content store", e);
            }
        }

        // Enrich templateSpec entry for this disk
        VmTemplateDiskSpec diskSpec = disks.get(diskIndex);
        diskSpec.setPath("disks/disk-" + diskIndex + ".qcow2");
        diskSpec.setChecksum("sha256:" + sha256HexLowercase);
        // Keep user-provided sizeBytes if present; otherwise set from upload
        if (diskSpec.getSizeBytes() == null || diskSpec.getSizeBytes() < 1) {
            diskSpec.setSizeBytes(sizeBytes);
        }

        // Persist updated templateSpec (with path/checksum enrichment)
        Map<String, Object> updated = objectMapper.convertValue(spec, new TypeReference<Map<String, Object>>() {});
        item.setTemplateSpec(updated);

        // Update aggregate size/checksum fields (best-effort; checksum refers to last uploaded file for now)
        item.setSizeBytes((item.getSizeBytes() == null ? 0L : item.getSizeBytes()) + sizeBytes);
        item.setChecksum(sha256HexLowercase);
        item.setChecksumAlgorithm("sha256");

        // If all disks uploaded, generate template.json and mark available
        if (isSpecComplete(spec) && allDiskFilesPresent(storage, library.getId(), baseDirRelative, disks)) {
            writeTemplateJsonAndChecksums(storage, library.getId(), baseDirRelative, spec);
            item.setContentStatus("available");
            item.setLastReplicatedAt(Instant.now());
        } else {
            item.setContentStatus("pending");
        }

        contentItemRepository.save(item);
    }

    private int nextDiskIndex(ContentStorageEntity storage, java.util.UUID libraryId, String baseDirRelative, java.util.UUID itemId) {
        if (contentStorageS3Uploader.isS3(storage)) {
            // For S3 we don't have a cheap list in current uploader; rely on count stored in DB via templateSpec.path.
            ContentItemEntity refreshed = contentItemRepository.findById(itemId)
                    .orElseThrow(() -> new EntityNotFoundException("Content item not found: " + itemId));
            VmTemplateSpec spec = objectMapper.convertValue(refreshed.getTemplateSpec(), VmTemplateSpec.class);
            long count = Optional.ofNullable(spec.getSpec())
                    .map(s -> s.getDisks())
                    .stream()
                    .flatMap(List::stream)
                    .filter(d -> d.getPath() != null && d.getPath().startsWith("disks/"))
                    .count();
            return (int) count;
        }
        Path root = contentStoragePathResolver.artifactRootForLibrary(libraryId).toAbsolutePath().normalize();
        Path disksDir = root.resolve(baseDirRelative).resolve("disks").normalize();
        if (!disksDir.startsWith(root)) {
            throw new IllegalStateException("Refusing to list outside artifact root");
        }
        if (!Files.exists(disksDir)) {
            return 0;
        }
        try (Stream<Path> s = Files.list(disksDir)) {
            return (int) s
                    .filter(p -> p.getFileName().toString().startsWith("disk-") && p.getFileName().toString().endsWith(".qcow2"))
                    .count();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to scan disks directory", e);
        }
    }

    private static boolean isSpecComplete(VmTemplateSpec spec) {
        if (spec == null || spec.getSpec() == null || spec.getSpec().getDisks() == null) return false;
        for (VmTemplateDiskSpec d : spec.getSpec().getDisks()) {
            if (d == null) return false;
            if (d.getPath() == null || d.getPath().isBlank()) return false;
            if (d.getChecksum() == null || d.getChecksum().isBlank()) return false;
        }
        return true;
    }

    private boolean allDiskFilesPresent(
            ContentStorageEntity storage,
            java.util.UUID libraryId,
            String baseDirRelative,
            List<VmTemplateDiskSpec> disks) {
        if (contentStorageS3Uploader.isS3(storage)) {
            // Initial phase: we don't list S3 objects here. Assume uploads succeeded if spec is complete.
            return true;
        }
        Path root = contentStoragePathResolver.artifactRootForLibrary(libraryId).toAbsolutePath().normalize();
        for (VmTemplateDiskSpec d : disks) {
            if (d.getPath() == null || d.getPath().isBlank()) return false;
            Path p = root.resolve(baseDirRelative).resolve(d.getPath()).normalize();
            if (!p.startsWith(root)) return false;
            if (!Files.exists(p)) return false;
        }
        return true;
    }

    private void writeTemplateJsonAndChecksums(ContentStorageEntity storage, java.util.UUID libraryId, String baseDirRelative, VmTemplateSpec spec) {
        String templateJsonRel = baseDirRelative + "/template.json";
        String checksumsRel = baseDirRelative + "/disks/checksums.sha256";

        if (contentStorageS3Uploader.isS3(storage)) {
            // Not implemented in initial phase (would require multi-object writes).
            return;
        }

        Path root = contentStoragePathResolver.artifactRootForLibrary(libraryId).toAbsolutePath().normalize();
        Path templateJson = root.resolve(templateJsonRel).normalize();
        Path checksums = root.resolve(checksumsRel).normalize();
        if (!templateJson.startsWith(root) || !checksums.startsWith(root)) {
            throw new IllegalStateException("Refusing to write outside artifact root");
        }

        try {
            Files.createDirectories(templateJson.getParent());
            Files.createDirectories(checksums.getParent());
            Files.writeString(templateJson, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec));

            // Rebuild checksums file from disk specs that have checksums populated.
            List<VmTemplateDiskSpec> disks = Objects.requireNonNull(spec.getSpec()).getDisks();
            StringBuilder sb = new StringBuilder();
            for (VmTemplateDiskSpec d : disks) {
                if (d.getChecksum() == null) continue;
                String c = d.getChecksum().toLowerCase(Locale.ROOT).trim();
                if (c.startsWith("sha256:")) c = c.substring("sha256:".length());
                if (!c.matches("[a-f0-9]{64}")) continue;
                String rel = d.getPath() == null ? "" : d.getPath();
                if (rel.isBlank()) continue;
                sb.append(c).append("  ").append(rel).append("\n");
            }
            Files.writeString(checksums, sb.toString());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write template.json/checksums", e);
        }
    }
}

