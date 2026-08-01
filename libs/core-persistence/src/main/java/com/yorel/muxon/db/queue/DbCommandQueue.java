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

import com.yorel.muxon.api.enums.QueueStatus;
import com.yorel.muxon.api.model.EntityType;
import com.yorel.muxon.db.model.QueueEntryEntity;
import com.yorel.muxon.db.repository.QueueEntryRepository;
import com.yorel.muxon.spi.queue.CommandMessage;
import com.yorel.muxon.spi.queue.CommandQueue;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database-backed implementation of CommandQueue. Uses the orchestrator_queue table and centralizes
 * PENDING → PROCESSING → COMPLETED/FAILED transitions.
 *
 * <p>{@link #pollCommands}, {@link #markCompleted}, and {@link #markFailed} use {@link
 * Propagation#REQUIRES_NEW} so the claim and terminal updates commit in isolated transactions. That
 * avoids leaving claimed queue rows managed in the caller's persistence context (which would flush
 * stale {@code PROCESSING} on outer commit and can roll back unrelated work such as {@code
 * provider_storage} inserts).
 */
public class DbCommandQueue implements CommandQueue {

  private final QueueEntryRepository repository;

  public DbCommandQueue(QueueEntryRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public UUID sendCommand(CommandMessage command) {
    QueueEntryEntity entry = QueueEntryMapper.toQueueEntry(command);
    QueueEntryEntity saved = repository.save(entry);
    return saved.getId();
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<CommandMessage> pollCommands(EntityType entityType, int limit) {
    List<QueueEntryEntity> entries =
        repository.findPendingByEntityTypeForUpdate(
            com.yorel.muxon.api.enums.EntityType.valueOf(entityType.name()),
            QueueStatus.PENDING,
            PageRequest.of(0, limit));
    Instant now = Instant.now();
    for (QueueEntryEntity entry : entries) {
      entry.setStatus(QueueStatus.PROCESSING);
      entry.setProcessedAt(now);
      entry.setUpdatedAt(now);
    }
    if (!entries.isEmpty()) {
      repository.saveAll(entries);
    }
    return entries.stream().map(QueueEntryMapper::toCommandMessage).toList();
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markCompleted(UUID commandId) {
    repository.markCompleted(commandId, Instant.now());
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void completeWithPayload(UUID commandId, Map<String, Object> payload) {
    QueueEntryEntity e =
        repository
            .findById(commandId)
            .orElseThrow(() -> new IllegalStateException("Queue entry not found: " + commandId));
    Instant now = Instant.now();
    e.setPayload(payload);
    e.setStatus(QueueStatus.COMPLETED);
    e.setProcessedAt(now);
    e.setUpdatedAt(now);
    repository.save(e);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void markFailed(UUID commandId, String errorMessage) {
    repository.markFailed(commandId, errorMessage, Instant.now());
  }

  @Override
  @Transactional(readOnly = true)
  public int getStalledCount(int staleThresholdMinutes) {
    Instant cutoff = Instant.now().minusSeconds(staleThresholdMinutes * 60L);
    return repository.findStaleProcessingEntries(cutoff).size();
  }

  @Override
  @Transactional
  public int resetStalledEntries(int staleThresholdMinutes) {
    Instant cutoff = Instant.now().minusSeconds(staleThresholdMinutes * 60L);
    List<QueueEntryEntity> stalled = repository.findStaleProcessingEntries(cutoff);
    Instant now = Instant.now();
    for (QueueEntryEntity entry : stalled) {
      entry.setStatus(QueueStatus.PENDING);
      entry.setProcessedAt(null);
      entry.setErrorMessage(null);
      entry.setUpdatedAt(now);
    }
    if (!stalled.isEmpty()) {
      repository.saveAll(stalled);
    }
    return stalled.size();
  }

  private com.yorel.muxon.api.enums.EntityType convertToEntityTypeEnum(EntityType modelType) {
    return com.yorel.muxon.api.enums.EntityType.valueOf(modelType.name());
  }
}
