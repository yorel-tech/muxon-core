package com.scal.muxon.providers;

import com.scal.muxon.providers.network.NicAttachmentRef;
import com.scal.muxon.providers.network.NetworkProvider;
import com.scal.muxon.providers.network.ResolvedNicAttachment;
import com.scal.muxon.providers.network.SubnetSpec;

import java.util.concurrent.CompletableFuture;

/**
 * Stub Kubernetes NetworkProvider — all methods throw {@link UnsupportedOperationException}.
 * Full implementation is deferred to the Kubernetes networking change.
 */
public class KubernetesNetworkProvider implements NetworkProvider {

    @Override
    public String id() {
        return "kubernetes-network";
    }

    @Override
    public CompletableFuture<String> createSubnetSegment(SubnetSpec spec) {
        throw new UnsupportedOperationException("KubernetesNetworkProvider: createSubnetSegment is not yet implemented");
    }

    @Override
    public CompletableFuture<Void> deleteSubnetSegment(String providerHandle) {
        throw new UnsupportedOperationException("KubernetesNetworkProvider: deleteSubnetSegment is not yet implemented");
    }

    @Override
    public CompletableFuture<ResolvedNicAttachment> resolveNicAttachment(NicAttachmentRef ref) {
        throw new UnsupportedOperationException("KubernetesNetworkProvider: resolveNicAttachment is not yet implemented");
    }
}
