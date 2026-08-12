/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.providers.libvirt;

import com.yorel.muxon.common.ContentLibraryProviderPaths;
import java.util.UUID;

/**
 * Resolves libvirt/KVM storage pool relative paths for content library items. Wire with {@code
 * muxon.instanceName} / {@code muxon.instanceId} from the host application.
 */
public final class LibvirtContentItemImporter {

  private final String instanceName;
  private final int instanceId;

  public LibvirtContentItemImporter(String instanceName, int instanceId) {
    this.instanceName = instanceName;
    this.instanceId = instanceId;
  }

  public String providerRelativePath(UUID libraryId, UUID itemId, String rawFilename) {
    return ContentLibraryProviderPaths.buildRelativePath(
        instanceName, instanceId, libraryId, itemId, rawFilename);
  }

  public String providerRelativePathForItem(
      UUID libraryId, UUID itemId, String itemName, String contentType) {
    String fn = ContentLibraryProviderPaths.filenameForStorage(itemName, contentType);
    return ContentLibraryProviderPaths.buildRelativePath(
        instanceName, instanceId, libraryId, itemId, fn);
  }
}
