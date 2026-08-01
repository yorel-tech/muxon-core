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

import com.yorel.muxon.db.model.TaskLogEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskLogRepository extends JpaRepository<TaskLogEntity, UUID> {

  List<TaskLogEntity> findByTaskIdOrderByCreatedAtDesc(UUID taskId);

  Page<TaskLogEntity> findByTaskIdOrderByCreatedAtDesc(UUID taskId, Pageable pageable);

  List<TaskLogEntity> findByStepIdOrderByCreatedAtDesc(UUID stepId);

  List<TaskLogEntity> findByTaskIdAndLogLevel(UUID taskId, String logLevel);

  @Query(
      "SELECT l FROM TaskLogEntity l WHERE l.taskId = :taskId AND l.createdAt >= :since ORDER BY l.createdAt DESC")
  List<TaskLogEntity> findRecentLogs(@Param("taskId") UUID taskId, @Param("since") Instant since);

  @Query("SELECT COUNT(l) FROM TaskLogEntity l WHERE l.taskId = :taskId")
  long countByTaskId(@Param("taskId") UUID taskId);

  void deleteByTaskId(UUID taskId);
}
