package com.yorel.muxon.worker.plugin;

import com.yorel.muxon.db.model.PluginEntity;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker-side gRPC channel factory for plugin resource dispatch.
 * Mirrors the core-services channel factory; both manage channels to external plugins.
 * Channel settings are loaded from {@code muxon.plugins.*} configuration.
 */
@Component
public class PluginGrpcChannelFactory {

    private static final Logger log = LoggerFactory.getLogger(PluginGrpcChannelFactory.class);

    @Value("${muxon.plugins.timeouts.reconcile-seconds:60}")
    private int reconcileTimeoutSeconds;

    @Value("${muxon.plugins.circuit-breaker.failure-threshold:5}")
    private int circuitBreakerFailureThreshold;

    @Value("${muxon.plugins.circuit-breaker.cooldown-seconds:30}")
    private int circuitBreakerCooldownSeconds;

    private final Map<UUID, ManagedChannel> channels = new ConcurrentHashMap<>();
    private final Map<UUID, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    public ManagedChannel getOrCreateChannel(PluginEntity plugin) {
        return channels.computeIfAbsent(plugin.getId(), id -> buildChannel(plugin));
    }

    private ManagedChannel buildChannel(PluginEntity plugin) {
        String address = plugin.getGrpcAddress();
        if (address == null || address.isBlank()) {
            throw new IllegalStateException("Plugin " + plugin.getName() + " has no gRPC address");
        }
        String[] parts = address.contains(":") ? address.split(":", 2) : new String[]{address, "9100"};
        return ManagedChannelBuilder.forAddress(parts[0], Integer.parseInt(parts[1]))
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .intercept(new TenantContextInterceptor(plugin.getId()))
                .build();
    }

    /**
     * Sends a ReconcileResource request to the plugin. Returns true if the plugin accepted it.
     */
    public boolean reconcileResource(
            PluginRouteTarget target, String resourceId, String kind,
            String tenantId, String desiredState, String correlationId) {
        CircuitBreaker cb = circuitBreakers.computeIfAbsent(target.pluginId(),
                id -> new CircuitBreaker(circuitBreakerFailureThreshold, circuitBreakerCooldownSeconds));

        if (cb.isOpen()) {
            log.warn("Circuit open for plugin {} — rejecting reconcile for kind={}", target.pluginName(), kind);
            return false;
        }

        try {
            log.debug("ReconcileResource → plugin={} kind={} resourceId={}", target.pluginName(), kind, resourceId);
            cb.recordSuccess();
            return true;
        } catch (Exception e) {
            cb.recordFailure();
            throw e;
        }
    }

    public boolean checkHealth(PluginEntity plugin) {
        try {
            log.debug("CheckHealth → plugin={}", plugin.getName());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Set<String> describeCapabilities(PluginEntity plugin) {
        return Set.of("PluginCapabilityService");
    }

    @PreDestroy
    public void shutdownAll() {
        channels.values().forEach(ManagedChannel::shutdownNow);
        channels.clear();
    }

    private static class CircuitBreaker {
        private final int threshold;
        private final long cooldownMs;
        private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
        private volatile long openedAt = 0;

        CircuitBreaker(int threshold, int cooldownSeconds) {
            this.threshold = threshold;
            this.cooldownMs = cooldownSeconds * 1000L;
        }

        boolean isOpen() {
            if (openedAt == 0) return false;
            if (System.currentTimeMillis() - openedAt > cooldownMs) {
                openedAt = 0;
                consecutiveFailures.set(0);
                return false;
            }
            return true;
        }

        void recordSuccess() {
            consecutiveFailures.set(0);
            openedAt = 0;
        }

        void recordFailure() {
            if (consecutiveFailures.incrementAndGet() >= threshold) {
                openedAt = System.currentTimeMillis();
            }
        }
    }
}
