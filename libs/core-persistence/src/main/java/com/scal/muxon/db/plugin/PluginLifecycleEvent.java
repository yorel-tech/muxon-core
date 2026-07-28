package com.scal.muxon.db.plugin;

import com.scal.muxon.db.model.PluginEntity;
import org.springframework.context.ApplicationEvent;

/**
 * Published when a plugin's lifecycle state changes (activated, disabled, degraded, recovered).
 * Shared across core-services (publisher) and core-worker (listeners) to avoid circular deps.
 */
public class PluginLifecycleEvent extends ApplicationEvent {

    private final PluginEntity plugin;
    private final String eventType;

    public PluginLifecycleEvent(Object source, PluginEntity plugin, String eventType) {
        super(source);
        this.plugin = plugin;
        this.eventType = eventType;
    }

    public PluginEntity getPlugin() { return plugin; }
    public String getEventType() { return eventType; }
}
