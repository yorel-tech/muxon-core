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

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class ConsoleWebSocketConfig implements WebSocketConfigurer {

  private final ConsoleWebSocketHandler consoleWebSocketHandler;
  private final ConsoleProxyProperties properties;

  public ConsoleWebSocketConfig(
      ConsoleWebSocketHandler consoleWebSocketHandler, ConsoleProxyProperties properties) {
    this.consoleWebSocketHandler = consoleWebSocketHandler;
    this.properties = properties;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    var origins = properties.getAllowedOrigins();
    String[] patterns =
        origins == null || origins.isEmpty() ? new String[] {"*"} : origins.toArray(String[]::new);
    registry
        .addHandler(consoleWebSocketHandler, "/ws/console")
        .addInterceptors(new ConsoleOriginHandshakeInterceptor(origins))
        .setAllowedOriginPatterns(patterns);
  }
}
