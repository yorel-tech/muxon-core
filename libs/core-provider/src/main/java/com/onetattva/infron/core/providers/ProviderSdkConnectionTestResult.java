package com.onetattva.infron.core.providers;

import java.util.Map;

/**
 * Core-provider result type returned by {@link ProviderSdk#testConnection(ProviderConnectionInfo)}.
 */
public record ProviderSdkConnectionTestResult(
    boolean success,
    String message,
    int latencyMs,
    Map<String, String> capabilities
) {}

