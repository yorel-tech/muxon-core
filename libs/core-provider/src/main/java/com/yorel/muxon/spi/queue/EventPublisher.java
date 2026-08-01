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
package com.yorel.muxon.spi.queue;

import com.yorel.muxon.api.model.EntityType;
import java.util.Map;
import java.util.UUID;

/**
 * Transport-agnostic port for publishing status/audit events. Implementations may write to the same
 * DB queue table (OSS) or to a message bus topic provided by enterprise extensions.
 */
public interface EventPublisher {

  /**
   * Publish a status or audit event (e.g. VM_STATUS_CHANGED, VM_OPERATION_COMPLETED).
   *
   * @param entityType type of entity (e.g. VM)
   * @param entityId id of the entity
   * @param eventType event type string (e.g. "VM_STATUS_CHANGED")
   * @param payload event payload (before_status, after_status, etc.)
   */
  void publishEvent(
      EntityType entityType, UUID entityId, String eventType, Map<String, Object> payload);
}
