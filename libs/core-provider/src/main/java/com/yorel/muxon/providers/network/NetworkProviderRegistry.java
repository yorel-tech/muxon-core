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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registry for NetworkProvider implementations, mirroring the VmProviderRegistry pattern.
 *
 * <p>Implementations are registered by their {@link NetworkProvider#id()} and resolved by the
 * SubnetProvisioningService and VM orchestration layer.
 */
@Component
public class NetworkProviderRegistry {

  private static final Logger logger = LoggerFactory.getLogger(NetworkProviderRegistry.class);

  private final Map<String, NetworkProvider> providersById = new ConcurrentHashMap<>();

  public NetworkProviderRegistry(List<NetworkProvider> providerBeans) {
    for (NetworkProvider provider : providerBeans) {
      providersById.put(provider.id(), provider);
      logger.info("Registered NetworkProvider: {}", provider.id());
    }
  }

  public Optional<NetworkProvider> getProvider(String providerId) {
    return Optional.ofNullable(providersById.get(providerId));
  }

  public Map<String, NetworkProvider> getAllProviders() {
    return Map.copyOf(providersById);
  }
}
