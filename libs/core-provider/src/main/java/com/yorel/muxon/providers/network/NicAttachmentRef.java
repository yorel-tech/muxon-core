package com.yorel.muxon.providers.network;

import java.util.List;
import java.util.UUID;

/**
 * Reference used to resolve a VM NIC attachment from subnet context.
 */
public record NicAttachmentRef(
        UUID subnetId,
        String subnetCidr,
        String providerHandle,
        String fabricNetworkExternalId,
        String fabricNetworkType,
        List<UUID> securityGroupIds
) {}
