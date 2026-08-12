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
package com.yorel.muxon.worker.support;

import com.yorel.muxon.db.model.ContentItemEntity;
import com.yorel.muxon.providers.IsoAttachment;
import java.util.Locale;
import java.util.Map;

public final class ContentItemResolution {

  private ContentItemResolution() {}

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
