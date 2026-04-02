package com.onetattva.infron.integration.content;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentLibraryContractIntegrationTest {

    @Test
    void contentLibrarySpecIncludesProviderAndTenantApis() throws IOException {
        String spec = Files.readString(Path.of("..", "openapi", "content_library.yaml"));
        assertTrue(spec.contains("/provider-content-libraries:"), "provider library endpoint should exist");
        assertTrue(spec.contains("/tenants/{tenantId}/content-libraries:"), "tenant-scoped library endpoint should exist");
        assertTrue(spec.contains("tags: [ProviderContentLibraries]"), "provider tag split should exist");
        assertTrue(spec.contains("tags: [TenantContentLibraries]"), "tenant tag split should exist");
    }

    @Test
    void vmSpecIncludesContentItemReference() throws IOException {
        String spec = Files.readString(Path.of("..", "openapi", "vms.yaml"));
        assertTrue(spec.contains("content_item_id:"), "vm schema should include content item id");
    }
}
