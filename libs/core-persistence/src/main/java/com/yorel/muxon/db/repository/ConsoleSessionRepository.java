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

import com.yorel.muxon.db.model.ConsoleSessionEntity;
import com.yorel.muxon.db.model.ConsoleSessionStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsoleSessionRepository extends JpaRepository<ConsoleSessionEntity, UUID> {

  Optional<ConsoleSessionEntity> findByToken(String token);

  Optional<ConsoleSessionEntity> findByVmIdAndUserId(UUID vmId, UUID userId);

  /** Active sessions whose expiry is still in the future (used for per-user console cap). */
  long countByUserIdAndStatusAndExpiresAtAfter(
      UUID userId, ConsoleSessionStatus status, Instant expiresAt);

  @Modifying
  @Query("DELETE FROM ConsoleSessionEntity c WHERE c.vmId = :vmId")
  int deleteByVmId(@Param("vmId") UUID vmId);

  /**
   * Removes expired rows (and any stale row past {@code now}) so the table does not grow unbounded.
   */
  @Modifying
  @Query("DELETE FROM ConsoleSessionEntity c WHERE c.expiresAt < :now")
  int deleteByExpiresAtBefore(@Param("now") Instant now);
}
