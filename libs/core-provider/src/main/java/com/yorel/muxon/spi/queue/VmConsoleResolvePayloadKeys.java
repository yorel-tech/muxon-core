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
package com.yorel.muxon.spi.queue;

/** JSON payload keys for {@link VmQueueCommands#CONSOLE_RESOLVE} queue rows. */
public final class VmConsoleResolvePayloadKeys {

  private VmConsoleResolvePayloadKeys() {}

  public static final String TENANT_DATACENTER_GRANT_ID = "tenantDatacenterGrantId";

  /**
   * Hypervisor VM provider id (UUID string); set by API / gRPC so orchestrator can resolve the
   * provider without traversing grant entities.
   */
  public static final String PROVIDER_ID = "providerId";

  public static final String EXTERNAL_ID = "externalId";
  public static final String NODE_ID = "nodeId";

  /** Present with value {@code true} when orchestrator completed successfully. */
  public static final String RESOLVED = "resolved";

  public static final String CONSOLE_TYPE = "consoleType";
  public static final String HOST = "host";
  public static final String PORT = "port";
  public static final String TLS = "tls";
  public static final String PASSWORD = "password";

  /**
   * When set, console-proxy dials this WebSocket instead of raw TCP to host:port (Proxmox {@code
   * vncwebsocket}).
   */
  public static final String UPSTREAM_WEB_SOCKET_URL = "upstreamWebSocketUrl";

  public static final String UPSTREAM_WEB_SOCKET_COOKIE = "upstreamWebSocketCookie";
  public static final String UPSTREAM_WEB_SOCKET_CSRF = "upstreamWebSocketCsrfToken";
  public static final String UPSTREAM_WEB_SOCKET_AUTHORIZATION = "upstreamWebSocketAuthorization";
}
