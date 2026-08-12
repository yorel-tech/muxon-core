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

import com.yorel.muxon.providers.network.NetworkProvider;
import com.yorel.muxon.providers.network.NicAttachmentRef;
import com.yorel.muxon.providers.network.ResolvedNicAttachment;
import com.yorel.muxon.providers.network.SubnetSpec;
import java.util.concurrent.CompletableFuture;

/**
 * Stub Kubernetes NetworkProvider — all methods throw {@link UnsupportedOperationException}. Full
 * implementation is deferred to the Kubernetes networking change.
 */
public class KubernetesNetworkProvider implements NetworkProvider {

  @Override
  public String id() {
    return "kubernetes-network";
  }

  @Override
  public CompletableFuture<String> createSubnetSegment(SubnetSpec spec) {
    throw new UnsupportedOperationException(
        "KubernetesNetworkProvider: createSubnetSegment is not yet implemented");
  }

  @Override
  public CompletableFuture<Void> deleteSubnetSegment(String providerHandle) {
    throw new UnsupportedOperationException(
        "KubernetesNetworkProvider: deleteSubnetSegment is not yet implemented");
  }

  @Override
  public CompletableFuture<ResolvedNicAttachment> resolveNicAttachment(NicAttachmentRef ref) {
    throw new UnsupportedOperationException(
        "KubernetesNetworkProvider: resolveNicAttachment is not yet implemented");
  }
}
