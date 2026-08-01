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

class ContentLibraryContractIntegrationTest {

  @Test
  void contentLibrarySpecIncludesProviderAndTenantApis() throws IOException {
    String spec = Files.readString(Path.of("..", "openapi", "content_library.yaml"));
    assertTrue(
        spec.contains("/platform/content-libraries:"), "platform library endpoint should exist");
    assertTrue(
        spec.contains("/tenants/{tenantId}/content-libraries:"),
        "tenant-scoped library endpoint should exist");
    assertTrue(spec.contains("tags: [PlatformContentLibraries]"), "platform tag should exist");
    assertTrue(spec.contains("tags: [TenantContentLibraries]"), "tenant tag split should exist");
    assertTrue(
        spec.contains("contentStorageId:"), "library schema should reference content storage");
  }

  @Test
  void contentStorageSpecHasCrudPaths() throws IOException {
    String spec = Files.readString(Path.of("..", "openapi", "content_storage.yaml"));
    assertTrue(
        spec.contains("/platform/content-storages:"),
        "content storages list/create path should exist");
    assertTrue(
        spec.contains("/platform/content-storages/{contentStorageId}:"),
        "content storage by id path should exist");
    assertTrue(spec.contains("tags: [ContentStorages]"), "content storages tag should exist");
  }

  @Test
  void vmSpecIncludesContentItemReference() throws IOException {
    String spec = Files.readString(Path.of("..", "openapi", "vms.yaml"));
    assertTrue(spec.contains("content_item_id:"), "vm schema should include content item id");
  }
}
