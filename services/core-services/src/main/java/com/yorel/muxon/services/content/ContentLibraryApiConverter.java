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
package com.yorel.muxon.services.content;

import com.yorel.muxon.api.model.ContentLibrary;
import com.yorel.muxon.api.model.ContentLibraryAccessMode;
import com.yorel.muxon.api.model.ContentLibraryList;
import com.yorel.muxon.api.model.ContentLibraryType;
import com.yorel.muxon.api.model.ContentSourceConfig;
import com.yorel.muxon.api.model.ContentSyncStatus;
import com.yorel.muxon.db.model.ContentLibraryEntity;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ContentLibraryApiConverter {

  private final ObjectMapper objectMapper;

  public ContentLibraryApiConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ContentLibrary toApi(ContentLibraryEntity entity) {
    ContentLibrary api = new ContentLibrary();
    api.setId(entity.getId());
    api.setName(entity.getName());
    api.setDescription(entity.getDescription());
    api.setType(ContentLibraryType.fromValue(entity.getLibraryType()));
    api.setAccessMode(ContentLibraryAccessMode.fromValue(entity.getAccessMode()));
    api.setTenantId(entity.getTenantId());
    if (entity.getSourceConfig() != null) {
      api.setSourceConfig(
          objectMapper.convertValue(entity.getSourceConfig(), ContentSourceConfig.class));
    }
    api.setContentStorageId(entity.getContentStorageId());
    api.setSyncStatus(ContentSyncStatus.fromValue(effectiveSyncStatus(entity)));
    api.setMetadata(entity.getMetadata());
    if (entity.getCreatedAt() != null) {
      api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
    if (entity.getUpdatedAt() != null) {
      api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }
    if (entity.getLastSyncedAt() != null) {
      api.setLastSyncedAt(entity.getLastSyncedAt().atOffset(ZoneOffset.UTC));
    }
    return api;
  }

  public ContentLibraryList toPagedList(Page<ContentLibraryEntity> page) {
    List<ContentLibrary> items = page.getContent().stream().map(this::toApi).toList();
    ContentLibraryList list = new ContentLibraryList();
    list.setTotal((int) page.getTotalElements());
    list.setPage(page.getNumber() + 1);
    list.setPerPage(page.getSize());
    list.setItems(items);
    return list;
  }

  public static String normalizeLowerEnum(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return value.toLowerCase(Locale.ROOT);
  }

  /**
   * Local libraries are backed by tenant/platform storage directly; they are not synced from an
   * external catalog, so {@code never_synced} is misleading for API consumers.
   */
  private static String effectiveSyncStatus(ContentLibraryEntity entity) {
    String raw = entity.getSyncStatus();
    if (entity.getLibraryType() != null
        && "local".equals(entity.getLibraryType().toLowerCase(Locale.ROOT))
        && raw != null
        && "never_synced".equalsIgnoreCase(raw)) {
      return "synced";
    }
    return raw;
  }
}
