package com.krito.muxon.core.providers.proxmox;

import com.krito.muxon.core.common.ContentLibraryProviderPaths;

import java.util.UUID;

/**
 * Resolves Proxmox/KVM-style relative paths under a storage pool for content library items.
 * Wire with {@code infron.instanceName} / {@code infron.instanceId} from the host application.
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
