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
package com.yorel.muxon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Console proxy URL and limits for VM graphical console sessions. */
@ConfigurationProperties(prefix = "muxon.console")
public class MuxonConsoleProperties {

  /**
   * Base WebSocket URL for the console-proxy service (e.g. ws://localhost:8082/ws/console). The
   * session token is appended as a query parameter.
   */
  private String proxyWsBaseUrl = "ws://localhost:8082/ws/console";

  private int maxSessionsPerUser = 5;

  /**
   * Max time to wait for the orchestrator to process a {@code VM_CONSOLE_RESOLVE_COMMAND} row
   * (seconds).
   */
  private int resolveTimeoutSeconds = 90;

  public String getProxyWsBaseUrl() {
    return proxyWsBaseUrl;
  }

  public void setProxyWsBaseUrl(String proxyWsBaseUrl) {
    this.proxyWsBaseUrl = proxyWsBaseUrl;
  }

  public int getMaxSessionsPerUser() {
    return maxSessionsPerUser;
  }

  public void setMaxSessionsPerUser(int maxSessionsPerUser) {
    this.maxSessionsPerUser = maxSessionsPerUser;
  }

  public int getResolveTimeoutSeconds() {
    return resolveTimeoutSeconds;
  }

  public void setResolveTimeoutSeconds(int resolveTimeoutSeconds) {
    this.resolveTimeoutSeconds = resolveTimeoutSeconds;
  }
}
