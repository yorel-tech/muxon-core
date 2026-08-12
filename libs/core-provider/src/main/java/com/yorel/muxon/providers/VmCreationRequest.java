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
package com.yorel.muxon.providers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Request for VM creation with provider context. */
public record VmCreationRequest(
    UUID vmId,
    String spec,
    ProviderContext providerContext,
    Map<String, String> metadata,
    String correlationId,
    String sourceImagePath,
    List<IsoAttachment> isoAttachments,
    /** Seed ISO for guest customization; null when customization is not requested. */
    CustomizationSeed customizationSeed,
    /** When true the provider must expose the QEMU guest agent virtio-serial channel. */
    boolean enableGuestAgent,
    /** Optional: subnet ID for NIC attachment resolution via NetworkProvider. */
    UUID subnetId,
    /** Subnet CIDR block; used for NIC attachment resolution. */
    String subnetCidr,
    /** Provider handle stored on the subnet entity after provisioning. */
    String subnetProviderHandle,
    /** external_id of the backing fabric_network; used to resolve bridge/network name. */
    String fabricNetworkExternalId,
    /** Type of the backing fabric_network (BRIDGE, VLAN, VXLAN, OVS, UNDERLAY). */
    String fabricNetworkType,
    /** Security group IDs applied at this VM's primary NIC (vNIC-level). */
    List<UUID> securityGroupIds,
    /** Stack ID this VM belongs to. */
    UUID stackId) {
  public VmCreationRequest {
    isoAttachments = isoAttachments == null ? List.of() : List.copyOf(isoAttachments);
    securityGroupIds = securityGroupIds == null ? List.of() : List.copyOf(securityGroupIds);
  }

  public static VmCreationRequestBuilder builder() {
    return new VmCreationRequestBuilder();
  }

  public static class VmCreationRequestBuilder {
    private UUID vmId;
    private String spec;
    private ProviderContext providerContext;
    private Map<String, String> metadata;
    private String correlationId;
    private String sourceImagePath;
    private List<IsoAttachment> isoAttachments = Collections.emptyList();
    private CustomizationSeed customizationSeed;
    private boolean enableGuestAgent;
    private UUID subnetId;
    private String subnetCidr;
    private String subnetProviderHandle;
    private String fabricNetworkExternalId;
    private String fabricNetworkType;
    private List<UUID> securityGroupIds = Collections.emptyList();
    private UUID stackId;

    public VmCreationRequestBuilder vmId(UUID vmId) {
      this.vmId = vmId;
      return this;
    }

    public VmCreationRequestBuilder spec(String spec) {
      this.spec = spec;
      return this;
    }

    public VmCreationRequestBuilder providerContext(ProviderContext providerContext) {
      this.providerContext = providerContext;
      return this;
    }

    public VmCreationRequestBuilder metadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    public VmCreationRequestBuilder correlationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public VmCreationRequestBuilder sourceImagePath(String sourceImagePath) {
      this.sourceImagePath = sourceImagePath;
      return this;
    }

    public VmCreationRequestBuilder isoAttachments(List<IsoAttachment> isoAttachments) {
      this.isoAttachments = isoAttachments == null ? Collections.emptyList() : isoAttachments;
      return this;
    }

    public VmCreationRequestBuilder customizationSeed(CustomizationSeed customizationSeed) {
      this.customizationSeed = customizationSeed;
      return this;
    }

    public VmCreationRequestBuilder enableGuestAgent(boolean enableGuestAgent) {
      this.enableGuestAgent = enableGuestAgent;
      return this;
    }

    public VmCreationRequestBuilder subnetId(UUID subnetId) {
      this.subnetId = subnetId;
      return this;
    }

    public VmCreationRequestBuilder subnetCidr(String subnetCidr) {
      this.subnetCidr = subnetCidr;
      return this;
    }

    public VmCreationRequestBuilder subnetProviderHandle(String subnetProviderHandle) {
      this.subnetProviderHandle = subnetProviderHandle;
      return this;
    }

    public VmCreationRequestBuilder fabricNetworkExternalId(String fabricNetworkExternalId) {
      this.fabricNetworkExternalId = fabricNetworkExternalId;
      return this;
    }

    public VmCreationRequestBuilder fabricNetworkType(String fabricNetworkType) {
      this.fabricNetworkType = fabricNetworkType;
      return this;
    }

    public VmCreationRequestBuilder securityGroupIds(List<UUID> securityGroupIds) {
      this.securityGroupIds = securityGroupIds == null ? Collections.emptyList() : securityGroupIds;
      return this;
    }

    public VmCreationRequestBuilder stackId(UUID stackId) {
      this.stackId = stackId;
      return this;
    }

    public VmCreationRequest build() {
      return new VmCreationRequest(
          vmId,
          spec,
          providerContext,
          metadata,
          correlationId,
          sourceImagePath,
          isoAttachments,
          customizationSeed,
          enableGuestAgent,
          subnetId,
          subnetCidr,
          subnetProviderHandle,
          fabricNetworkExternalId,
          fabricNetworkType,
          securityGroupIds,
          stackId);
    }
  }
}
