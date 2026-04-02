package com.onetattva.infron.core.services.content;

import com.onetattva.infron.api.model.ContentLibrary;
import com.onetattva.infron.api.model.ContentLibraryCreate;
import com.onetattva.infron.api.model.ContentLibraryList;
import com.onetattva.infron.api.model.ContentLibraryUpdate;
import com.onetattva.infron.core.common.EntityNotFoundException;
import com.onetattva.infron.db.model.ContentLibraryEntity;
import com.onetattva.infron.db.repository.ContentLibraryRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ContentLibraryService {

    private final ContentLibraryRepository contentLibraryRepository;
    private final ContentLibraryApiConverter converter;
    private final ObjectMapper objectMapper;

    public ContentLibraryService(
            ContentLibraryRepository contentLibraryRepository,
            ContentLibraryApiConverter converter,
            ObjectMapper objectMapper) {
        this.contentLibraryRepository = contentLibraryRepository;
        this.converter = converter;
        this.objectMapper = objectMapper;
    }

    public ContentLibraryList list(Integer page, Integer perPage) {
        Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("name"));
        Page<ContentLibraryEntity> entityPage = contentLibraryRepository.findAll(pageable);
        return converter.toPagedList(entityPage);
    }

    public ContentLibraryList listByScope(String scope, Integer page, Integer perPage) {
        Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("name"));
        Page<ContentLibraryEntity> entityPage = contentLibraryRepository.findByScope(scope.toLowerCase(Locale.ROOT), pageable);
        return converter.toPagedList(entityPage);
    }

    public ContentLibraryList listVisibleToTenant(UUID tenantId, Integer page, Integer perPage) {
        Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("name"));
        Page<ContentLibraryEntity> entityPage = contentLibraryRepository.findByTenantIdOrScope(
                tenantId, "provider", pageable);
        return converter.toPagedList(entityPage);
    }

    public ContentLibrary get(UUID id) {
        ContentLibraryEntity entity = contentLibraryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + id));
        return converter.toApi(entity);
    }

    public ContentLibrary getByScope(UUID id, String scope) {
        ContentLibraryEntity entity = contentLibraryRepository.findByIdAndScope(id, scope.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + id));
        return converter.toApi(entity);
    }

    public void requireTenantLibrary(UUID tenantId, UUID libraryId) {
        contentLibraryRepository.findByIdAndTenantId(libraryId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
    }

    @Transactional
    public ContentLibrary create(ContentLibraryCreate body, String scope, UUID tenantId) {
        ContentLibraryEntity entity = new ContentLibraryEntity();
        entity.setName(body.getName());
        entity.setDescription(body.getDescription());
        entity.setScope(scope.toLowerCase(Locale.ROOT));
        entity.setLibraryType(body.getType().getValue().toLowerCase(Locale.ROOT));
        entity.setAccessMode(body.getAccessMode() != null
                ? body.getAccessMode().getValue().toLowerCase(Locale.ROOT)
                : "read_write");
        entity.setTenantId(tenantId);
        entity.setSourceConfig(toMap(body.getSourceConfig()));
        entity.setStorageClassName(body.getStorageClassName());
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
        entity.setSourceConfig(toMap(body.getSourceConfig()));
        entity.setStorageClassName(body.getStorageClassName());
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
        if (body.getStorageClassName() != null) {
            entity.setStorageClassName(body.getStorageClassName());
        }
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object sourceConfig) {
        if (sourceConfig == null) {
            return null;
        }
        return objectMapper.convertValue(sourceConfig, Map.class);
    }
}
