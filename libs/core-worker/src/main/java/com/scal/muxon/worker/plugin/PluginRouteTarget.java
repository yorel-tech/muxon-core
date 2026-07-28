package com.scal.muxon.worker.plugin;

import com.scal.muxon.api.enums.ResourceTypeStatus;

import java.util.UUID;

/**
 * Resolved routing target for a plugin-registered resource kind.
 * Returned by {@code ResourceTypeRegistry.resolveKind(kind)}.
 */
public record PluginRouteTarget(
        UUID pluginId,
        String pluginName,
        String grpcAddress,
        String kind,
        ResourceTypeStatus status
) {}
