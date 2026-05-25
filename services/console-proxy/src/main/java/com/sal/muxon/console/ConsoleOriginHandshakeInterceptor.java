package com.sal.muxon.console;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * Optional Origin allow-list for browser WebSocket connections.
 */
public class ConsoleOriginHandshakeInterceptor implements HandshakeInterceptor {

    private final List<String> allowedOrigins;

    public ConsoleOriginHandshakeInterceptor(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
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
        return allowedOrigins.stream().anyMatch(allowed ->
                allowed.equalsIgnoreCase(origin) || "*".equals(allowed));
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
