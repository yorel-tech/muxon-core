package com.sal.muxon.providers.network;

import java.util.List;
import java.util.UUID;

/**
 * Specification for creating a subnet segment on the provider underlay.
 */
public record SubnetSpec(
        UUID subnetId,
        String name,
        String cidr,
        String gatewayIp,
        List<String> dnsServers,
        String fabricNetworkExternalId,
        String fabricNetworkType
) {}
