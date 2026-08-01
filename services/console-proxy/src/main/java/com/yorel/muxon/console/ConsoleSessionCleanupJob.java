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
package com.yorel.muxon.console;

import com.yorel.muxon.db.repository.ConsoleSessionRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConsoleSessionCleanupJob {

  private static final Logger log = LoggerFactory.getLogger(ConsoleSessionCleanupJob.class);

  private final ConsoleSessionRepository consoleSessionRepository;

  public ConsoleSessionCleanupJob(ConsoleSessionRepository consoleSessionRepository) {
    this.consoleSessionRepository = consoleSessionRepository;
  }

  /**
   * Removes rows past {@code expires_at} so tokens cannot be revived and the table stays bounded.
   * New console opens upsert the single (vm_id, user_id) row with a fresh token.
   */
  @Scheduled(fixedDelayString = "${muxon.console.cleanup-interval-ms:60000}")
  @Transactional
  public void removeExpiredSessions() {
    Instant now = Instant.now();
    int deleted = consoleSessionRepository.deleteByExpiresAtBefore(now);
    if (deleted > 0) {
      log.info("Deleted {} expired console session row(s)", deleted);
    }
  }
}
