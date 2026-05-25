package com.sal.muxon.orch;

import com.sal.muxon.providers.IsoAttachment;
import com.sal.muxon.worker.support.ContentItemResolution;
import com.sal.muxon.db.model.ContentItemEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentItemResolutionTest {

    @Test
    void assertAvailableTemplate_rejectsWrongType() {
        ContentItemEntity iso = item("iso", "available", "/p");
        assertThrows(IllegalStateException.class, () -> ContentItemResolution.assertAvailableTemplate(iso));
    }

    @Test
    void assertAvailableIso_rejectsPending() {
        ContentItemEntity iso = item("iso", "pending", "/p");
        assertThrows(IllegalStateException.class, () -> ContentItemResolution.assertAvailableIso(iso));
    }

    @Test
    void assertAvailableIso_acceptsReady() {
        ContentItemEntity iso = item("iso", "available", "/images/x.iso");
        ContentItemResolution.assertAvailableIso(iso);
    }

    @Test
    void toIsoAttachment_readsMetadata() {
        ContentItemEntity iso = item("iso", "available", "/srv/a.iso");
        iso.setMetadata(Map.of("deviceName", "sda", "bootable", "true"));
        IsoAttachment a = ContentItemResolution.toIsoAttachment(iso);
        assertEquals("/srv/a.iso", a.isoPath());
        assertEquals("sda", a.deviceName());
        assertTrue(a.bootable());
    }

    @Test
    void toIsoAttachment_caseInsensitiveKeys() {
        ContentItemEntity iso = item("iso", "available", "/b.iso");
        iso.setMetadata(Map.of("DeviceName", "cdrom0", "Bootable", "1"));
        IsoAttachment a = ContentItemResolution.toIsoAttachment(iso);
        assertEquals("cdrom0", a.deviceName());
        assertTrue(a.bootable());
    }

    private static ContentItemEntity item(String type, String status, String path) {
        ContentItemEntity e = new ContentItemEntity();
        e.setId(UUID.randomUUID());
        e.setLibraryId(UUID.randomUUID());
        e.setName("n");
        e.setContentType(type);
        e.setContentStatus(status);
        e.setProviderRelativePath(path);
        return e;
    }
}
