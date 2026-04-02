package com.onetattva.infron.core.services.content;

import com.onetattva.infron.api.model.ContentLibrary;
import com.onetattva.infron.api.model.ContentLibraryList;
import com.onetattva.infron.api.model.ContentLibraryType;
import com.onetattva.infron.api.model.ContentSyncStatus;
import com.onetattva.infron.api.model.ContentLibraryAccessMode;
import com.onetattva.infron.api.model.ContentSourceConfig;
import com.onetattva.infron.db.model.ContentLibraryEntity;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

@Component
public class ContentLibraryApiConverter {

    private final ObjectMapper objectMapper;

    public ContentLibraryApiConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ContentLibrary toApi(ContentLibraryEntity entity) {
        ContentLibrary api = new ContentLibrary();
        api.setId(entity.getId());
        api.setName(entity.getName());
        api.setDescription(entity.getDescription());
        api.setType(ContentLibraryType.fromValue(entity.getLibraryType()));
        api.setAccessMode(ContentLibraryAccessMode.fromValue(entity.getAccessMode()));
        api.setTenantId(entity.getTenantId());
        if (entity.getSourceConfig() != null) {
            api.setSourceConfig(objectMapper.convertValue(entity.getSourceConfig(), ContentSourceConfig.class));
        }
        api.setStorageClassName(entity.getStorageClassName());
        api.setSyncStatus(ContentSyncStatus.fromValue(entity.getSyncStatus()));
        api.setMetadata(entity.getMetadata());
        if (entity.getCreatedAt() != null) {
            api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getUpdatedAt() != null) {
            api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getLastSyncedAt() != null) {
            api.setLastSyncedAt(entity.getLastSyncedAt().atOffset(ZoneOffset.UTC));
        }
        return api;
    }

    public ContentLibraryList toPagedList(Page<ContentLibraryEntity> page) {
        List<ContentLibrary> items = page.getContent().stream().map(this::toApi).toList();
        ContentLibraryList list = new ContentLibraryList();
        list.setTotal((int) page.getTotalElements());
        list.setPage(page.getNumber() + 1);
        list.setPerPage(page.getSize());
        list.setItems(items);
        return list;
    }

    public static String normalizeLowerEnum(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.toLowerCase(Locale.ROOT);
    }
}
