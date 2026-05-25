package com.sal.muxon.services.content;

import com.sal.muxon.common.ContentLibraryProviderPaths;
import com.sal.muxon.config.MuxonProperties;
import com.sal.muxon.db.model.ContentItemEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Resolves provider-side paths using {@link MuxonProperties} (instanceName / instanceId).
 */
@Service
public class ContentLibraryProviderPathBuilder {

    private final MuxonProperties muxonProperties;

    public ContentLibraryProviderPathBuilder(MuxonProperties muxonProperties) {
        this.muxonProperties = muxonProperties;
    }

    public String instanceSegment() {
        return ContentLibraryProviderPaths.instanceSegment(
                ContentLibraryProviderPaths.sanitizeInstanceName(muxonProperties.getInstanceName()),
                muxonProperties.getInstanceId());
    }

    public String filenameForStorage(String itemName, String contentType) {
        if (contentType != null && "vm_template".equalsIgnoreCase(contentType.trim())) {
            // VM templates are directories of artifacts (template.json + disks/). Use a stable primary artifact name.
            return "template.json";
        }
        return ContentLibraryProviderPaths.filenameForStorage(itemName, contentType);
    }

    public String providerRelativePath(UUID libraryId, UUID itemId, String itemName, String contentType) {
        String fn = filenameForStorage(itemName, contentType);
        return ContentLibraryProviderPaths.relativePath(instanceSegment(), libraryId, itemId, fn);
    }

    /**
     * Sets {@link ContentItemEntity#providerRelativePath} and {@link ContentItemEntity#muxonInstanceSegment}
     * from current instance configuration and item identity.
     */
    public void applyProviderPaths(ContentItemEntity item) {
        item.setMuxonInstanceSegment(instanceSegment());
        item.setProviderRelativePath(
                providerRelativePath(item.getLibraryId(), item.getId(), item.getName(), item.getContentType()));
    }
}
