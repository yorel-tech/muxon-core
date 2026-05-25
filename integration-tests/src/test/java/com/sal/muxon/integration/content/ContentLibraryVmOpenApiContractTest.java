package com.sal.muxon.integration.content;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentLibraryVmOpenApiContractTest {

    @Test
    void vmCreateIncludesIsoContentItemIds() throws IOException {
        String spec = Files.readString(Path.of("..", "openapi", "vms.yaml"));
        assertTrue(spec.contains("iso_content_item_ids:"), "VmCreateRequest should list ISO content items");
        assertTrue(spec.contains("attached_iso_item_ids:"), "Vm schema should expose attached ISO ids");
    }

    @Test
    void vmPathsIncludeIsoAttachDetachAndPublish() throws IOException {
        String paths = Files.readString(Path.of("..", "openapi", "vms_paths.yaml"));
        assertTrue(paths.contains("attach-iso"), "attach-iso path should exist");
        assertTrue(paths.contains("detach-iso"), "detach-iso path should exist");
        assertTrue(paths.contains("publish-as-template"), "publish-as-template path should exist");
    }

    @Test
    void vmSchemasIncludePublishAndIsoRequests() throws IOException {
        String spec = Files.readString(Path.of("..", "openapi", "vms.yaml"));
        assertTrue(spec.contains("VmIsoAttachRequest:"));
        assertTrue(spec.contains("VmIsoDetachRequest:"));
        assertTrue(spec.contains("VmPublishTemplateRequest:"));
        assertTrue(spec.contains("VmPublishTemplateResponse:"));
    }
}
