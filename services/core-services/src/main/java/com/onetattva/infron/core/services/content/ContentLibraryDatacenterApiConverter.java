package com.onetattva.infron.core.services.content;

import com.onetattva.infron.api.model.ContentLibraryDatacenter;
import com.onetattva.infron.api.model.ContentLibraryDatacenterList;
import com.onetattva.infron.api.model.EntityReference;
import com.onetattva.infron.db.model.ContentLibraryDatacenterEntity;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Component
public class ContentLibraryDatacenterApiConverter {

    public ContentLibraryDatacenter toApi(ContentLibraryDatacenterEntity entity) {
        return toApi(entity, null);
    }

    public ContentLibraryDatacenter toApi(ContentLibraryDatacenterEntity entity, EntityReference datacenterRef) {
        ContentLibraryDatacenter api = new ContentLibraryDatacenter();
        api.setId(entity.getId());
        api.setLibraryId(entity.getLibraryId());
        api.setDatacenterId(entity.getDatacenterId());
        api.setStorageClassName(entity.getStorageClassName());
        if (datacenterRef != null) {
            api.setDatacenter(datacenterRef);
        } else if (entity.getDatacenterId() != null) {
            EntityReference ref = new EntityReference();
            ref.setId(entity.getDatacenterId());
            api.setDatacenter(ref);
        }
        api.setReplicateStatus(ContentLibraryDatacenter.ReplicateStatusEnum.fromValue(entity.getReplicateStatus()));
        if (entity.getLastReplicatedAt() != null) {
            api.setLastReplicatedAt(entity.getLastReplicatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getCreatedAt() != null) {
            api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getUpdatedAt() != null) {
            api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }
        return api;
    }

    public ContentLibraryDatacenterList toList(List<ContentLibraryDatacenterEntity> entities) {
        ContentLibraryDatacenterList list = new ContentLibraryDatacenterList();
        list.setItems(entities.stream().map(this::toApi).toList());
        return list;
    }

    public ContentLibraryDatacenterList toList(
            List<ContentLibraryDatacenterEntity> entities, java.util.Map<UUID, String> datacenterNamesById) {
        ContentLibraryDatacenterList list = new ContentLibraryDatacenterList();
        list.setItems(entities.stream()
                .map(e -> {
                    String name = datacenterNamesById != null
                            ? datacenterNamesById.get(e.getDatacenterId())
                            : null;
                    EntityReference ref = new EntityReference();
                    ref.setId(e.getDatacenterId());
                    if (name != null && !name.isBlank()) {
                        ref.setName(name);
                    }
                    return toApi(e, ref);
                })
                .toList());
        return list;
    }
}
