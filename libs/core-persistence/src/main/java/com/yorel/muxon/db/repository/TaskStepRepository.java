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
package com.yorel.muxon.db.repository;

import com.yorel.muxon.api.enums.JobStatus;
import com.yorel.muxon.db.model.TaskStepEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskStepRepository extends JpaRepository<TaskStepEntity, UUID> {

  List<TaskStepEntity> findByTaskIdOrderByStepNumberAsc(UUID taskId);

  Optional<TaskStepEntity> findByTaskIdAndStepNumber(UUID taskId, Integer stepNumber);

  List<TaskStepEntity> findByTaskIdAndStatus(UUID taskId, JobStatus status);

  @Query("SELECT s FROM TaskStepEntity s WHERE s.taskId = :taskId AND s.status = 'RUNNING'")
  Optional<TaskStepEntity> findCurrentRunningStep(@Param("taskId") UUID taskId);

  @Query(
      "SELECT COUNT(s) FROM TaskStepEntity s WHERE s.taskId = :taskId AND s.status = 'COMPLETED'")
  long countCompletedSteps(@Param("taskId") UUID taskId);

  @Query("SELECT COUNT(s) FROM TaskStepEntity s WHERE s.taskId = :taskId")
  long countTotalSteps(@Param("taskId") UUID taskId);

  void deleteByTaskId(UUID taskId);
}
