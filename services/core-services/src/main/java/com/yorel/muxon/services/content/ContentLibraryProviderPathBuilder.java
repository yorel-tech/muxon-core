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
package com.yorel.muxon.services.content;

import com.yorel.muxon.common.ContentLibraryProviderPaths;
import com.yorel.muxon.config.MuxonProperties;
import com.yorel.muxon.db.model.ContentItemEntity;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Resolves provider-side paths using {@link MuxonProperties} (instanceName / instanceId). */
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
      // VM templates are directories of artifacts (template.json + disks/). Use a stable primary
      // artifact name.
      return "template.json";
    }
    return ContentLibraryProviderPaths.filenameForStorage(itemName, contentType);
  }

  public String providerRelativePath(
      UUID libraryId, UUID itemId, String itemName, String contentType) {
    String fn = filenameForStorage(itemName, contentType);
    return ContentLibraryProviderPaths.relativePath(instanceSegment(), libraryId, itemId, fn);
  }

  /**
   * Sets {@link ContentItemEntity#providerRelativePath} and {@link
   * ContentItemEntity#muxonInstanceSegment} from current instance configuration and item identity.
   */
  public void applyProviderPaths(ContentItemEntity item) {
    item.setMuxonInstanceSegment(instanceSegment());
    item.setProviderRelativePath(
        providerRelativePath(
            item.getLibraryId(), item.getId(), item.getName(), item.getContentType()));
  }
}
