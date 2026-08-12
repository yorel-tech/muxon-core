/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.integration.content;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

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
    assertTrue(
        spec.contains("VmTemplateContentItem:"), "VmTemplateContentItem schema should exist");
    assertTrue(
        spec.contains("required: [templateSpec]"),
        "VmTemplateContentItem should require templateSpec");
    assertTrue(spec.contains("VmTemplateSpec:"), "VmTemplateSpec schema should exist");
  }
}
