package com.onetattva.infron.core.services.content;

import com.onetattva.infron.api.dto.TaskCreateRequest;
import com.onetattva.infron.api.enums.EntityType;
import com.onetattva.infron.api.enums.JobType;
import com.onetattva.infron.api.model.ContentSyncResponse;
import com.onetattva.infron.core.services.task.TaskOrchestrationService;
import com.onetattva.infron.db.model.ContentLibraryEntity;
import com.onetattva.infron.db.model.JobEntity;
import com.onetattva.infron.db.repository.ContentLibraryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Remote catalog metadata sync (stub items); does not pull binaries into the content store.
 */
@Service
public class ContentLibrarySyncService {

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
        ContentLibraryEntity library = contentLibraryRepository.findById(libraryId)
                .orElseThrow(() -> new IllegalArgumentException("Content library not found: " + libraryId));
        if (!"remote".equals(library.getLibraryType().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Sync applies only to remote content libraries");
        }
        if (!ContentLibraryService.SYSTEM_TENANT_ID.equals(library.getTenantId())) {
            throw new IllegalArgumentException("Remote catalog sync is only supported for platform libraries");
        }

        TaskCreateRequest request = new TaskCreateRequest();
        request.setOperation(JobType.CONTENT_LIBRARY_SYNC);
        request.setEntityType(EntityType.PROVIDER);
        request.setEntityId(libraryId);
        request.setTimeoutSeconds(DEFAULT_TASK_TIMEOUT_SECONDS);
        request.setParameters(Map.of("libraryId", libraryId.toString()));
        request.setMetadata(Map.of("kind", "CONTENT_LIBRARY_SYNC", "libraryId", libraryId.toString()));
        JobEntity job = taskOrchestrationService.createTask(request);

        library.setSyncStatus("in_progress");
        contentLibraryRepository.save(library);

        taskOrchestrationService.startTask(job.getId());

        library.setSyncStatus("synced");
        library.setLastSyncedAt(Instant.now());
        contentLibraryRepository.save(library);

        taskOrchestrationService.completeTask(job.getId(), Map.of("libraryId", libraryId.toString(), "discoveredCount", 0));

        ContentSyncResponse response = new ContentSyncResponse();
        response.setLibraryId(libraryId);
        response.setTaskId(job.getId());
        response.setStatus(ContentSyncResponse.StatusEnum.ACCEPTED);
        response.setSyncedCount(0);
        response.setMessage("Metadata sync accepted");
        return response;
    }
}
