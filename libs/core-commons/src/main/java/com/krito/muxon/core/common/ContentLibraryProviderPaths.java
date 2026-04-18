package com.krito.muxon.core.common;

import java.util.Locale;
import java.util.UUID;

/**
 * Relative paths for content artifacts on Proxmox/KVM storage pools:
 * {@code muxon-{instance}-{id}/content-libraries/{libraryId}/{itemId}/{filename}}.
 */
public final class ContentLibraryProviderPaths {

    private static final int MAX_INSTANCE_NAME_LEN = 48;
    private static final int MAX_FILENAME_LEN = 255;

    private ContentLibraryProviderPaths() {
    }

    /**
     * Allow only alphanumerics, underscore, hyphen; collapse repeats; trim.
     */
    public static String sanitizeInstanceName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "default";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "-");
        s = s.replaceAll("-{2,}", "-").replaceAll("^_+|_+$", "").replaceAll("^-+|-+$", "");
        if (s.isEmpty()) {
            return "default";
        }
        return s.length() > MAX_INSTANCE_NAME_LEN ? s.substring(0, MAX_INSTANCE_NAME_LEN) : s;
    }

    /**
     * Single path segment for a file name: no slashes, no "..", no control chars.
     */
    public static String sanitizeFilename(String raw) {
        if (raw == null || raw.isBlank()) {
            return "artifact";
        }
        String s = raw.trim().replaceAll("[\u0000-\u001f\u007f]", "");
        s = s.replace("\\", "/");
        int slash = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'));
        if (slash >= 0 && slash < s.length() - 1) {
            s = s.substring(slash + 1);
        }
        while (s.startsWith(".")) {
            s = s.substring(1);
        }
        s = s.replaceAll("[^a-zA-Z0-9._-]+", "_");
        if (s.isEmpty() || ".".equals(s) || "..".equals(s)) {
            return "artifact";
        }
        return s.length() > MAX_FILENAME_LEN ? s.substring(0, MAX_FILENAME_LEN) : s;
    }

    public static String instanceSegment(String sanitizedInstanceName, int instanceId) {
        return "muxon-" + sanitizedInstanceName + "-" + instanceId;
    }

    public static String relativePath(
            String instanceSegment,
            UUID libraryId,
            UUID itemId,
            String safeFilename) {
        return instanceSegment
                + "/content-libraries/"
                + libraryId
                + "/"
                + itemId
                + "/"
                + safeFilename;
    }

    /**
     * Full relative path under the storage pool root from raw config and item identity.
     */
    public static String buildRelativePath(
            String rawInstanceName,
            int instanceId,
            UUID libraryId,
            UUID itemId,
            String rawFilename) {
        String seg = instanceSegment(sanitizeInstanceName(rawInstanceName), instanceId);
        return relativePath(seg, libraryId, itemId, sanitizeFilename(rawFilename));
    }

    /**
     * Storage file name from catalog item name; adds a default extension when none present.
     */
    public static String filenameForStorage(String itemName, String contentType) {
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        String base = sanitizeFilename(itemName);
        if (base.equals("artifact") && itemName != null && !itemName.isBlank()) {
            base = sanitizeFilename(itemName.replace(' ', '_'));
        }
        if (base.contains(".")) {
            return base;
        }
        String suffix = defaultSuffix(ct);
        return suffix.isEmpty() ? base : base + suffix;
    }

    private static String defaultSuffix(String contentType) {
        return switch (contentType) {
            case "iso" -> ".iso";
            case "vm_template" -> ".qcow2";
            case "script" -> ".sh";
            case "container_image" -> ".tar";
            case "helm_chart" -> ".tgz";
            default -> "";
        };
    }
}
