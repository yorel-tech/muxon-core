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

class ContentLibraryVmOpenApiContractTest {

  @Test
  void vmCreateIncludesIsoContentItemIds() throws IOException {
    String spec = Files.readString(Path.of("..", "openapi", "vms.yaml"));
    assertTrue(
        spec.contains("iso_content_item_ids:"), "VmCreateRequest should list ISO content items");
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
