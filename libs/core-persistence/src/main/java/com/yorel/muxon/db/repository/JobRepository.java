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

import com.yorel.muxon.api.enums.EntityType;
import com.yorel.muxon.api.enums.JobStatus;
import com.yorel.muxon.api.enums.JobType;
import com.yorel.muxon.db.model.JobEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Repository for Job entity. */
@Repository
public interface JobRepository extends JpaRepository<JobEntity, UUID> {

  /** Find jobs by target entity */
  Page<JobEntity> findByTargetEntityTypeAndTargetEntityId(
      EntityType entityType, UUID entityId, Pageable pageable);

  /** Find jobs by target entity and status */
  Page<JobEntity> findByTargetEntityTypeAndTargetEntityIdAndStatus(
      EntityType entityType, UUID entityId, JobStatus status, Pageable pageable);

  /** Find jobs by status */
  Page<JobEntity> findByStatus(JobStatus status, Pageable pageable);

  /** Find jobs by job type */
  Page<JobEntity> findByJobType(JobType jobType, Pageable pageable);

  /** Find pending jobs */
  @Query("SELECT j FROM JobEntity j WHERE j.status = 'PENDING' ORDER BY j.createdAt ASC")
  List<JobEntity> findPendingJobs();

  /** Find running jobs */
  @Query("SELECT j FROM JobEntity j WHERE j.status = 'RUNNING' ORDER BY j.startedAt ASC")
  List<JobEntity> findRunningJobs();

  /** Find jobs by correlation ID */
  List<JobEntity> findByCorrelationId(String correlationId);

  /** Find jobs by request ID */
  List<JobEntity> findByRequestId(String requestId);

  /** Find stalled jobs (running for too long) */
  @Query("SELECT j FROM JobEntity j WHERE j.status = 'RUNNING' AND j.startedAt < :staleThreshold")
  List<JobEntity> findStalledJobs(@Param("staleThreshold") Instant staleThreshold);

  /** Find jobs by provider */
  Page<JobEntity> findByProviderId(UUID providerId, Pageable pageable);

  /** Count jobs by status */
  long countByStatus(JobStatus status);

  /** Count jobs by job type */
  long countByJobType(JobType jobType);
}
