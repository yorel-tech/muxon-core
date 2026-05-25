package com.sal.muxon.console;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class ConsoleWebSocketConfig implements WebSocketConfigurer {

    private final ConsoleWebSocketHandler consoleWebSocketHandler;
    private final ConsoleProxyProperties properties;

    public ConsoleWebSocketConfig(ConsoleWebSocketHandler consoleWebSocketHandler,
                                  ConsoleProxyProperties properties) {
        this.consoleWebSocketHandler = consoleWebSocketHandler;
        this.properties = properties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        var origins = properties.getAllowedOrigins();
        String[] patterns = origins == null || origins.isEmpty()
                ? new String[]{"*"}
                : origins.toArray(String[]::new);
        registry.addHandler(consoleWebSocketHandler, "/ws/console")
                .addInterceptors(new ConsoleOriginHandshakeInterceptor(origins))
                .setAllowedOriginPatterns(patterns);
    }
}
