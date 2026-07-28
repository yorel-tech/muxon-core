package com.yorel.muxon.customization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Resolves content library script items by reading their files from the provider storage path.
 *
 * <p>The worker passes a resolved path root (the content storage base directory) and the
 * provider-relative path stored in {@code content_items.provider_relative_path}.
 */
public class ScriptFetcher {

    private final Path storageRoot;

    /**
     * @param storageRoot Absolute path to the content storage root on the worker host.
     */
    public ScriptFetcher(Path storageRoot) {
        this.storageRoot = storageRoot;
    }

    /**
     * Fetch the content of each script item identified by its provider-relative path.
     *
     * @param relativePathsByItemId Map of content-item-id → provider_relative_path (resolved
     *                              by the worker from the DB before calling this method).
     * @return Ordered list of script contents (one entry per item, in input order).
     */
    public List<String> fetchScripts(List<ScriptRef> refs) throws IOException {
        List<String> results = new ArrayList<>(refs.size());
        for (ScriptRef ref : refs) {
            Path scriptPath = storageRoot.resolve(ref.providerRelativePath());
            if (!Files.exists(scriptPath)) {
                throw new IOException(
                        "Script content item not found at path: " + scriptPath
                                + " (itemId=" + ref.itemId() + ")");
            }
            String content = Files.readString(scriptPath, StandardCharsets.UTF_8);
            // Validate checksum if provided
            if (ref.expectedChecksum() != null && !ref.expectedChecksum().isBlank()) {
                validateChecksum(content, ref.expectedChecksum(), ref.itemId());
            }
            results.add(content);
        }
        return results;
    }

    private void validateChecksum(String content, String expectedChecksum, UUID itemId) throws IOException {
        if (!expectedChecksum.startsWith("sha256:")) return;
        String expected = expectedChecksum.substring("sha256:".length());
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            if (!hex.toString().equalsIgnoreCase(expected)) {
                throw new IOException("Checksum mismatch for script item " + itemId
                        + ": expected " + expected + " got " + hex);
            }
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 unavailable", e);
        }
    }

    /** Lightweight DTO for a resolved script reference. */
    public record ScriptRef(UUID itemId, String providerRelativePath, String expectedChecksum) {}
}
