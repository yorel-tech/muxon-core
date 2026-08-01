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
package com.yorel.muxon.orch.orch;

import com.yorel.muxon.api.enums.JobStatus;
import com.yorel.muxon.db.model.JobEntity;
import com.yorel.muxon.db.repository.JobRepository;
import com.yorel.muxon.spi.queue.TaskEventMessage;
import com.yorel.muxon.spi.queue.TaskEventQueue;
import com.yorel.muxon.spi.queue.TaskEventQueue.TaskEventTypes;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls the {@link TaskEventQueue} and updates Job status accordingly.
 *
 * <p>This is the only component that writes to the {@code job} table in response to task events.
 * The worker never writes to the job table directly.
 */
@Service
public class TaskEventProcessor {

  private static final Logger log = LoggerFactory.getLogger(TaskEventProcessor.class);
  private static final int POLL_BATCH = 50;

  @Autowired private TaskEventQueue taskEventQueue;

  @Autowired private JobRepository jobRepository;

  @Scheduled(fixedDelay = 2000)
  public void processTaskEvents() {
    List<TaskEventMessage> events = taskEventQueue.pollTaskEvents(POLL_BATCH);
    if (events.isEmpty()) {
      return;
    }
    log.debug("Processing {} task event(s)", events.size());
    for (TaskEventMessage event : events) {
      try {
        handle(event);
        taskEventQueue.markProcessed(event.id());
      } catch (Exception ex) {
        log.error("Failed to process task event {}: {}", event.id(), ex.getMessage(), ex);
        taskEventQueue.markFailed(event.id(), ex.getMessage());
      }
    }
  }

  @Transactional
  protected void handle(TaskEventMessage event) {
    UUID jobId = extractJobId(event.payload());
    if (jobId == null) {
      log.warn("Task event {} has no jobId in payload — skipping job update", event.id());
      return;
    }

    JobEntity job = jobRepository.findById(jobId).orElse(null);
    if (job == null) {
      log.warn("Job {} not found for task event {} — skipping", jobId, event.id());
      return;
    }

    switch (event.eventType()) {
      case TaskEventTypes.STARTED -> {
        if (job.getStatus() == JobStatus.PENDING) {
          job.setStatus(JobStatus.RUNNING);
          job.setStartedAt(Instant.now());
          log.info("Job {} started via task event", jobId);
        }
      }
      case TaskEventTypes.COMPLETED -> {
        job.setStatus(JobStatus.COMPLETED);
        job.setCompletedAt(Instant.now());
        job.setProgressPercentage(100);
        log.info("Job {} completed via task event", jobId);
      }
      case TaskEventTypes.FAILED -> {
        job.setStatus(JobStatus.FAILED);
        job.setCompletedAt(Instant.now());
        Object errMsg = event.payload().get("errorMessage");
        if (errMsg != null) {
          job.setErrorMessage(errMsg.toString());
        }
        log.info("Job {} failed via task event: {}", jobId, errMsg);
      }
      case TaskEventTypes.HEARTBEAT -> {
        job.setLastHeartbeatAt(Instant.now());
        Object pct = event.payload().get("progressPercentage");
        if (pct instanceof Number n) {
          job.setProgressPercentage(n.intValue());
        }
      }
      default -> log.debug("Unhandled task event type '{}' for job {}", event.eventType(), jobId);
    }

    jobRepository.save(job);
  }

  private UUID extractJobId(Map<String, Object> payload) {
    Object raw = payload.get("jobId");
    if (raw == null) {
      return null;
    }
    try {
      return UUID.fromString(raw.toString());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
