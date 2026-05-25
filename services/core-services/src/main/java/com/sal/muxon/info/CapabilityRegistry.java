package com.sal.muxon.info;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Aggregates capabilities from all registered {@link CapabilityProvider}s
 * into a single unified set.
 */
@Component
public class CapabilityRegistry {

    private final List<CapabilityProvider> providers;

    /**
     * Simple in-memory cache of the last resolved capability set.
     * <p>
     * This can be invalidated explicitly in the future when configuration
     * changes (e.g. license reload, plugin changes).
     */
    private final AtomicReference<Set<String>> cachedCapabilities = new AtomicReference<>();

    public CapabilityRegistry(List<CapabilityProvider> providers) {
        this.providers = providers;
    }

    /**
     * Resolve the unified capability set by aggregating all providers.
     * Results are cached until {@link #invalidate()} is called.
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
     * Invalidate the cached capability set so that the next call to
     * {@link #resolveCapabilities()} recomputes it from all providers.
     */
    public void invalidate() {
        cachedCapabilities.set(null);
    }
}

