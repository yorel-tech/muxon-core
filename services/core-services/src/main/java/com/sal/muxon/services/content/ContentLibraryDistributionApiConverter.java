package com.sal.muxon.services.content;

import com.sal.muxon.api.model.ContentLibraryDistribution;
import com.sal.muxon.api.model.ContentLibraryDistributionList;
import com.sal.muxon.api.model.EntityReference;
import com.sal.muxon.db.model.ContentLibraryDistributionEntity;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Component
public class ContentLibraryDistributionApiConverter {

    public ContentLibraryDistribution toApi(ContentLibraryDistributionEntity entity) {
        return toApi(entity, null);
    }

    public ContentLibraryDistribution toApi(ContentLibraryDistributionEntity entity, EntityReference datacenterRef) {
        ContentLibraryDistribution api = new ContentLibraryDistribution();
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
        api.setReplicateStatus(ContentLibraryDistribution.ReplicateStatusEnum.fromValue(entity.getReplicateStatus()));
        api.setProgressPercent(entity.getProgressPercent());
        api.setErrorMessage(entity.getErrorMessage());
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

    public ContentLibraryDistributionList toList(List<ContentLibraryDistributionEntity> entities) {
        ContentLibraryDistributionList list = new ContentLibraryDistributionList();
        list.setItems(entities.stream().map(this::toApi).toList());
        return list;
    }

    public ContentLibraryDistributionList toList(
            List<ContentLibraryDistributionEntity> entities, java.util.Map<UUID, String> datacenterNamesById) {
        ContentLibraryDistributionList list = new ContentLibraryDistributionList();
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
