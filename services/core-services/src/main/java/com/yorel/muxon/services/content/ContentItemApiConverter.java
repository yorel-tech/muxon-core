package com.yorel.muxon.services.content;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.yorel.muxon.api.model.ContentItem;
import com.yorel.muxon.api.model.ContentItemList;
import com.yorel.muxon.api.model.ContentItemStatus;
import com.yorel.muxon.api.model.ContainerImageContentItem;
import com.yorel.muxon.api.model.HelmChartContentItem;
import com.yorel.muxon.api.model.IsoContentItem;
import com.yorel.muxon.api.model.ScriptContentItem;
import com.yorel.muxon.api.model.VmTemplateContentItem;
import com.yorel.muxon.api.model.VmTemplateSpec;
import com.yorel.muxon.db.model.ContentItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ContentItemApiConverter {

    private final ObjectMapper objectMapper;

    public ContentItemApiConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ContentItem toApi(ContentItemEntity entity) {
        String type = entity.getContentType() == null ? "" : entity.getContentType().toLowerCase(Locale.ROOT);
        Object api;

        switch (type) {
            case "vm_template" -> {
                VmTemplateContentItem vm = new VmTemplateContentItem();
                if (entity.getTemplateSpec() != null) {
                    vm.setTemplateSpec(objectMapper.convertValue(entity.getTemplateSpec(), VmTemplateSpec.class));
                }
                api = vm;
            }
            case "iso" -> api = new IsoContentItem();
            case "script" -> api = new ScriptContentItem();
            case "container_image" -> api = new ContainerImageContentItem();
            case "helm_chart" -> api = new HelmChartContentItem();
            default -> api = new ScriptContentItem();
        }

        fillBase(api, entity);
        return (ContentItem) api;
    }

    private static void fillBase(Object api, ContentItemEntity entity) {
        if (api instanceof VmTemplateContentItem x) {
            fillBase(x, entity);
        } else if (api instanceof IsoContentItem x) {
            fillBase(x, entity);
        } else if (api instanceof ScriptContentItem x) {
            fillBase(x, entity);
        } else if (api instanceof ContainerImageContentItem x) {
            fillBase(x, entity);
        } else if (api instanceof HelmChartContentItem x) {
            fillBase(x, entity);
        } else {
            throw new IllegalStateException("Unsupported content item api type: " + api.getClass().getName());
        }
    }

    private static void fillBase(VmTemplateContentItem api, ContentItemEntity entity) {
        api.setId(entity.getId());
        api.setLibraryId(entity.getLibraryId());
        api.setName(entity.getName());
        api.setDescription(entity.getDescription());
        api.setContentType(entity.getContentType());
        api.setVersion(entity.getVersionLabel());
        api.setSizeBytes(entity.getSizeBytes());
        api.setChecksum(entity.getChecksum());
        api.setChecksumAlgorithm(entity.getChecksumAlgorithm());
        api.setSourceUrl(entity.getSourceUrl());
        api.setSourceItemId(entity.getSourceItemId());
        api.setContentStatus(ContentItemStatus.fromValue(entity.getContentStatus()));
        api.setMetadata(entity.getMetadata());
        if (entity.getCreatedAt() != null) api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        if (entity.getUpdatedAt() != null) api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        if (entity.getLastReplicatedAt() != null) api.setLastReplicatedAt(entity.getLastReplicatedAt().atOffset(ZoneOffset.UTC));
        api.setProviderRelativePath(entity.getProviderRelativePath());
        api.setMuxonInstanceSegment(entity.getMuxonInstanceSegment());
    }

    private static void fillBase(IsoContentItem api, ContentItemEntity entity) {
        api.setId(entity.getId());
        api.setLibraryId(entity.getLibraryId());
        api.setName(entity.getName());
        api.setDescription(entity.getDescription());
        api.setContentType(entity.getContentType());
        api.setVersion(entity.getVersionLabel());
        api.setSizeBytes(entity.getSizeBytes());
        api.setChecksum(entity.getChecksum());
        api.setChecksumAlgorithm(entity.getChecksumAlgorithm());
        api.setSourceUrl(entity.getSourceUrl());
        api.setSourceItemId(entity.getSourceItemId());
        api.setContentStatus(ContentItemStatus.fromValue(entity.getContentStatus()));
        api.setMetadata(entity.getMetadata());
        if (entity.getCreatedAt() != null) api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        if (entity.getUpdatedAt() != null) api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        if (entity.getLastReplicatedAt() != null) api.setLastReplicatedAt(entity.getLastReplicatedAt().atOffset(ZoneOffset.UTC));
        api.setProviderRelativePath(entity.getProviderRelativePath());
        api.setMuxonInstanceSegment(entity.getMuxonInstanceSegment());
    }

    private static void fillBase(ScriptContentItem api, ContentItemEntity entity) {
        api.setId(entity.getId());
        api.setLibraryId(entity.getLibraryId());
        api.setName(entity.getName());
        api.setDescription(entity.getDescription());
        api.setContentType(entity.getContentType());
        api.setVersion(entity.getVersionLabel());
        api.setSizeBytes(entity.getSizeBytes());
        api.setChecksum(entity.getChecksum());
        api.setChecksumAlgorithm(entity.getChecksumAlgorithm());
        api.setSourceUrl(entity.getSourceUrl());
        api.setSourceItemId(entity.getSourceItemId());
        api.setContentStatus(ContentItemStatus.fromValue(entity.getContentStatus()));
        api.setMetadata(entity.getMetadata());
        if (entity.getCreatedAt() != null) api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        if (entity.getUpdatedAt() != null) api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        if (entity.getLastReplicatedAt() != null) api.setLastReplicatedAt(entity.getLastReplicatedAt().atOffset(ZoneOffset.UTC));
        api.setProviderRelativePath(entity.getProviderRelativePath());
        api.setMuxonInstanceSegment(entity.getMuxonInstanceSegment());
    }

    private static void fillBase(ContainerImageContentItem api, ContentItemEntity entity) {
        api.setId(entity.getId());
        api.setLibraryId(entity.getLibraryId());
        api.setName(entity.getName());
        api.setDescription(entity.getDescription());
        api.setContentType(entity.getContentType());
        api.setVersion(entity.getVersionLabel());
        api.setSizeBytes(entity.getSizeBytes());
        api.setChecksum(entity.getChecksum());
        api.setChecksumAlgorithm(entity.getChecksumAlgorithm());
        api.setSourceUrl(entity.getSourceUrl());
        api.setSourceItemId(entity.getSourceItemId());
        api.setContentStatus(ContentItemStatus.fromValue(entity.getContentStatus()));
        api.setMetadata(entity.getMetadata());
        if (entity.getCreatedAt() != null) api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        if (entity.getUpdatedAt() != null) api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        if (entity.getLastReplicatedAt() != null) api.setLastReplicatedAt(entity.getLastReplicatedAt().atOffset(ZoneOffset.UTC));
        api.setProviderRelativePath(entity.getProviderRelativePath());
        api.setMuxonInstanceSegment(entity.getMuxonInstanceSegment());
    }

    private static void fillBase(HelmChartContentItem api, ContentItemEntity entity) {
        api.setId(entity.getId());
        api.setLibraryId(entity.getLibraryId());
        api.setName(entity.getName());
        api.setDescription(entity.getDescription());
        api.setContentType(entity.getContentType());
        api.setVersion(entity.getVersionLabel());
        api.setSizeBytes(entity.getSizeBytes());
        api.setChecksum(entity.getChecksum());
        api.setChecksumAlgorithm(entity.getChecksumAlgorithm());
        api.setSourceUrl(entity.getSourceUrl());
        api.setSourceItemId(entity.getSourceItemId());
        api.setContentStatus(ContentItemStatus.fromValue(entity.getContentStatus()));
        api.setMetadata(entity.getMetadata());
        if (entity.getCreatedAt() != null) api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        if (entity.getUpdatedAt() != null) api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        if (entity.getLastReplicatedAt() != null) api.setLastReplicatedAt(entity.getLastReplicatedAt().atOffset(ZoneOffset.UTC));
        api.setProviderRelativePath(entity.getProviderRelativePath());
        api.setMuxonInstanceSegment(entity.getMuxonInstanceSegment());
    }

    Map<String, Object> toTemplateSpecMap(VmTemplateSpec spec) {
        if (spec == null) return null;
        return objectMapper.convertValue(spec, new TypeReference<Map<String, Object>>() {});
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
