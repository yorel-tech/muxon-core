package com.scal.muxon.worker.support;

import com.scal.muxon.providers.IsoAttachment;
import com.scal.muxon.db.model.ContentItemEntity;

import java.util.Locale;
import java.util.Map;

public final class ContentItemResolution {

    private ContentItemResolution() {
    }

    public static void assertAvailableTemplate(ContentItemEntity item) {
        if (!"vm_template".equalsIgnoreCase(item.getContentType())) {
            throw new IllegalStateException("Content item is not a vm_template: " + item.getId());
        }
        assertAvailable(item);
    }

    public static void assertAvailableIso(ContentItemEntity item) {
        if (!"iso".equalsIgnoreCase(item.getContentType())) {
            throw new IllegalStateException("Content item is not an iso: " + item.getId());
        }
        assertAvailable(item);
    }

    public static void assertAvailableScript(ContentItemEntity item) {
        if (!"script".equalsIgnoreCase(item.getContentType())) {
            throw new IllegalStateException("Content item is not a script: " + item.getId());
        }
        assertAvailable(item);
    }

    private static void assertAvailable(ContentItemEntity item) {
        if (!"available".equalsIgnoreCase(item.getContentStatus())) {
            throw new IllegalStateException(
                    "Content item not available: " + item.getId() + " status=" + item.getContentStatus());
        }
        if (item.getProviderRelativePath() == null || item.getProviderRelativePath().isBlank()) {
            throw new IllegalStateException("Content item has no provider path: " + item.getId());
        }
    }

    public static IsoAttachment toIsoAttachment(ContentItemEntity item) {
        Map<String, String> md = item.getMetadata();
        String device = meta(md, "deviceName");
        boolean bootable = parseBool(meta(md, "bootable"));
        return new IsoAttachment(item.getProviderRelativePath(), device, bootable, md);
    }

    private static String meta(Map<String, String> md, String key) {
        if (md == null || md.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, String> e : md.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    private static boolean parseBool(String v) {
        if (v == null) {
            return false;
        }
        return "true".equalsIgnoreCase(v.trim()) || "1".equals(v.trim());
    }

    static String normalizeType(String contentType) {
        return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    }
}

