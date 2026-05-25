package com.sal.muxon.console;

import com.sal.muxon.db.model.ConsoleSessionEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Opens a client WebSocket to Proxmox {@code .../vncwebsocket} (cookie or API-token auth) and relays binary
 * frames to/from the browser session managed by {@link ConsoleWebSocketHandler}.
 */
@Component
public class ConsoleUpstreamWebSocketService {

    private static final int HANDSHAKE_TIMEOUT_SEC = 20;

    public WebSocket connect(ConsoleSessionEntity row, ConsoleProxyProperties properties, WebSocketSession browserSession)
            throws ExecutionException, InterruptedException, TimeoutException {
        String url = row.getUpstreamWsUrl();
        URI uri = URI.create(url);
        HttpClient client = httpClient(properties);

        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                webSocket.request(1);
            }

            @Override
            public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                byte[] buf = new byte[data.remaining()];
                data.get(buf);
                synchronized (browserSession) {
                    try {
                        if (browserSession.isOpen()) {
                            browserSession.sendMessage(new BinaryMessage(buf));
                        }
                    } catch (Exception e) {
                        webSocket.abort();
                    }
                }
                webSocket.request(1);
                return null;
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                try {
                    if (browserSession.isOpen()) {
                        browserSession.close();
                    }
                } catch (Exception ignored) {
                }
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                try {
                    if (browserSession.isOpen()) {
                        browserSession.close();
                    }
                } catch (Exception ignored) {
                }
                return null;
            }
        };

        var builder = client.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(HANDSHAKE_TIMEOUT_SEC))
                .header("Origin", originMatchingPveUi(uri));
        if (row.getUpstreamWsCookie() != null && !row.getUpstreamWsCookie().isBlank()) {
            builder.header("Cookie", row.getUpstreamWsCookie().trim());
        }
        if (row.getUpstreamWsCsrf() != null && !row.getUpstreamWsCsrf().isBlank()) {
            builder.header("CSRFPreventionToken", row.getUpstreamWsCsrf().trim());
        }
        if (row.getUpstreamWsAuthorization() != null && !row.getUpstreamWsAuthorization().isBlank()) {
            builder.header("Authorization", row.getUpstreamWsAuthorization().trim());
        }

        CompletableFuture<WebSocket> future = builder.buildAsync(uri, listener);
        return future.get(HANDSHAKE_TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    /**
     * Proxmox expects an {@code Origin} header matching the cluster UI ({@code https://host:port}).
     */
    private static String originMatchingPveUi(URI wsUri) {
        String host = wsUri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("upstream WebSocket URL has no host");
        }
        int port = wsUri.getPort();
        boolean secure = "wss".equalsIgnoreCase(wsUri.getScheme());
        String httpScheme = secure ? "https" : "http";
        if (port < 0) {
            port = secure ? 443 : 80;
        }
        return httpScheme + "://" + host + ":" + port;
    }

    private static HttpClient httpClient(ConsoleProxyProperties properties) {
        HttpClient.Builder b = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(HANDSHAKE_TIMEOUT_SEC));
        if (properties.isTrustAllHypervisorTls()) {
            b.sslContext(insecureSslContext());
        }
        return b.build();
    }

    private static SSLContext insecureSslContext() {
        try {
            TrustManager[] trust = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trust, new SecureRandom());
            return ctx;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("TLS setup failed", e);
        }
    }
}
