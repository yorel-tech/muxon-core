package com.yorel.muxon.providers;

import java.util.List;

/**
 * Information returned by the QEMU guest agent (QGA) channel.
 * Used for post-boot telemetry: IP addresses, hostname, and customization phase.
 */
public record GuestAgentInfo(
        List<String> ipAddresses,
        String hostname,
        /** Raw cloud-init status JSON string (Linux only; null on Windows). */
        String cloudInitStatusJson,
        /** True when QGA responded; false when the channel is not yet reachable. */
        boolean agentReachable
) {
    public static GuestAgentInfo unreachable() {
        return new GuestAgentInfo(List.of(), null, null, false);
    }
}
