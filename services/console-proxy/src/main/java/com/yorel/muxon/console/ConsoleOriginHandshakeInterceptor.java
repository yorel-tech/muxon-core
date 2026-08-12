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

import java.util.List;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/** Optional Origin allow-list for browser WebSocket connections. */
public class ConsoleOriginHandshakeInterceptor implements HandshakeInterceptor {

  private final List<String> allowedOrigins;

  public ConsoleOriginHandshakeInterceptor(List<String> allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    if (allowedOrigins == null || allowedOrigins.isEmpty()) {
      return true;
    }
    if (!(request instanceof ServletServerHttpRequest servletRequest)) {
      return true;
    }
    String origin = servletRequest.getServletRequest().getHeader("Origin");
    if (origin == null || origin.isBlank()) {
      // Non-browser clients may omit Origin
      return true;
    }
    return allowedOrigins.stream()
        .anyMatch(allowed -> allowed.equalsIgnoreCase(origin) || "*".equals(allowed));
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {}
}
