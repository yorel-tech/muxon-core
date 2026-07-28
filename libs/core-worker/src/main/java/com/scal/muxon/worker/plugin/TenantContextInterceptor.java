package com.scal.muxon.worker.plugin;

import io.grpc.*;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * gRPC client interceptor that attaches per-call tenant and observability context
 * as gRPC metadata headers on every outbound call to a plugin.
 *
 * <p>Headers attached:
 * <ul>
 *   <li>{@code x-plugin-id} — the plugin's registered UUID</li>
 *   <li>{@code x-tenant-id} — resolved from {@link MDC} or call context</li>
 *   <li>{@code x-operation-id} — idempotency key from call context</li>
 *   <li>{@code x-trace-id} — distributed trace ID from MDC</li>
 * </ul>
 */
public class TenantContextInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> X_PLUGIN_ID =
            Metadata.Key.of("x-plugin-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> X_TENANT_ID =
            Metadata.Key.of("x-tenant-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> X_OPERATION_ID =
            Metadata.Key.of("x-operation-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> X_TRACE_ID =
            Metadata.Key.of("x-trace-id", Metadata.ASCII_STRING_MARSHALLER);

    private final UUID pluginId;

    public TenantContextInterceptor(UUID pluginId) {
        this.pluginId = pluginId;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                headers.put(X_PLUGIN_ID, pluginId.toString());

                String tenantId = MDC.get("tenantId");
                if (tenantId != null) headers.put(X_TENANT_ID, tenantId);

                String traceId = MDC.get("traceId");
                if (traceId != null) headers.put(X_TRACE_ID, traceId);

                String operationId = MDC.get("operationId");
                if (operationId == null) operationId = UUID.randomUUID().toString();
                headers.put(X_OPERATION_ID, operationId);

                super.start(responseListener, headers);
            }
        };
    }
}
