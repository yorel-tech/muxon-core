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
package com.yorel.muxon.providers.network;

/** Result of resolving a VM NIC attachment to a provider-specific network handle. */
public record ResolvedNicAttachment(
    AttachmentType attachmentType, String networkName, String bridgeName) {
  public enum AttachmentType {
    NETWORK,
    BRIDGE
  }

  public static ResolvedNicAttachment network(String networkName) {
    return new ResolvedNicAttachment(AttachmentType.NETWORK, networkName, null);
  }

  public static ResolvedNicAttachment bridge(String bridgeName) {
    return new ResolvedNicAttachment(AttachmentType.BRIDGE, null, bridgeName);
  }

  public String getEffectiveName() {
    return attachmentType == AttachmentType.BRIDGE ? bridgeName : networkName;
  }
}
