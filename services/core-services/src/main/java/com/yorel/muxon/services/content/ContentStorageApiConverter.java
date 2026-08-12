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

import com.yorel.muxon.api.model.ContentStorage;
import com.yorel.muxon.api.model.ContentStorageConfig;
import com.yorel.muxon.api.model.ContentStorageList;
import com.yorel.muxon.api.model.ContentStorageType;
import com.yorel.muxon.db.model.ContentStorageEntity;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ContentStorageApiConverter {

  private final ObjectMapper objectMapper;

  public ContentStorageApiConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public ContentStorage toApi(ContentStorageEntity entity) {
    ContentStorage api = new ContentStorage();
    api.setId(entity.getId());
    api.setName(entity.getName());
    api.setType(ContentStorageType.fromValue(entity.getStorageType()));
    api.setConfig(toConfig(entity.getConfig()));
    api.setIsDefault(entity.isDefaultStorage());
    if (entity.getCreatedAt() != null) {
      api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
    if (entity.getUpdatedAt() != null) {
      api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }
    return api;
  }

  public ContentStorageList toPagedList(Page<ContentStorageEntity> page) {
    List<ContentStorage> items = page.getContent().stream().map(this::toApi).toList();
    ContentStorageList list = new ContentStorageList();
    list.setTotal((int) page.getTotalElements());
    list.setPage(page.getNumber() + 1);
    list.setPerPage(page.getSize());
    list.setItems(items);
    return list;
  }

  public ContentStorageConfig toConfig(Map<String, Object> map) {
    if (map == null || map.isEmpty()) {
      return new ContentStorageConfig();
    }
    return objectMapper.convertValue(map, ContentStorageConfig.class);
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> configToMap(ContentStorageConfig cfg) {
    if (cfg == null) {
      return new HashMap<>();
    }
    return objectMapper.convertValue(cfg, Map.class);
  }
}
