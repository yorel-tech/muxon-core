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
package com.yorel.muxon.services.statemachine;

import com.yorel.muxon.api.enums.EntityType;
import com.yorel.muxon.api.enums.JobStatus;
import com.yorel.muxon.db.model.JobEntity;
import java.util.Set;
import java.util.UUID;

public interface EntityStateMachine {

  boolean canTransitionToState(EntityType entityType, UUID entityId, Object targetState);

  boolean isOperationAllowed(EntityType entityType, UUID entityId, String operation);

  Set<String> getBlockedOperations(EntityType entityType, UUID entityId);

  void validateStateForTask(UUID entityId, JobEntity task);

  Object getCurrentState(EntityType entityType, UUID entityId);

  void updateStateForTaskStatus(UUID entityId, JobEntity task, JobStatus newStatus);
}
