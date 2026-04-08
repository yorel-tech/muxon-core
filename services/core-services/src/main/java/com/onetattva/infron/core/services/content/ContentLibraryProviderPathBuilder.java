package com.onetattva.infron.core.services.content;

import com.onetattva.infron.core.common.ContentLibraryProviderPaths;
import com.onetattva.infron.core.config.InfronProperties;
import com.onetattva.infron.db.model.ContentItemEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Resolves provider-side paths using {@link InfronProperties} (instanceName / instanceId).
 */
@Service
public class ContentLibraryProviderPathBuilder {

    private final InfronProperties infronProperties;

    public ContentLibraryProviderPathBuilder(InfronProperties infronProperties) {
        this.infronProperties = infronProperties;
    }

    public String instanceSegment() {
        return ContentLibraryProviderPaths.instanceSegment(
                ContentLibraryProviderPaths.sanitizeInstanceName(infronProperties.getInstanceName()),
                infronProperties.getInstanceId());
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
     * Sets {@link ContentItemEntity#providerRelativePath} and {@link ContentItemEntity#infronInstanceSegment}
     * from current instance configuration and item identity.
     */
    public void applyProviderPaths(ContentItemEntity item) {
        item.setInfronInstanceSegment(instanceSegment());
        item.setProviderRelativePath(
                providerRelativePath(item.getLibraryId(), item.getId(), item.getName(), item.getContentType()));
    }
}
