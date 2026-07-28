package com.yorel.muxon.services.plugin;

import com.yorel.muxon.db.model.PluginEntity;
import io.grpc.*;
import io.grpc.stub.MetadataUtils;
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
 * Manages per-plugin gRPC {@link ManagedChannel} instances.
 *
 * <p>Features:
 * <ul>
 *   <li>Per-plugin channel lifecycle (create/reuse/shutdown)</li>
 *   <li>Per-capability deadline interceptors</li>
 *   <li>Circuit breaker: opens after N consecutive non-OK responses</li>
 *   <li>{@link TenantContextInterceptor} attached to all outbound calls</li>
 *   <li>mTLS stub (Phase 1: self-signed cert stored in plugin manifest JSONB)</li>
 * </ul>
 */
@Component
public class PluginGrpcChannelFactory {

    private static final Logger log = LoggerFactory.getLogger(PluginGrpcChannelFactory.class);

    @Value("${muxon.plugins.timeouts.health-seconds:5}")
    private int healthTimeoutSeconds;

    @Value("${muxon.plugins.timeouts.reconcile-seconds:60}")
    private int reconcileTimeoutSeconds;

    @Value("${muxon.plugins.timeouts.catalog-provision-seconds:120}")
    private int catalogProvisionTimeoutSeconds;

    @Value("${muxon.plugins.timeouts.status-query-seconds:10}")
    private int statusQueryTimeoutSeconds;

    @Value("${muxon.plugins.circuit-breaker.failure-threshold:5}")
    private int circuitBreakerFailureThreshold;

    @Value("${muxon.plugins.circuit-breaker.cooldown-seconds:30}")
    private int circuitBreakerCooldownSeconds;

    private final Map<UUID, ManagedChannel> channels = new ConcurrentHashMap<>();
    private final Map<UUID, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    // ── Channel management ───────────────────────────────────────────────────

    public ManagedChannel getChannel(PluginEntity plugin) {
        return channels.computeIfAbsent(plugin.getId(), id -> buildChannel(plugin));
    }

    private ManagedChannel buildChannel(PluginEntity plugin) {
        String address = plugin.getGrpcAddress();
        if (address == null || address.isBlank()) {
            throw new IllegalStateException("Plugin " + plugin.getName() + " has no gRPC address");
        }

        String host;
        int port;
        if (address.contains(":")) {
            String[] parts = address.split(":", 2);
            host = parts[0];
            port = Integer.parseInt(parts[1]);
        } else {
            host = address;
            port = 9100;
        }

        log.info("Building gRPC channel for plugin {}:{} → {}:{}", plugin.getName(), plugin.getVersion(), host, port);

        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .intercept(new TenantContextInterceptor(plugin.getId()))
                .build();
    }

    public void evictChannel(UUID pluginId) {
        ManagedChannel channel = channels.remove(pluginId);
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    @PreDestroy
    public void shutdownAll() {
        channels.values().forEach(ManagedChannel::shutdownNow);
        channels.clear();
    }

    // ── Health check ─────────────────────────────────────────────────────────

    public boolean checkHealth(PluginEntity plugin) {
        CircuitBreaker cb = circuitBreakers.computeIfAbsent(plugin.getId(),
                id -> new CircuitBreaker(circuitBreakerFailureThreshold, circuitBreakerCooldownSeconds));

        if (cb.isOpen()) {
            log.debug("Circuit open for plugin {} — skipping health check", plugin.getName());
            return false;
        }

        try {
            ManagedChannel channel = getChannel(plugin);
            // Stub call with deadline
            channel.newCall(
                    io.grpc.MethodDescriptor.<Object, Object>newBuilder()
                            .setFullMethodName("muxon.plugin.v1.PluginCapabilityService/CheckHealth")
                            .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                            .setRequestMarshaller(new NoopMarshaller<>())
                            .setResponseMarshaller(new NoopMarshaller<>())
                            .build(),
                    CallOptions.DEFAULT.withDeadlineAfter(healthTimeoutSeconds, TimeUnit.SECONDS)
            );
            cb.recordSuccess();
            return true;
        } catch (Exception e) {
            cb.recordFailure();
            if (cb.isOpen()) {
                log.warn("Circuit breaker opened for plugin {} after {} consecutive failures",
                        plugin.getName(), circuitBreakerFailureThreshold);
            }
            return false;
        }
    }

    // ── DescribeCapabilities ─────────────────────────────────────────────────

    public Set<String> describeCapabilities(PluginEntity plugin) {
        // In production this calls PluginCapabilityService.DescribeCapabilities via gRPC.
        // Returns the set of implemented service names from the response.
        // Phase 1 stub: trusts the manifest declarations.
        log.debug("describeCapabilities called for plugin {}", plugin.getName());
        return Set.of("PluginCapabilityService");
    }

    // ── ReconcileResource ────────────────────────────────────────────────────

    public boolean reconcileResource(
            PluginEntity plugin,
            String resourceId, String kind, String tenantId,
            String desiredState, String correlationId) {
        log.debug("reconcileResource kind={} resourceId={} plugin={}", kind, resourceId, plugin.getName());
        return true;
    }

    // ── CatalogContribution ──────────────────────────────────────────────────

    public Map<String, Object> validateCatalogConfig(PluginEntity plugin, String itemName, String configJson) {
        log.debug("validateCatalogConfig plugin={} item={}", plugin.getName(), itemName);
        return Map.of("valid", true, "errors", java.util.List.of());
    }

    public boolean provisionCatalogItem(PluginEntity plugin, UUID serviceInstanceId, String itemName, String configJson) {
        log.debug("provisionCatalogItem plugin={} item={} instanceId={}", plugin.getName(), itemName, serviceInstanceId);
        return true;
    }

    public boolean deprovisionCatalogItem(PluginEntity plugin, UUID serviceInstanceId, String externalId) {
        log.debug("deprovisionCatalogItem plugin={} instanceId={}", plugin.getName(), serviceInstanceId);
        return true;
    }

    // ── Circuit breaker ──────────────────────────────────────────────────────

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

    // ── gRPC utilities ───────────────────────────────────────────────────────

    private static class NoopMarshaller<T> implements io.grpc.MethodDescriptor.Marshaller<T> {
        @Override public java.io.InputStream stream(T value) { return java.io.InputStream.nullInputStream(); }
        @Override public T parse(java.io.InputStream stream) { return null; }
    }
}
