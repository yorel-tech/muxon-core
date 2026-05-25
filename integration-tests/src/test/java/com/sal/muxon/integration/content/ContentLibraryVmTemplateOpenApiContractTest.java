package com.sal.muxon.integration.content;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentLibraryVmTemplateOpenApiContractTest {

    @Test
    void contentItemsAreTypedUsingOneOfAndDiscriminator() throws IOException {
        String spec = Files.readString(Path.of("..", "openapi", "content_library.yaml"));
        assertTrue(spec.contains("ContentItem:"), "ContentItem schema should exist");
        assertTrue(spec.contains("oneOf:"), "ContentItem should use oneOf");
        assertTrue(spec.contains("discriminator:"), "ContentItem should declare discriminator");
        assertTrue(spec.contains("propertyName: contentType"), "discriminator should use contentType");
    }

    @Test
    void vmTemplateContentItemRequiresTemplateSpec() throws IOException {
        String spec = Files.readString(Path.of("..", "openapi", "content_library.yaml"));
        assertTrue(spec.contains("VmTemplateContentItem:"), "VmTemplateContentItem schema should exist");
        assertTrue(spec.contains("required: [templateSpec]"), "VmTemplateContentItem should require templateSpec");
        assertTrue(spec.contains("VmTemplateSpec:"), "VmTemplateSpec schema should exist");
    }
}

