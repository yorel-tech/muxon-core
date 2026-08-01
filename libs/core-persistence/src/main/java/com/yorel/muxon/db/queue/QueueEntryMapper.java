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
package com.yorel.muxon.db.queue;

import com.yorel.muxon.api.enums.QueueCategory;
import com.yorel.muxon.api.enums.QueueStatus;
import com.yorel.muxon.api.model.EntityType;
import com.yorel.muxon.db.model.QueueEntryEntity;
import com.yorel.muxon.spi.queue.CommandMessage;
import java.time.Instant;
import java.util.Map;

/** Maps between QueueEntry (JPA) and CommandMessage (transport-agnostic DTO). */
public final class QueueEntryMapper {

  private static final String DEFAULT_VERSION = "1.0";

  private QueueEntryMapper() {}

  public static CommandMessage toCommandMessage(QueueEntryEntity entry) {
    if (entry == null) {
      return null;
    }
    return CommandMessage.builder()
        .id(entry.getId())
        .queueType(entry.getQueueType())
        .entityType(convertToModelEntityType(entry.getEntityType()))
        .entityId(entry.getEntityId())
        .payload(entry.getPayload() != null ? entry.getPayload() : Map.of())
        .metadata(entry.getMetadata() != null ? entry.getMetadata() : Map.of())
        .source(entry.getSource())
        .actorType(entry.getActorType())
        .actorUserId(entry.getActorUserId())
        .actorService(entry.getActorService())
        .createdAt(entry.getCreatedAt() != null ? entry.getCreatedAt() : Instant.now())
        .requestId(entry.getRequestId())
        .correlationId(entry.getCorrelationId())
        .build();
  }

  public static QueueEntryEntity toQueueEntry(CommandMessage msg) {
    if (msg == null) {
      return null;
    }
    QueueEntryEntity entry = new QueueEntryEntity();
    if (msg.id() != null) {
      entry.setId(msg.id());
    }
    entry.setQueueType(msg.queueType());
    entry.setEntityType(convertToEntityTypeEnum(msg.entityType()));
    entry.setEntityId(msg.entityId());
    entry.setQueueCategory(QueueCategory.COMMAND);
    entry.setStatus(QueueStatus.PENDING);
    entry.setPayload(msg.payload());
    entry.setMetadata(msg.metadata());
    entry.setSource(msg.source());
    entry.setActorType(msg.actorType());
    entry.setActorUserId(msg.actorUserId());
    entry.setActorService(msg.actorService());
    Instant now = Instant.now();
    entry.setCreatedAt(msg.createdAt() != null ? msg.createdAt() : now);
    entry.setUpdatedAt(now);
    entry.setRequestId(msg.requestId());
    entry.setCorrelationId(msg.correlationId());
    entry.setVersion(DEFAULT_VERSION);
    return entry;
  }

  private static EntityType convertToModelEntityType(
      com.yorel.muxon.api.enums.EntityType enumType) {
    return EntityType.valueOf(enumType.name());
  }

  private static com.yorel.muxon.api.enums.EntityType convertToEntityTypeEnum(
      EntityType modelType) {
    return com.yorel.muxon.api.enums.EntityType.valueOf(modelType.name());
  }
}
