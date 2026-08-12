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
package com.yorel.muxon.db.plugin;

import com.yorel.muxon.db.model.PluginEntity;
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

  public PluginEntity getPlugin() {
    return plugin;
  }

  public String getEventType() {
    return eventType;
  }
}
