package com.onetattva.infron.core.services.content;

import com.onetattva.infron.api.model.ContentItem;
import com.onetattva.infron.api.model.ContentItemCreate;
import com.onetattva.infron.api.model.ContentItemCreateBase;
import com.onetattva.infron.api.model.ContentItemList;
import com.onetattva.infron.api.model.ContentItemUpdate;
import com.onetattva.infron.api.model.VmTemplateContentItemCreate;
import com.onetattva.infron.core.common.EntityNotFoundException;
import com.onetattva.infron.db.model.ContentItemEntity;
import com.onetattva.infron.db.repository.ContentItemRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class ContentItemService {

    private final ContentItemRepository contentItemRepository;
    private final ContentItemApiConverter converter;
    private final VmTemplateValidator vmTemplateValidator;
    private final ContentItemArtifactDeletionService contentItemArtifactDeletionService;

    public ContentItemService(
            ContentItemRepository contentItemRepository,
            ContentItemApiConverter converter,
            VmTemplateValidator vmTemplateValidator,
            ContentItemArtifactDeletionService contentItemArtifactDeletionService) {
        this.contentItemRepository = contentItemRepository;
        this.converter = converter;
        this.vmTemplateValidator = vmTemplateValidator;
        this.contentItemArtifactDeletionService = contentItemArtifactDeletionService;
    }

    public ContentItemList listByLibrary(UUID libraryId, Integer page, Integer perPage) {
        Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("name"));
        Page<ContentItemEntity> entityPage = contentItemRepository.findByLibraryId(libraryId, pageable);
        return converter.toPagedList(entityPage);
    }

    public ContentItemList list(UUID libraryId, String contentType, Integer page, Integer perPage) {
        Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("name"));
        Page<ContentItemEntity> entityPage;
        if (libraryId != null && contentType != null && !contentType.isBlank()) {
            entityPage = contentItemRepository.findByLibraryIdAndContentType(
                    libraryId, contentType.toLowerCase(Locale.ROOT), pageable);
        } else if (libraryId != null) {
            entityPage = contentItemRepository.findByLibraryId(libraryId, pageable);
        } else if (contentType != null && !contentType.isBlank()) {
            entityPage = contentItemRepository.findByContentType(contentType.toLowerCase(Locale.ROOT), pageable);
        } else {
            entityPage = contentItemRepository.findAll(pageable);
        }
        return converter.toPagedList(entityPage);
    }

    public void assertItemInLibrary(UUID libraryId, UUID itemId) {
        ContentItemEntity entity = contentItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Content item not found: " + itemId));
        if (!libraryId.equals(entity.getLibraryId())) {
            throw new EntityNotFoundException("Content item not found: " + itemId);
        }
    }

    public ContentItem get(UUID id) {
        ContentItemEntity entity = contentItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content item not found: " + id));
        return converter.toApi(entity);
    }

    @Transactional
    public ContentItem create(UUID libraryId, ContentItemCreate body) {
        if (body.getContentType() == null || body.getContentType().isBlank()) {
            throw new IllegalArgumentException("contentType is required");
        }
        if (!(body instanceof ContentItemCreateBase base)) {
            throw new IllegalArgumentException("Invalid content item create payload");
        }
        ContentItemEntity entity = new ContentItemEntity();
        entity.setLibraryId(libraryId);
        entity.setName(base.getName());
        entity.setDescription(base.getDescription());
        entity.setContentType(body.getContentType().toLowerCase(Locale.ROOT));
        entity.setVersionLabel(base.getVersion());
        entity.setChecksum(base.getChecksum());
        entity.setChecksumAlgorithm(base.getChecksumAlgorithm());
        entity.setSourceUrl(base.getSourceUrl());
        entity.setSourceItemId(base.getSourceItemId());
        entity.setMetadata(base.getMetadata());
        entity.setContentStatus("pending");

        if ("vm_template".equalsIgnoreCase(entity.getContentType())) {
            if (!(body instanceof VmTemplateContentItemCreate vmBody)) {
                throw new IllegalArgumentException("vm_template item must include templateSpec");
            }
            vmTemplateValidator.validate(vmBody.getTemplateSpec());
            // Persist as JSONB (Map) so we can query efficiently and regenerate template.json later.
            entity.setTemplateSpec(converter.toTemplateSpecMap(vmBody.getTemplateSpec()));
        }

        return converter.toApi(contentItemRepository.save(entity));
    }

    @Transactional
    public ContentItem replace(UUID id, ContentItemUpdate body) {
        ContentItemEntity entity = contentItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content item not found: " + id));
        entity.setName(body.getName() != null ? body.getName() : entity.getName());
        entity.setDescription(body.getDescription());
        entity.setVersionLabel(body.getVersion());
        entity.setChecksum(body.getChecksum());
        entity.setChecksumAlgorithm(body.getChecksumAlgorithm());
        entity.setSourceUrl(body.getSourceUrl());
        entity.setMetadata(body.getMetadata());
        return converter.toApi(contentItemRepository.save(entity));
    }

    @Transactional
    public ContentItem update(UUID id, ContentItemUpdate body) {
        ContentItemEntity entity = contentItemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content item not found: " + id));
        if (body.getName() != null) {
            entity.setName(body.getName());
        }
        if (body.getDescription() != null) {
            entity.setDescription(body.getDescription());
        }
        if (body.getVersion() != null) {
            entity.setVersionLabel(body.getVersion());
        }
        if (body.getChecksum() != null) {
            entity.setChecksum(body.getChecksum());
        }
        if (body.getChecksumAlgorithm() != null) {
            entity.setChecksumAlgorithm(body.getChecksumAlgorithm());
        }
        if (body.getSourceUrl() != null) {
            entity.setSourceUrl(body.getSourceUrl());
        }
        if (body.getMetadata() != null) {
            entity.setMetadata(body.getMetadata());
        }
        return converter.toApi(contentItemRepository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        ContentItemEntity entity = contentItemRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content item not found: " + id));
        contentItemArtifactDeletionService.deleteStoredArtifacts(entity);
        contentItemRepository.deleteById(id);
    }

    @Transactional
    public void deleteInLibrary(UUID libraryId, UUID itemId) {
        ContentItemEntity entity = contentItemRepository
                .findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("Content item not found: " + itemId));
        if (!libraryId.equals(entity.getLibraryId())) {
            throw new EntityNotFoundException("Content item not found: " + itemId);
        }
        contentItemArtifactDeletionService.deleteStoredArtifacts(entity);
        contentItemRepository.deleteById(itemId);
    }
}
