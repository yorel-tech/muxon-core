package com.onetattva.infron.core.services.content;

import com.onetattva.infron.api.model.ContentItem;
import com.onetattva.infron.api.model.ContentItemList;
import com.onetattva.infron.api.model.ContentItemStatus;
import com.onetattva.infron.api.model.ContentType;
import com.onetattva.infron.db.model.ContentItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;

@Component
public class ContentItemApiConverter {

    public ContentItem toApi(ContentItemEntity entity) {
        ContentItem api = new ContentItem();
        api.setId(entity.getId());
        api.setLibraryId(entity.getLibraryId());
        api.setName(entity.getName());
        api.setDescription(entity.getDescription());
        api.setContentType(ContentType.fromValue(entity.getContentType()));
        api.setVersion(entity.getVersionLabel());
        api.setSizeBytes(entity.getSizeBytes());
        api.setChecksum(entity.getChecksum());
        api.setChecksumAlgorithm(entity.getChecksumAlgorithm());
        api.setSourceUrl(entity.getSourceUrl());
        api.setSourceItemId(entity.getSourceItemId());
        api.setContentStatus(ContentItemStatus.fromValue(entity.getContentStatus()));
        api.setMetadata(entity.getMetadata());
        if (entity.getCreatedAt() != null) {
            api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getUpdatedAt() != null) {
            api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getLastReplicatedAt() != null) {
            api.setLastReplicatedAt(entity.getLastReplicatedAt().atOffset(ZoneOffset.UTC));
        }
        api.setProviderRelativePath(entity.getProviderRelativePath());
        api.setInfronInstanceSegment(entity.getInfronInstanceSegment());
        return api;
    }

    public ContentItemList toPagedList(Page<ContentItemEntity> page) {
        List<ContentItem> items = page.getContent().stream().map(this::toApi).toList();
        ContentItemList list = new ContentItemList();
        list.setTotal((int) page.getTotalElements());
        list.setPage(page.getNumber() + 1);
        list.setPerPage(page.getSize());
        list.setItems(items);
        return list;
    }
}
