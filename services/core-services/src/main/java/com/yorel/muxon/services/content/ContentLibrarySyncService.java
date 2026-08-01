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
package com.yorel.muxon.services.content;

import com.yorel.muxon.api.dto.TaskCreateRequest;
import com.yorel.muxon.api.enums.EntityType;
import com.yorel.muxon.api.enums.JobType;
import com.yorel.muxon.api.model.ContentSyncResponse;
import com.yorel.muxon.db.model.ContentLibraryEntity;
import com.yorel.muxon.db.model.JobEntity;
import com.yorel.muxon.db.repository.ContentLibraryRepository;
import com.yorel.muxon.services.task.TaskOrchestrationService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Remote catalog metadata sync (stub items); does not pull binaries into the content store. */
@Service
public class ContentLibrarySyncService {

  private static final Logger log = LoggerFactory.getLogger(ContentLibrarySyncService.class);

  private static final int DEFAULT_TASK_TIMEOUT_SECONDS = 3600;

  private final ContentLibraryRepository contentLibraryRepository;
  private final TaskOrchestrationService taskOrchestrationService;

  public ContentLibrarySyncService(
      ContentLibraryRepository contentLibraryRepository,
      TaskOrchestrationService taskOrchestrationService) {
    this.contentLibraryRepository = contentLibraryRepository;
    this.taskOrchestrationService = taskOrchestrationService;
  }

  @Transactional
  public ContentSyncResponse enqueueSync(UUID libraryId) {
    ContentLibraryEntity library =
        contentLibraryRepository
            .findById(libraryId)
            .orElseThrow(
                () -> new IllegalArgumentException("Content library not found: " + libraryId));
    if (log.isDebugEnabled()) {
      log.debug(
          "Content library sync: libraryId={}, type={}, tenantId={}, contentStorageId={}, "
              + "sourceConfigKeys={}",
          libraryId,
          library.getLibraryType(),
          library.getTenantId(),
          library.getContentStorageId(),
          library.getSourceConfig() != null
              ? library.getSourceConfig().keySet()
              : java.util.Set.of());
    }
    if (!"remote".equals(library.getLibraryType().toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException("Sync applies only to remote content libraries");
    }
    if (!ContentLibraryService.SYSTEM_TENANT_ID.equals(library.getTenantId())) {
      throw new IllegalArgumentException(
          "Remote catalog sync is only supported for platform libraries");
    }

    TaskCreateRequest request = new TaskCreateRequest();
    request.setOperation(JobType.CONTENT_LIBRARY_SYNC);
    request.setEntityType(EntityType.PROVIDER);
    request.setEntityId(libraryId);
    request.setTimeoutSeconds(DEFAULT_TASK_TIMEOUT_SECONDS);
    request.setParameters(Map.of("libraryId", libraryId.toString()));
    request.setMetadata(Map.of("kind", "CONTENT_LIBRARY_SYNC", "libraryId", libraryId.toString()));
    JobEntity job = taskOrchestrationService.createTask(request);
    log.debug(
        "Content library sync: created task jobId={} for libraryId={}", job.getId(), libraryId);

    library.setSyncStatus("in_progress");
    contentLibraryRepository.save(library);

    taskOrchestrationService.startTask(job.getId());
    log.debug("Content library sync: task started jobId={}", job.getId());

    library.setSyncStatus("synced");
    library.setLastSyncedAt(Instant.now());
    contentLibraryRepository.save(library);

    taskOrchestrationService.completeTask(
        job.getId(), Map.of("libraryId", libraryId.toString(), "discoveredCount", 0));
    log.debug("Content library sync: completed jobId={}, discoveredCount=0 (stub)", job.getId());

    ContentSyncResponse response = new ContentSyncResponse();
    response.setLibraryId(libraryId);
    response.setTaskId(job.getId());
    response.setStatus(ContentSyncResponse.StatusEnum.ACCEPTED);
    response.setSyncedCount(0);
    response.setMessage("Metadata sync accepted");
    return response;
  }
}
