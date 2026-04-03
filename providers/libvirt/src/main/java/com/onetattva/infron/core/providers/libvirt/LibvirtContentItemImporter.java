package com.onetattva.infron.core.providers.libvirt;

import com.onetattva.infron.core.common.ContentLibraryProviderPaths;

import java.util.UUID;

/**
 * Resolves libvirt/KVM storage pool relative paths for content library items.
 * Wire with {@code infron.instanceName} / {@code infron.instanceId} from the host application.
 */
public final class LibvirtContentItemImporter {

    private final String instanceName;
    private final int instanceId;

    public LibvirtContentItemImporter(String instanceName, int instanceId) {
        this.instanceName = instanceName;
        this.instanceId = instanceId;
    }

    public String providerRelativePath(UUID libraryId, UUID itemId, String rawFilename) {
        return ContentLibraryProviderPaths.buildRelativePath(instanceName, instanceId, libraryId, itemId, rawFilename);
    }

    public String providerRelativePathForItem(UUID libraryId, UUID itemId, String itemName, String contentType) {
        String fn = ContentLibraryProviderPaths.filenameForStorage(itemName, contentType);
        return ContentLibraryProviderPaths.buildRelativePath(instanceName, instanceId, libraryId, itemId, fn);
    }
}
