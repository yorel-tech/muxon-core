package com.scal.muxon.providers.proxmox;

import com.scal.muxon.providers.network.NicAttachmentRef;
import com.scal.muxon.providers.network.NetworkProvider;
import com.scal.muxon.providers.network.ResolvedNicAttachment;
import com.scal.muxon.providers.network.SubnetSpec;

import java.util.concurrent.CompletableFuture;

/**
 * Stub Proxmox NetworkProvider — all methods throw {@link UnsupportedOperationException}.
 * Full implementation is deferred to a future Proxmox networking change.
 */
public class ProxmoxNetworkProvider implements NetworkProvider {

    @Override
    public String id() {
        return "proxmox-network";
    }

    @Override
    public CompletableFuture<String> createSubnetSegment(SubnetSpec spec) {
        throw new UnsupportedOperationException("ProxmoxNetworkProvider: createSubnetSegment is not yet implemented");
    }

    @Override
    public CompletableFuture<Void> deleteSubnetSegment(String providerHandle) {
        throw new UnsupportedOperationException("ProxmoxNetworkProvider: deleteSubnetSegment is not yet implemented");
    }

    @Override
    public CompletableFuture<ResolvedNicAttachment> resolveNicAttachment(NicAttachmentRef ref) {
        throw new UnsupportedOperationException("ProxmoxNetworkProvider: resolveNicAttachment is not yet implemented");
    }
}
