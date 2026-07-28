package com.scal.muxon.worker.plugin;

import com.scal.muxon.api.enums.ResourceTypeStatus;
import com.scal.muxon.spi.queue.CommandMessage;
import com.scal.muxon.spi.queue.TaskEventQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Dispatches stack resource items with plugin-registered kinds to the owning plugin's
 * {@code ResourceReconcileService.ReconcileResource} gRPC call.
 *
 * <p>Called exclusively from {@code TaskRouter} for non-vm resource kinds.
 * VM-kind resources bypass this and go directly to {@code TenantAwareVmProviderRegistry}.
 */
@Component
public class PluginResourceDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PluginResourceDispatcher.class);

    @Autowired private PluginGrpcChannelFactory channelFactory;
    @Autowired private TaskEventQueue taskEventQueue;
    @Autowired private ResourceTypeRegistry resourceTypeRegistry;

    public void dispatch(CommandMessage command, PluginRouteTarget target) {
        if (target.status() == ResourceTypeStatus.UNAVAILABLE) {
            log.error("Kind '{}' is UNAVAILABLE (plugin {} is degraded/disabled) — rejecting command {}",
                    target.kind(), target.pluginName(), command.id());
            emitFailure(command, "Resource kind '" + target.kind() + "' is currently unavailable");
            return;
        }

        String resourceId = extractResourceId(command);
        String kind = target.kind();
        String tenantId = extractTenantId(command);
        String desiredState = extractDesiredState(command);
        String correlationId = command.correlationId();

        validateQuotaDimensions(command, target);

        try {
            boolean accepted = channelFactory.reconcileResource(
                    target, resourceId, kind, tenantId, desiredState, correlationId);

            if (accepted) {
                log.info("ReconcileResource accepted for kind={} resourceId={} plugin={}",
                        kind, resourceId, target.pluginName());
            } else {
                log.warn("ReconcileResource rejected for kind={} resourceId={} plugin={}",
                        kind, resourceId, target.pluginName());
                emitFailure(command, "Plugin rejected reconcile request");
            }
        } catch (Exception e) {
            log.error("ReconcileResource failed for kind={} resourceId={}: {}", kind, resourceId, e.getMessage());
            emitFailure(command, e.getMessage());
        }
    }

    private void validateQuotaDimensions(CommandMessage command, PluginRouteTarget target) {
        // Quota dimension validation is intentionally lightweight here:
        // the ResourceTypeDefinition declares quotaDimensions, and the core quota
        // engine validates them upstream before the command reaches the TaskRouter.
        // This method is a hook for future per-dispatch checks if needed.
    }

    private String extractResourceId(CommandMessage command) {
        Object id = command.payload().get("resourceId");
        return id != null ? id.toString() : command.entityId().toString();
    }

    private String extractTenantId(CommandMessage command) {
        return command.metadata().getOrDefault("tenantId", "");
    }

    private String extractDesiredState(CommandMessage command) {
        Object state = command.payload().get("desiredState");
        return state != null ? state.toString() : "{}";
    }

    private void emitFailure(CommandMessage command, String reason) {
        log.error("Plugin dispatch failed for command {}: {}", command.id(), reason);
    }
}
