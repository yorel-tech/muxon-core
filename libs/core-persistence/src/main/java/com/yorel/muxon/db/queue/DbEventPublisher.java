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
import com.yorel.muxon.db.repository.QueueEntryRepository;
import com.yorel.muxon.spi.queue.EventPublisher;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database-backed implementation of EventPublisher. Writes status/audit events as rows in the
 * orchestrator_queue table.
 */
public class DbEventPublisher implements EventPublisher {

  private static final String DEFAULT_VERSION = "1.0";

  private final QueueEntryRepository repository;

  public DbEventPublisher(QueueEntryRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void publishEvent(
      EntityType entityType, UUID entityId, String eventType, Map<String, Object> payload) {
    QueueEntryEntity entry = new QueueEntryEntity();
    entry.setQueueType(eventType);
    entry.setEntityType(com.yorel.muxon.api.enums.EntityType.valueOf(entityType.name()));
    entry.setEntityId(entityId);
    entry.setQueueCategory(QueueCategory.STATUS);
    entry.setStatus(QueueStatus.PENDING);
    entry.setPayload(payload != null ? payload : Map.of());
    entry.setActorType("SYSTEM");
    entry.setSource("orchestrator");
    entry.setCreatedAt(Instant.now());
    entry.setUpdatedAt(Instant.now());
    entry.setVersion(DEFAULT_VERSION);
    repository.save(entry);
  }

  private com.yorel.muxon.api.enums.EntityType convertToEntityTypeEnum(EntityType modelType) {
    return com.yorel.muxon.api.enums.EntityType.valueOf(modelType.name());
  }
}
