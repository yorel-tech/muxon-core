package com.scal.muxon.providers.network;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * SPI for network provider implementations.
 *
 * <p>Backed by fabric_network context; each provider type (Libvirt, Proxmox, Kubernetes)
 * implements this interface to manage virtual network segments and NIC attachments.
 *
 * <p>Methods:
 * <ul>
 *   <li>{@link #createSubnetSegment} — provision a virtual network segment on the provider underlay</li>
 *   <li>{@link #deleteSubnetSegment} — tear down a previously created segment</li>
 *   <li>{@link #resolveNicAttachment} — translate a subnet reference to a provider-specific NIC attachment</li>
 *   <li>{@link #applyNicPolicy} — apply security group rules at the virtual NIC level</li>
 * </ul>
 */
public interface NetworkProvider {

    /**
     * Get the unique identifier for this provider implementation.
     */
    String id();

    /**
     * Create a virtual network segment for the given subnet specification.
     *
     * @param spec Subnet specification with CIDR, fabric network handle, etc.
     * @return Provider-assigned handle for the created segment (stored as subnet.provider_handle)
     */
    CompletableFuture<String> createSubnetSegment(SubnetSpec spec);

    /**
     * Delete a previously provisioned subnet segment.
     *
     * @param providerHandle The provider handle returned by {@link #createSubnetSegment}
     */
    CompletableFuture<Void> deleteSubnetSegment(String providerHandle);

    /**
     * Resolve a NIC attachment reference to a provider-specific network configuration.
     * Used by the VM provisioning flow to pass the correct network source to the provider driver.
     *
     * @param ref NIC attachment reference containing subnet context
     * @return Resolved attachment with provider-specific bridge or network name
     */
    CompletableFuture<ResolvedNicAttachment> resolveNicAttachment(NicAttachmentRef ref);

    /**
     * Apply security group enforcement rules at the virtual NIC level.
     * Called after VM NIC attachment to enforce micro-segmentation policies.
     *
     * @param vmExternalId Provider-assigned VM identifier
     * @param nicMacAddress MAC address of the virtual NIC
     * @param securityGroupRules Serialized rules to apply (nftables/iptables format)
     */
    default CompletableFuture<Void> applyNicPolicy(String vmExternalId, String nicMacAddress,
                                                     List<String> securityGroupRules) {
        return CompletableFuture.completedFuture(null);
    }
}
