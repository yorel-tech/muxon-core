package com.yorel.muxon.services.content;

import com.yorel.muxon.api.model.ContentStorage;
import com.yorel.muxon.api.model.ContentStorageCreate;
import com.yorel.muxon.api.model.ContentStorageList;
import com.yorel.muxon.api.model.ContentStorageType;
import com.yorel.muxon.api.model.ContentStorageUpdate;
import com.yorel.muxon.common.EntityNotFoundException;
import com.yorel.muxon.db.model.ContentStorageEntity;
import com.yorel.muxon.db.repository.ContentLibraryRepository;
import com.yorel.muxon.db.repository.ContentStorageRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class ContentStorageService {

    private final ContentStorageRepository contentStorageRepository;
    private final ContentLibraryRepository contentLibraryRepository;
    private final ContentStorageApiConverter converter;

    public ContentStorageService(
            ContentStorageRepository contentStorageRepository,
            ContentLibraryRepository contentLibraryRepository,
            ContentStorageApiConverter converter) {
        this.contentStorageRepository = contentStorageRepository;
        this.contentLibraryRepository = contentLibraryRepository;
        this.converter = converter;
    }

    public ContentStorageList list(Integer page, Integer perPage) {
        Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("name"));
        Page<ContentStorageEntity> p = contentStorageRepository.findAllByOrderByNameAsc(pageable);
        return converter.toPagedList(p);
    }

    public ContentStorage get(UUID id) {
        return converter.toApi(require(id));
    }

    @Transactional
    public ContentStorage create(ContentStorageCreate body) {
        validateConfig(body.getType(), body.getConfig());
        ContentStorageEntity entity = new ContentStorageEntity();
        entity.setName(body.getName().trim());
        entity.setStorageType(body.getType().getValue().toLowerCase(Locale.ROOT));
        entity.setConfig(converter.configToMap(body.getConfig()));
        boolean asDefault = Boolean.TRUE.equals(body.getIsDefault());
        entity.setDefaultStorage(asDefault);
        if (asDefault) {
            clearDefaultFlagExcept(null);
        }
        return converter.toApi(contentStorageRepository.save(entity));
    }

    @Transactional
    public ContentStorage update(UUID id, ContentStorageUpdate body) {
        ContentStorageEntity entity = require(id);
        if (body.getName() != null && !body.getName().isBlank()) {
            entity.setName(body.getName().trim());
        }
        if (body.getConfig() != null) {
            ContentStorageType type = ContentStorageType.fromValue(entity.getStorageType());
            validateConfig(type, body.getConfig());
            entity.setConfig(converter.configToMap(body.getConfig()));
        }
        if (body.getIsDefault() != null) {
            if (Boolean.TRUE.equals(body.getIsDefault())) {
                clearDefaultFlagExcept(id);
                entity.setDefaultStorage(true);
            } else if (Boolean.FALSE.equals(body.getIsDefault()) && entity.isDefaultStorage()) {
                throw new IllegalArgumentException("Clearing default is not supported; set another storage as default first");
            }
        }
        return converter.toApi(contentStorageRepository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        ContentStorageEntity entity = require(id);
        if (entity.isDefaultStorage()) {
            throw new IllegalArgumentException("Cannot delete the default content storage; designate another as default first");
        }
        if (contentLibraryRepository.countByContentStorageId(id) > 0) {
            throw new IllegalArgumentException("Content storage is still referenced by content libraries");
        }
        contentStorageRepository.delete(entity);
    }

    public ContentStorageEntity require(UUID id) {
        return contentStorageRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content storage not found: " + id));
    }

    public ContentStorageEntity requireDefault() {
        return contentStorageRepository
                .findByDefaultStorageIsTrue()
                .orElseThrow(() -> new IllegalStateException("No default content storage is configured"));
    }

    private void clearDefaultFlagExcept(UUID exceptId) {
        for (ContentStorageEntity e : contentStorageRepository.findAll()) {
            if (!e.isDefaultStorage()) {
                continue;
            }
            if (exceptId != null && e.getId().equals(exceptId)) {
                continue;
            }
            e.setDefaultStorage(false);
            contentStorageRepository.save(e);
        }
    }

    private static void validateConfig(ContentStorageType type, com.yorel.muxon.api.model.ContentStorageConfig cfg) {
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (cfg == null) {
            throw new IllegalArgumentException("config is required");
        }
        if (type == ContentStorageType.NFS) {
            if (cfg.getMountPath() == null || cfg.getMountPath().isBlank()) {
                throw new IllegalArgumentException("NFS content storage requires config.mountPath");
            }
        }
        if (type == ContentStorageType.S3) {
            if (cfg.getBucket() == null || cfg.getBucket().isBlank()) {
                throw new IllegalArgumentException("S3 content storage requires config.bucket");
            }
            if (cfg.getRegion() == null || cfg.getRegion().isBlank()) {
                throw new IllegalArgumentException("S3 content storage requires config.region");
            }
        }
    }
}
