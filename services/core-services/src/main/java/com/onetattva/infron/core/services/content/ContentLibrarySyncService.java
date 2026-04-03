package com.onetattva.infron.core.services.content;

import com.onetattva.infron.api.dto.TaskCreateRequest;
import com.onetattva.infron.api.enums.EntityType;
import com.onetattva.infron.api.enums.JobType;
import com.onetattva.infron.api.model.ContentFetchResponse;
import com.onetattva.infron.api.model.ContentSyncResponse;
import com.onetattva.infron.core.services.task.TaskOrchestrationService;
import com.onetattva.infron.db.model.ContentItemEntity;
import com.onetattva.infron.db.model.ContentLibraryEntity;
import com.onetattva.infron.db.model.JobEntity;
import com.onetattva.infron.db.repository.ContentItemRepository;
import com.onetattva.infron.db.repository.ContentLibraryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class ContentLibrarySyncService {

    private static final int DEFAULT_TASK_TIMEOUT_SECONDS = 3600;

    private final ContentLibraryRepository contentLibraryRepository;
    private final ContentItemRepository contentItemRepository;
    private final TaskOrchestrationService taskOrchestrationService;
    private final ContentLibraryProviderPathBuilder contentLibraryProviderPathBuilder;

    public ContentLibrarySyncService(
            ContentLibraryRepository contentLibraryRepository,
            ContentItemRepository contentItemRepository,
            TaskOrchestrationService taskOrchestrationService,
            ContentLibraryProviderPathBuilder contentLibraryProviderPathBuilder) {
        this.contentLibraryRepository = contentLibraryRepository;
        this.contentItemRepository = contentItemRepository;
        this.taskOrchestrationService = taskOrchestrationService;
        this.contentLibraryProviderPathBuilder = contentLibraryProviderPathBuilder;
    }

    /**
     * Runs library catalog sync in-process (content libraries are not provider-queue work).
     */
    @Transactional
    public ContentSyncResponse enqueueSync(UUID libraryId) {
        ContentLibraryEntity library = contentLibraryRepository.findById(libraryId)
                .orElseThrow(() -> new IllegalArgumentException("Content library not found: " + libraryId));

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
        response.setMessage("Sync accepted");
        return response;
    }

    /**
     * Runs single-item fetch in-process (content libraries are not provider-queue work).
     */
    @Transactional
    public ContentFetchResponse enqueueFetch(UUID libraryId, UUID itemId) {
        contentLibraryRepository.findById(libraryId)
                .orElseThrow(() -> new IllegalArgumentException("Content library not found: " + libraryId));
        ContentItemEntity item = contentItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Content item not found: " + itemId));

        TaskCreateRequest request = new TaskCreateRequest();
        request.setOperation(JobType.CONTENT_ITEM_FETCH);
        request.setEntityType(EntityType.PROVIDER);
        request.setEntityId(libraryId);
        request.setTimeoutSeconds(DEFAULT_TASK_TIMEOUT_SECONDS);
        request.setParameters(Map.of("libraryId", libraryId.toString(), "itemId", itemId.toString()));
        request.setMetadata(Map.of("kind", "CONTENT_ITEM_FETCH", "libraryId", libraryId.toString(), "itemId", itemId.toString()));
        JobEntity job = taskOrchestrationService.createTask(request);

        item.setFetchStatus("fetching");
        contentItemRepository.save(item);

        taskOrchestrationService.startTask(job.getId());

        item.setFetchStatus("available");
        item.setLastFetchedAt(Instant.now());
        contentLibraryProviderPathBuilder.applyProviderPaths(item);
        contentItemRepository.save(item);

        ContentLibraryEntity library = contentLibraryRepository.findById(item.getLibraryId()).orElse(null);
        if (library != null) {
            library.setSyncStatus("synced");
            library.setLastSyncedAt(Instant.now());
            contentLibraryRepository.save(library);
        }

        taskOrchestrationService.completeTask(job.getId(), Map.of("libraryId", libraryId.toString(), "itemId", itemId.toString(), "discoveredCount", 1));

        ContentFetchResponse response = new ContentFetchResponse();
        response.setItemId(itemId);
        response.setTaskId(job.getId());
        response.setStatus(ContentFetchResponse.StatusEnum.ACCEPTED);
        response.setMessage("Fetch accepted");
        return response;
    }
}
