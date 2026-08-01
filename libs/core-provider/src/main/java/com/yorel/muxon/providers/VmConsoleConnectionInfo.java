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
package com.yorel.muxon.providers;

/**
 * Target for the console proxy: either a raw TCP/TLS socket to the hypervisor, or (for Proxmox) an
 * upstream WebSocket URL with auth headers while the browser still receives binary RFB over Muxon's
 * WebSocket.
 *
 * @param consoleType Type of stream (VNC binary RFB, SPICE, etc.)
 * @param host Resolvable host or IP (logging / legacy; may match API host for Proxmox WS mode)
 * @param port TCP port for direct mode; for Proxmox WS mode often the VNC proxy port from API
 *     (informational)
 * @param password VNC password / Proxmox vncticket for noVNC when needed (may be null)
 * @param tls Whether the TCP connection should use TLS (direct mode; Proxmox WS uses wss from URL)
 * @param tlsPort Optional explicit TLS port if different from {@code port}
 * @param upstreamWebSocketUrl When non-null, console-proxy opens this WebSocket instead of {@code
 *     host}:{@code port}
 * @param upstreamWebSocketCookie Optional {@code Cookie} header value (e.g. {@code
 *     PVEAuthCookie=...})
 * @param upstreamWebSocketCsrfToken Optional Proxmox {@code CSRFPreventionToken} header for the WS
 *     handshake
 * @param upstreamWebSocketAuthorization Optional {@code Authorization} header (e.g. {@code
 *     PVEAPIToken=...})
 */
public record VmConsoleConnectionInfo(
    VmConsoleType consoleType,
    String host,
    int port,
    String password,
    boolean tls,
    Integer tlsPort,
    String upstreamWebSocketUrl,
    String upstreamWebSocketCookie,
    String upstreamWebSocketCsrfToken,
    String upstreamWebSocketAuthorization) {
  public VmConsoleConnectionInfo(
      VmConsoleType consoleType,
      String host,
      int port,
      String password,
      boolean tls,
      Integer tlsPort) {
    this(consoleType, host, port, password, tls, tlsPort, null, null, null, null);
  }
}
