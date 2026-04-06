package com.onetattva.infron.core.services.content;

import com.onetattva.infron.api.model.ContentLibrary;
import com.onetattva.infron.api.model.ContentLibraryCreate;
import com.onetattva.infron.api.model.ContentLibraryList;
import com.onetattva.infron.api.model.ContentLibraryType;
import com.onetattva.infron.api.model.ContentLibraryUpdate;
import com.onetattva.infron.core.common.Constants;
import com.onetattva.infron.core.common.EntityNotFoundException;
import com.onetattva.infron.db.model.ContentLibraryEntity;
import com.onetattva.infron.db.repository.ContentLibraryRepository;
import com.onetattva.infron.db.repository.ContentStorageRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ContentLibraryService {

    public static final UUID SYSTEM_TENANT_ID = UUID.fromString(Constants.SYSTEM_ID);

    private final ContentLibraryRepository contentLibraryRepository;
    private final ContentStorageRepository contentStorageRepository;
    private final ContentLibraryApiConverter converter;
    private final ObjectMapper objectMapper;

    public ContentLibraryService(
            ContentLibraryRepository contentLibraryRepository,
            ContentStorageRepository contentStorageRepository,
            ContentLibraryApiConverter converter,
            ObjectMapper objectMapper) {
        this.contentLibraryRepository = contentLibraryRepository;
        this.contentStorageRepository = contentStorageRepository;
        this.converter = converter;
        this.objectMapper = objectMapper;
    }

    public ContentLibraryList listPlatform(Integer page, Integer perPage) {
        Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("name"));
        Page<ContentLibraryEntity> entityPage = contentLibraryRepository.findByTenantId(SYSTEM_TENANT_ID, pageable);
        return converter.toPagedList(entityPage);
    }

    public ContentLibraryList listVisibleToTenant(UUID tenantId, Integer page, Integer perPage) {
        Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("name"));
        Page<ContentLibraryEntity> entityPage =
                contentLibraryRepository.findByTenantIdIn(List.of(tenantId, SYSTEM_TENANT_ID), pageable);
        return converter.toPagedList(entityPage);
    }

    public ContentLibrary get(UUID id) {
        ContentLibraryEntity entity = contentLibraryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + id));
        return converter.toApi(entity);
    }

    public ContentLibrary getPlatform(UUID id) {
        ContentLibraryEntity entity = contentLibraryRepository.findByIdAndTenantId(id, SYSTEM_TENANT_ID)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + id));
        return converter.toApi(entity);
    }

    public void requirePlatformLibrary(UUID libraryId) {
        contentLibraryRepository.findByIdAndTenantId(libraryId, SYSTEM_TENANT_ID)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
    }

    /**
     * Tenant may read own libraries or platform (system) libraries.
     */
    public void requireReadAccess(UUID tenantId, UUID libraryId) {
        contentLibraryRepository
                .findByIdAndTenantIdIn(libraryId, List.of(tenantId, SYSTEM_TENANT_ID))
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
    }

    /**
     * Mutations only on libraries owned by the tenant.
     */
    public void requireWriteAccess(UUID tenantId, UUID libraryId) {
        contentLibraryRepository.findByIdAndTenantId(libraryId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
    }

    @Transactional
    public ContentLibrary create(ContentLibraryCreate body, UUID owningTenantId) {
        if (!SYSTEM_TENANT_ID.equals(owningTenantId)
                && body.getType() == ContentLibraryType.REMOTE) {
            throw new IllegalArgumentException("Tenant content libraries must be local");
        }
        UUID storageId;
        if (SYSTEM_TENANT_ID.equals(owningTenantId)) {
            if (body.getContentStorageId() == null) {
                throw new IllegalArgumentException("contentStorageId is required for platform content libraries");
            }
            storageId = body.getContentStorageId();
            contentStorageRepository
                    .findById(storageId)
                    .orElseThrow(() -> new EntityNotFoundException("Content storage not found: " + storageId));
        } else {
            storageId = contentStorageRepository
                    .findByDefaultStorageIsTrue()
                    .orElseThrow(() -> new IllegalStateException("No default content storage is configured"))
                    .getId();
        }
        ContentLibraryEntity entity = new ContentLibraryEntity();
        entity.setName(body.getName());
        entity.setDescription(body.getDescription());
        entity.setLibraryType(body.getType().getValue().toLowerCase(Locale.ROOT));
        entity.setAccessMode(body.getAccessMode() != null
                ? body.getAccessMode().getValue().toLowerCase(Locale.ROOT)
                : "read_write");
        entity.setTenantId(owningTenantId);
        entity.setSourceConfig(toMap(body.getSourceConfig()));
        entity.setContentStorageId(storageId);
        entity.setMetadata(body.getMetadata());
        entity.setSyncStatus("never_synced");
        return converter.toApi(contentLibraryRepository.save(entity));
    }

    @Transactional
    public ContentLibrary replace(UUID id, ContentLibraryUpdate body) {
        ContentLibraryEntity entity = contentLibraryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + id));
        entity.setName(body.getName() != null ? body.getName() : entity.getName());
        entity.setDescription(body.getDescription());
        if (body.getAccessMode() != null) {
            entity.setAccessMode(body.getAccessMode().getValue().toLowerCase(Locale.ROOT));
        }
        entity.setSourceConfig(body.getSourceConfig() != null ? toMap(body.getSourceConfig()) : entity.getSourceConfig());
        entity.setMetadata(body.getMetadata());
        return converter.toApi(contentLibraryRepository.save(entity));
    }

    @Transactional
    public ContentLibrary update(UUID id, ContentLibraryUpdate body) {
        ContentLibraryEntity entity = contentLibraryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + id));
        if (body.getName() != null) {
            entity.setName(body.getName());
        }
        if (body.getDescription() != null) {
            entity.setDescription(body.getDescription());
        }
        if (body.getAccessMode() != null) {
            entity.setAccessMode(body.getAccessMode().getValue().toLowerCase(Locale.ROOT));
        }
        if (body.getSourceConfig() != null) {
            entity.setSourceConfig(toMap(body.getSourceConfig()));
        }
        applyContentStorageIdIfPlatform(entity, body.getContentStorageId());
        if (body.getMetadata() != null) {
            entity.setMetadata(body.getMetadata());
        }
        return converter.toApi(contentLibraryRepository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        if (!contentLibraryRepository.existsById(id)) {
            throw new EntityNotFoundException("Content library not found: " + id);
        }
        contentLibraryRepository.deleteById(id);
    }

    public ContentLibraryEntity requireEntity(UUID libraryId) {
        return contentLibraryRepository.findById(libraryId)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object sourceConfig) {
        if (sourceConfig == null) {
            return null;
        }
        return objectMapper.convertValue(sourceConfig, Map.class);
    }

    private void applyContentStorageIdIfPlatform(ContentLibraryEntity entity, UUID requestedStorageId) {
        if (requestedStorageId == null) {
            return;
        }
        if (!SYSTEM_TENANT_ID.equals(entity.getTenantId())) {
            return;
        }
        contentStorageRepository
                .findById(requestedStorageId)
                .orElseThrow(() -> new EntityNotFoundException("Content storage not found: " + requestedStorageId));
        entity.setContentStorageId(requestedStorageId);
    }
}
