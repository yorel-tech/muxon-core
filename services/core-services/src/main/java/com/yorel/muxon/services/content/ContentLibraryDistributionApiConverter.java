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

import com.yorel.muxon.api.model.ContentLibraryDistribution;
import com.yorel.muxon.api.model.ContentLibraryDistributionList;
import com.yorel.muxon.api.model.EntityReference;
import com.yorel.muxon.db.model.ContentLibraryDistributionEntity;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ContentLibraryDistributionApiConverter {

  public ContentLibraryDistribution toApi(ContentLibraryDistributionEntity entity) {
    return toApi(entity, null);
  }

  public ContentLibraryDistribution toApi(
      ContentLibraryDistributionEntity entity, EntityReference datacenterRef) {
    ContentLibraryDistribution api = new ContentLibraryDistribution();
    api.setId(entity.getId());
    api.setLibraryId(entity.getLibraryId());
    api.setDatacenterId(entity.getDatacenterId());
    api.setStorageClassName(entity.getStorageClassName());
    if (datacenterRef != null) {
      api.setDatacenter(datacenterRef);
    } else if (entity.getDatacenterId() != null) {
      EntityReference ref = new EntityReference();
      ref.setId(entity.getDatacenterId());
      api.setDatacenter(ref);
    }
    api.setReplicateStatus(
        ContentLibraryDistribution.ReplicateStatusEnum.fromValue(entity.getReplicateStatus()));
    api.setProgressPercent(entity.getProgressPercent());
    api.setErrorMessage(entity.getErrorMessage());
    if (entity.getLastReplicatedAt() != null) {
      api.setLastReplicatedAt(entity.getLastReplicatedAt().atOffset(ZoneOffset.UTC));
    }
    if (entity.getCreatedAt() != null) {
      api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
    if (entity.getUpdatedAt() != null) {
      api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }
    return api;
  }

  public ContentLibraryDistributionList toList(List<ContentLibraryDistributionEntity> entities) {
    ContentLibraryDistributionList list = new ContentLibraryDistributionList();
    list.setItems(entities.stream().map(this::toApi).toList());
    return list;
  }

  public ContentLibraryDistributionList toList(
      List<ContentLibraryDistributionEntity> entities,
      java.util.Map<UUID, String> datacenterNamesById) {
    ContentLibraryDistributionList list = new ContentLibraryDistributionList();
    list.setItems(
        entities.stream()
            .map(
                e -> {
                  String name =
                      datacenterNamesById != null
                          ? datacenterNamesById.get(e.getDatacenterId())
                          : null;
                  EntityReference ref = new EntityReference();
                  ref.setId(e.getDatacenterId());
                  if (name != null && !name.isBlank()) {
                    ref.setName(name);
                  }
                  return toApi(e, ref);
                })
            .toList());
    return list;
  }
}
