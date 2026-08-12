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
package com.yorel.muxon.console;

import com.yorel.muxon.db.model.ConsoleSessionConsoleType;
import com.yorel.muxon.db.model.ConsoleSessionEntity;
import com.yorel.muxon.db.model.ConsoleSessionStatus;
import com.yorel.muxon.db.repository.ConsoleSessionRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

/**
 * Binary WebSocket bridge: raw TCP/TLS to hypervisor VNC/SPICE, or (Proxmox) client WebSocket to
 * {@code /api2/json/.../vncwebsocket} with PVE auth headers.
 */
@Component
public class ConsoleWebSocketHandler extends BinaryWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(ConsoleWebSocketHandler.class);

  private static final String ATTR_SOCKET = "hypervisorSocket";
  private static final String ATTR_UPSTREAM = "upstreamFuture";
  private static final String ATTR_UPSTREAM_WS = "upstreamPveWebSocket";
  private static final String ATTR_SESSION_ROW_ID = "consoleSessionDbId";

  private final ConsoleSessionRepository consoleSessionRepository;
  private final ConsoleProxyProperties properties;
  private final VncProxyService vncProxyService;
  private final SpiceProxyService spiceProxyService;
  private final ConsoleUpstreamWebSocketService upstreamWebSocketService;
  private final ExecutorService relayExecutor = Executors.newVirtualThreadPerTaskExecutor();

  public ConsoleWebSocketHandler(
      ConsoleSessionRepository consoleSessionRepository,
      ConsoleProxyProperties properties,
      VncProxyService vncProxyService,
      SpiceProxyService spiceProxyService,
      ConsoleUpstreamWebSocketService upstreamWebSocketService) {
    this.consoleSessionRepository = consoleSessionRepository;
    this.properties = properties;
    this.vncProxyService = vncProxyService;
    this.spiceProxyService = spiceProxyService;
    this.upstreamWebSocketService = upstreamWebSocketService;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    String token = extractToken(session);
    if (token == null || token.isBlank()) {
      session.close(CloseStatus.NOT_ACCEPTABLE.withReason("missing token"));
      return;
    }

    Optional<ConsoleSessionEntity> rowOpt = consoleSessionRepository.findByToken(token);
    if (rowOpt.isEmpty()) {
      session.close(CloseStatus.NOT_ACCEPTABLE.withReason("unknown token"));
      return;
    }

    ConsoleSessionEntity row = rowOpt.get();
    Instant now = Instant.now();
    if (row.getStatus() != ConsoleSessionStatus.ACTIVE || row.getExpiresAt().isBefore(now)) {
      session.close(CloseStatus.NOT_ACCEPTABLE.withReason("session expired"));
      return;
    }

    row.setLastActivityAt(now);
    consoleSessionRepository.save(row);

    if (row.getUpstreamWsUrl() != null && !row.getUpstreamWsUrl().isBlank()) {
      try {
        WebSocket upstreamWs = upstreamWebSocketService.connect(row, properties, session);
        session.getAttributes().put(ATTR_UPSTREAM_WS, upstreamWs);
        session.getAttributes().put(ATTR_SESSION_ROW_ID, row.getId());
        log.info(
            "Console WebSocket established (upstream WS) for vm {} user {}",
            row.getVmId(),
            row.getUserId());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("Upstream WebSocket interrupted: {}", e.getMessage());
        session.close(CloseStatus.SERVER_ERROR.withReason("hypervisor unreachable"));
      } catch (ExecutionException | TimeoutException e) {
        Throwable c = e.getCause() != null ? e.getCause() : e;
        log.warn("Failed upstream Proxmox WebSocket: {}", c.getMessage());
        session.close(CloseStatus.SERVER_ERROR.withReason("hypervisor unreachable"));
      }
      return;
    }

    Socket socket;
    try {
      socket =
          row.getConsoleType() == ConsoleSessionConsoleType.SPICE
              ? spiceProxyService.connect(row, properties)
              : vncProxyService.connect(row, properties);
    } catch (IOException e) {
      log.warn(
          "Failed to connect to hypervisor {}:{} : {}",
          row.getHypervisorHost(),
          row.getHypervisorPort(),
          e.getMessage());
      session.close(CloseStatus.SERVER_ERROR.withReason("hypervisor unreachable"));
      return;
    }

    session.getAttributes().put(ATTR_SOCKET, socket);
    session.getAttributes().put(ATTR_SESSION_ROW_ID, row.getId());

    Future<?> upstream = relayExecutor.submit(() -> pumpHypervisorToBrowser(session, socket));
    session.getAttributes().put(ATTR_UPSTREAM, upstream);

    log.info("Console WebSocket established for vm {} user {}", row.getVmId(), row.getUserId());
  }

  @Override
  protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message)
      throws Exception {
    WebSocket upstreamWs = (WebSocket) session.getAttributes().get(ATTR_UPSTREAM_WS);
    if (upstreamWs != null) {
      ByteBuffer payload = message.getPayload();
      byte[] data = new byte[payload.remaining()];
      payload.get(data);
      upstreamWs
          .sendBinary(ByteBuffer.wrap(data), true)
          .exceptionally(
              ex -> {
                log.debug("Write to upstream WebSocket failed: {}", ex.getMessage());
                try {
                  session.close(CloseStatus.GOING_AWAY);
                } catch (IOException ignored) {
                }
                return null;
              });
      return;
    }

    Socket socket = (Socket) session.getAttributes().get(ATTR_SOCKET);
    if (socket == null || socket.isClosed()) {
      return;
    }
    ByteBuffer payload = message.getPayload();
    byte[] data = new byte[payload.remaining()];
    payload.get(data);
    try {
      OutputStream out = socket.getOutputStream();
      out.write(data);
      out.flush();
    } catch (IOException e) {
      log.debug("Write to hypervisor failed: {}", e.getMessage());
      session.close(CloseStatus.GOING_AWAY);
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    cleanup(session, status);
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    log.debug("WebSocket transport error: {}", exception.getMessage());
    cleanup(session, CloseStatus.SERVER_ERROR);
  }

  private void pumpHypervisorToBrowser(WebSocketSession session, Socket socket) {
    try (InputStream in = socket.getInputStream()) {
      byte[] buf = new byte[65536];
      int r;
      while ((r = in.read(buf)) != -1 && session.isOpen()) {
        synchronized (session) {
          if (!session.isOpen()) {
            break;
          }
          session.sendMessage(new BinaryMessage(ByteBuffer.wrap(buf, 0, r)));
        }
      }
    } catch (Exception e) {
      log.debug("Hypervisor read ended: {}", e.getMessage());
    } finally {
      try {
        if (session.isOpen()) {
          session.close(CloseStatus.NORMAL);
        }
      } catch (IOException ignored) {
      }
    }
  }

  private void cleanup(WebSocketSession wsSession, CloseStatus status) {
    Future<?> upstream = (Future<?>) wsSession.getAttributes().remove(ATTR_UPSTREAM);
    if (upstream != null) {
      upstream.cancel(true);
    }
    WebSocket upstreamWs = (WebSocket) wsSession.getAttributes().remove(ATTR_UPSTREAM_WS);
    if (upstreamWs != null) {
      try {
        upstreamWs.sendClose(WebSocket.NORMAL_CLOSURE, "").get(5, TimeUnit.SECONDS);
      } catch (Exception ignored) {
      }
      upstreamWs.abort();
    }
    Socket socket = (Socket) wsSession.getAttributes().remove(ATTR_SOCKET);
    if (socket != null) {
      try {
        socket.close();
      } catch (IOException ignored) {
      }
    }
    Object id = wsSession.getAttributes().remove(ATTR_SESSION_ROW_ID);
    if (id instanceof java.util.UUID uuid) {
      consoleSessionRepository
          .findById(uuid)
          .ifPresent(
              row -> {
                row.setStatus(ConsoleSessionStatus.CLOSED);
                row.setClosedAt(Instant.now());
                consoleSessionRepository.save(row);
              });
    }
    log.debug("Console WebSocket closed: {}", status);
  }

  private static String extractToken(WebSocketSession session) {
    URI uri = session.getUri();
    if (uri == null || uri.getQuery() == null) {
      return null;
    }
    for (String part : uri.getQuery().split("&")) {
      int i = part.indexOf('=');
      if (i > 0 && "token".equals(part.substring(0, i))) {
        return java.net.URLDecoder.decode(
            part.substring(i + 1), java.nio.charset.StandardCharsets.UTF_8);
      }
    }
    return null;
  }
}
