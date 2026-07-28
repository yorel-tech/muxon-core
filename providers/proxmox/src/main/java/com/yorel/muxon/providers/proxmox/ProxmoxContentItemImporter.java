package com.yorel.muxon.providers.proxmox;

import com.yorel.muxon.common.ContentLibraryProviderPaths;

import java.util.UUID;

/**
 * Resolves Proxmox/KVM-style relative paths under a storage pool for content library items.
 * Wire with {@code muxon.instanceName} / {@code muxon.instanceId} from the host application.
 */
public final class ProxmoxContentItemImporter {

    private final String instanceName;
    private final int instanceId;

    public ProxmoxContentItemImporter(String instanceName, int instanceId) {
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
