package com.onetattva.infron.core.services.content;

import com.onetattva.infron.api.model.ContentStorage;
import com.onetattva.infron.api.model.ContentStorageConfig;
import com.onetattva.infron.api.model.ContentStorageList;
import com.onetattva.infron.api.model.ContentStorageType;
import com.onetattva.infron.db.model.ContentStorageEntity;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ContentStorageApiConverter {

    private final ObjectMapper objectMapper;

    public ContentStorageApiConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ContentStorage toApi(ContentStorageEntity entity) {
        ContentStorage api = new ContentStorage();
        api.setId(entity.getId());
        api.setName(entity.getName());
        api.setType(ContentStorageType.fromValue(entity.getStorageType()));
        api.setConfig(toConfig(entity.getConfig()));
        api.setIsDefault(entity.isDefaultStorage());
        if (entity.getCreatedAt() != null) {
            api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getUpdatedAt() != null) {
            api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }
        return api;
    }

    public ContentStorageList toPagedList(Page<ContentStorageEntity> page) {
        List<ContentStorage> items = page.getContent().stream().map(this::toApi).toList();
        ContentStorageList list = new ContentStorageList();
        list.setTotal((int) page.getTotalElements());
        list.setPage(page.getNumber() + 1);
        list.setPerPage(page.getSize());
        list.setItems(items);
        return list;
    }

    public ContentStorageConfig toConfig(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return new ContentStorageConfig();
        }
        return objectMapper.convertValue(map, ContentStorageConfig.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> configToMap(ContentStorageConfig cfg) {
        if (cfg == null) {
            return new HashMap<>();
        }
        return objectMapper.convertValue(cfg, Map.class);
    }
}
