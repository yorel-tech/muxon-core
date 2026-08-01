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
package com.yorel.muxon.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ContentLibraryProviderPathsTest {

  @Test
  void sanitizeInstanceName_replacesUnsafeCharacters() {
    assertEquals("acme-prod", ContentLibraryProviderPaths.sanitizeInstanceName("ACME Prod!!"));
    assertEquals("default", ContentLibraryProviderPaths.sanitizeInstanceName(""));
    assertEquals("default", ContentLibraryProviderPaths.sanitizeInstanceName("!!!"));
  }

  @Test
  void sanitizeFilename_stripsPathAndDots() {
    assertEquals(
        "image.iso", ContentLibraryProviderPaths.sanitizeFilename("../../../path/to/image.iso"));
    assertEquals(
        "debian-12.qcow2", ContentLibraryProviderPaths.sanitizeFilename("debian-12.qcow2"));
  }

  @Test
  void buildRelativePath_matchesConvention() {
    UUID lib = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    UUID item = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
    String path =
        ContentLibraryProviderPaths.buildRelativePath("local", 19, lib, item, "debian-12.qcow2");
    assertEquals(
        "muxon-local-19/content-libraries/550e8400-e29b-41d4-a716-446655440000/"
            + "6ba7b810-9dad-11d1-80b4-00c04fd430c8/debian-12.qcow2",
        path);
  }

  @Test
  void filenameForStorage_addsExtensionWhenMissing() {
    assertTrue(ContentLibraryProviderPaths.filenameForStorage("myiso", "iso").endsWith(".iso"));
    assertTrue(
        ContentLibraryProviderPaths.filenameForStorage("tpl", "vm_template").endsWith(".qcow2"));
    assertEquals(
        "already.img",
        ContentLibraryProviderPaths.filenameForStorage("already.img", "vm_template"));
  }

  @Test
  void instanceSegment_noSpaces() {
    String seg = ContentLibraryProviderPaths.instanceSegment("local", 19);
    assertFalse(seg.contains(" "));
    assertEquals("muxon-local-19", seg);
  }
}
