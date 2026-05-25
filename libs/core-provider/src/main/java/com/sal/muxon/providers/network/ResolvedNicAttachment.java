package com.sal.muxon.providers.network;

/**
 * Result of resolving a VM NIC attachment to a provider-specific network handle.
 */
public record ResolvedNicAttachment(
        AttachmentType attachmentType,
        String networkName,
        String bridgeName
) {
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
