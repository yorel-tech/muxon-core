package com.yorel.muxon.worker;

import com.yorel.muxon.api.model.EntityType;
import com.yorel.muxon.spi.queue.CommandMessage;
import com.yorel.muxon.worker.executors.ContentTaskExecutor;
import com.yorel.muxon.worker.executors.ProviderTaskExecutor;
import com.yorel.muxon.worker.executors.VmTaskExecutor;
import com.yorel.muxon.worker.plugin.PluginResourceDispatcher;
import com.yorel.muxon.worker.plugin.PluginRouteTarget;
import com.yorel.muxon.worker.plugin.ResourceTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TaskRouter {

    private static final Logger log = LoggerFactory.getLogger(TaskRouter.class);

    @Value("${muxon.plugins.framework.enabled:true}")
    private boolean pluginFrameworkEnabled;

    @Autowired private VmTaskExecutor vmTaskExecutor;
    @Autowired private ProviderTaskExecutor providerTaskExecutor;
    @Autowired private ContentTaskExecutor contentTaskExecutor;
    @Autowired private ResourceTypeRegistry resourceTypeRegistry;
    @Autowired private PluginResourceDispatcher pluginResourceDispatcher;

    public void route(CommandMessage command) {
        if (command == null) {
            return;
        }
        EntityType type = command.entityType();
        if (type == null) {
            log.warn("Cannot route command {} (null entityType)", command.id());
            return;
        }

        switch (type) {
            case VM -> vmTaskExecutor.execute(command);
            case PROVIDER -> providerTaskExecutor.execute(command);
            case CONTENT_LIBRARY -> contentTaskExecutor.execute(command);
            case PLUGIN_RESOURCE -> routePluginResource(command);
            default -> log.warn("No executor for entityType={} commandId={}", type, command.id());
        }
    }

    /**
     * Routes a PLUGIN_RESOURCE command through the plugin framework dispatch path.
     * The resource kind is resolved from the command payload.
     * If the plugin framework is disabled, fails fast with a clear error.
     */
    private void routePluginResource(CommandMessage command) {
        if (!pluginFrameworkEnabled) {
            log.error("Plugin framework is disabled; rejecting PLUGIN_RESOURCE command {}", command.id());
            return;
        }

        String kind = extractKind(command);
        if (kind == null || kind.isBlank()) {
            log.error("PLUGIN_RESOURCE command {} has no 'kind' in payload", command.id());
            return;
        }

        // 'vm' kind must never enter this path
        if ("vm".equalsIgnoreCase(kind)) {
            log.error("'vm' kind must be dispatched as EntityType.VM, not PLUGIN_RESOURCE. Command: {}", command.id());
            return;
        }

        Optional<PluginRouteTarget> target = resourceTypeRegistry.resolveKind(kind);
        if (target.isEmpty()) {
            log.error("No active plugin registered for kind '{}' — rejecting command {}", kind, command.id());
            return;
        }

        pluginResourceDispatcher.dispatch(command, target.get());
    }

    private String extractKind(CommandMessage command) {
        Object kind = command.payload().get("kind");
        return kind != null ? kind.toString() : null;
    }
}

