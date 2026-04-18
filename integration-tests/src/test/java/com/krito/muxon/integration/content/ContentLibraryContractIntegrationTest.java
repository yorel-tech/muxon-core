package com.krito.muxon.integration.content;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentLibraryContractIntegrationTest {

    @Test
    void contentLibrarySpecIncludesProviderAndTenantApis() throws IOException {
        String spec = Files.readString(Path.of("..", "openapi", "content_library.yaml"));
        assertTrue(spec.contains("/platform/content-libraries:"), "platform library endpoint should exist");
        assertTrue(spec.contains("/tenants/{tenantId}/content-libraries:"), "tenant-scoped library endpoint should exist");
        assertTrue(spec.contains("tags: [PlatformContentLibraries]"), "platform tag should exist");
        assertTrue(spec.contains("tags: [TenantContentLibraries]"), "tenant tag split should exist");
        assertTrue(spec.contains("contentStorageId:"), "library schema should reference content storage");
    }

    @Test
    void contentStorageSpecHasCrudPaths() throws IOException {
        String spec = Files.readString(Path.of("..", "openapi", "content_storage.yaml"));
        assertTrue(spec.contains("/platform/content-storages:"), "content storages list/create path should exist");
        assertTrue(spec.contains("/platform/content-storages/{contentStorageId}:"), "content storage by id path should exist");
        assertTrue(spec.contains("tags: [ContentStorages]"), "content storages tag should exist");
    }

    @Test
    void vmSpecIncludesContentItemReference() throws IOException {
        String spec = Files.readString(Path.of("..", "openapi", "vms.yaml"));
        assertTrue(spec.contains("content_item_id:"), "vm schema should include content item id");
    }
}
