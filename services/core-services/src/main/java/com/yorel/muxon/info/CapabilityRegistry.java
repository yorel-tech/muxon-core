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
package com.yorel.muxon.info;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/**
 * Aggregates capabilities from all registered {@link CapabilityProvider}s into a single unified
 * set.
 */
@Component
public class CapabilityRegistry {

  private final List<CapabilityProvider> providers;

  /**
   * Simple in-memory cache of the last resolved capability set.
   *
   * <p>This can be invalidated explicitly in the future when configuration changes (e.g. license
   * reload, plugin changes).
   */
  private final AtomicReference<Set<String>> cachedCapabilities = new AtomicReference<>();

  public CapabilityRegistry(List<CapabilityProvider> providers) {
    this.providers = providers;
  }

  /**
   * Resolve the unified capability set by aggregating all providers. Results are cached until
   * {@link #invalidate()} is called.
   *
   * @return an immutable set of capabilities
   */
  public Set<String> resolveCapabilities() {
    Set<String> existing = cachedCapabilities.get();
    if (existing != null) {
      return existing;
    }

    Set<String> aggregated = new HashSet<>();
    for (CapabilityProvider provider : providers) {
      Set<String> contribution = provider.getCapabilities();
      if (contribution != null && !contribution.isEmpty()) {
        aggregated.addAll(contribution);
      }
    }

    Set<String> immutable = Collections.unmodifiableSet(aggregated);
    cachedCapabilities.set(immutable);
    return immutable;
  }

  /**
   * Invalidate the cached capability set so that the next call to {@link #resolveCapabilities()}
   * recomputes it from all providers.
   */
  public void invalidate() {
    cachedCapabilities.set(null);
  }
}
