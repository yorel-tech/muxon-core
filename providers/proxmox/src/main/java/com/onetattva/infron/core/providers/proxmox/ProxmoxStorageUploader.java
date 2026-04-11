package com.onetattva.infron.core.providers.proxmox;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.net.ssl.HttpsURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Uploads files to Proxmox storage via {@code POST /api2/json/nodes/{node}/storage/{storage}/upload}
 * using ticket authentication (username/password/realm).
 */
public final class ProxmoxStorageUploader {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_FILENAME_LEN = 255;
    private static final Duration HTTP_TIMEOUT = Duration.ofMinutes(30);
    private static final int UPLOAD_COPY_BUFFER = 64 * 1024;
    private static final SSLContext INSECURE_SSL = buildInsecureSslContext();

    /** Ticket cookie + CSRF token returned by {@code /access/ticket}. */
    public record PveAuthSession(String pveAuthCookie, String csrfToken) {}

    private final HttpClient httpClient;

    public ProxmoxStorageUploader() {
        this.httpClient = buildInsecureHttpClient();
    }

    /**
     * Maps a leaf filename to Proxmox upload {@code content} parameter (multipart field {@code content}).
     * <p>Use {@link #resolveProxmoxContentTypeForItem(String, String)} in replication paths: stored paths
     * from {@link com.onetattva.infron.core.common.ContentLibraryProviderPaths#filenameForStorage} may omit
     * the real extension when the catalog item name already contains a dot (e.g. {@code ubuntu-22.04} with
     * catalog type {@code iso} is stored without {@code .iso}).
     */
    public static String resolveProxmoxContentType(String leafFilename) {
        if (leafFilename == null || leafFilename.isBlank()) {
            return "snippets";
        }
        String lower = leafFilename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".iso")) {
            return "iso";
        }
        if (lower.endsWith(".tar.gz") || lower.endsWith(".tar.zst") || lower.endsWith(".tar.xz")) {
            return "vztmpl";
        }
        if (lower.endsWith(".qcow2") || lower.endsWith(".ova")) {
            return "import";
        }
        return "snippets";
    }

    /**
     * Maps Infron catalog {@code contentType} (see content library API enum) to Proxmox's multipart
     * {@code content} value, falling back to {@link #resolveProxmoxContentType(String)} when the catalog
     * value is unknown.
     */
    public static String resolveProxmoxContentTypeForItem(String catalogContentType, String leafFilename) {
        if (catalogContentType != null && !catalogContentType.isBlank()) {
            String mapped = mapCatalogToProxmoxContent(catalogContentType.trim().toLowerCase(Locale.ROOT));
            if (mapped != null) {
                return mapped;
            }
        }
        return resolveProxmoxContentType(leafFilename);
    }

    private static String mapCatalogToProxmoxContent(String catalog) {
        return switch (catalog) {
            case "iso" -> "iso";
            case "container_image" -> "vztmpl";
            case "vm_template" -> "import";
            case "script", "helm_chart" -> "snippets";
            default -> null;
        };
    }

    /**
     * Builds {@code infron-{lib8}-{item8}-{original}} and truncates to {@value #MAX_FILENAME_LEN} chars.
     */
    public static String encodeInfronUploadFilename(java.util.UUID libraryId, java.util.UUID itemId, String originalLeaf) {
        String lib = libraryId.toString().replace("-", "").substring(0, 8);
        String item = itemId.toString().replace("-", "").substring(0, 8);
        String safeOriginal = originalLeaf == null || originalLeaf.isBlank() ? "artifact" : originalLeaf;
        String base = "infron-" + lib + "-" + item + "-" + safeOriginal;
        if (base.length() <= MAX_FILENAME_LEN) {
            return base;
        }
        return base.substring(0, MAX_FILENAME_LEN);
    }

    /**
     * Proxmox UI / API WebSocket origin for the cluster (used as {@code Origin} on {@code vncwebsocket}).
     */
    public static String pveWebUiOrigin(String clusterEndpoint) {
        return Endpoint.parse(clusterEndpoint).webUiOrigin();
    }

    /**
     * {@code wss|ws://host:port/api2/json/nodes/{node}/qemu/{vmid}/vncwebsocket?port=...&vncticket=...}
     */
    public static String buildQemuVncWebSocketUrl(
            String clusterEndpoint, String nodeName, int vmid, int vncPort, String vncticket) {
        Objects.requireNonNull(clusterEndpoint, "clusterEndpoint");
        Objects.requireNonNull(nodeName, "nodeName");
        Objects.requireNonNull(vncticket, "vncticket");
        Endpoint ep = Endpoint.parse(clusterEndpoint);
        String scheme = ep.https ? "wss" : "ws";
        String encNode = URLEncoder.encode(nodeName, StandardCharsets.UTF_8);
        String encTicket = URLEncoder.encode(vncticket, StandardCharsets.UTF_8);
        return scheme + "://" + ep.host + ":" + ep.port + "/api2/json/nodes/" + encNode + "/qemu/" + vmid
                + "/vncwebsocket?port=" + vncPort + "&vncticket=" + encTicket;
    }

    public PveAuthSession authenticate(String endpoint, Map<String, String> credentials) throws IOException {
        Objects.requireNonNull(endpoint, "endpoint");
        Objects.requireNonNull(credentials, "credentials");
        Endpoint ep = Endpoint.parse(endpoint);
        String username = credentials.get("username");
        String password = credentials.get("password");
        String realm = credentials.getOrDefault("realm", "pam");
        if (username != null && username.contains("@")) {
            int at = username.indexOf('@');
            realm = username.substring(at + 1);
            username = username.substring(0, at);
        }
        if (username == null || password == null) {
            throw new IOException("Proxmox credentials require username and password for ticket auth");
        }
        String form = "username=" + urlEncode(username + "@" + realm)
                + "&password=" + urlEncode(password);
        String url = ep.baseApi() + "/access/ticket";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .version(HttpClient.Version.HTTP_1_1)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Proxmox ticket failed HTTP " + response.statusCode() + ": " + response.body());
            }
            JsonNode root = JSON.readTree(response.body());
            JsonNode data = root.get("data");
            if (data == null || !data.isObject()) {
                throw new IOException("Proxmox ticket response missing data: " + response.body());
            }
            String ticket = textOrNull(data.get("ticket"));
            String csrf = textOrNull(data.get("CSRFPreventionToken"));
            if (ticket == null || csrf == null) {
                throw new IOException("Proxmox ticket response missing ticket or CSRFPreventionToken");
            }
            return new PveAuthSession(ticket, csrf);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Proxmox ticket interrupted", e);
        }
    }

    /**
     * Uploads a local file; returns the task UPID string.
     */
    /**
     * @param catalogContentType Infron catalog {@code contentType} for multipart file {@code Content-Type}
     *                           when the upload filename lacks a recognizable extension; may be {@code null}.
     */
    public String upload(
            String endpoint,
            PveAuthSession session,
            String nodeName,
            String storageId,
            String proxmoxContent,
            String uploadFilename,
            Path localFile,
            String catalogContentType) throws IOException {
        Objects.requireNonNull(localFile, "localFile");
        if (!Files.isRegularFile(localFile)) {
            throw new IOException("Not a regular file: " + localFile);
        }
        Endpoint ep = Endpoint.parse(endpoint);
        String boundary = "----InfronBoundary" + System.currentTimeMillis();
        String url = ep.baseApi() + "/nodes/" + urlPathSegment(nodeName) + "/storage/"
                + urlPathSegment(storageId) + "/upload";

        byte[] part1 = buildMultipartPreamble(boundary, proxmoxContent, uploadFilename, catalogContentType);
        byte[] closing = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        long fileSize = Files.size(localFile);
        long totalLen = Math.addExact(Math.addExact(part1.length, fileSize), closing.length);

        HttpURLConnection conn = openUploadConnection(ep, url);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout((int) Math.min(HTTP_TIMEOUT.toMillis(), Integer.MAX_VALUE));
        // Match Proxmox web UI (Firefox): fixed Content-Length, keep-alive, same-origin hints.
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setRequestProperty("User-Agent", "Infron/ProxmoxStorageUploader");
        conn.setRequestProperty("Origin", ep.webUiOrigin());
        conn.setRequestProperty("Referer", ep.webUiOrigin() + "/");
        conn.setRequestProperty("Cookie", "PVEAuthCookie=" + session.pveAuthCookie());
        conn.setRequestProperty("CSRFPreventionToken", session.csrfToken());
        conn.setFixedLengthStreamingMode(totalLen);

        try (OutputStream out = conn.getOutputStream()) {
            out.write(part1);
            try (InputStream fileIn = Files.newInputStream(localFile.toAbsolutePath())) {
                copyStream(fileIn, out, UPLOAD_COPY_BUFFER);
            }
            out.write(closing);
        } catch (IOException e) {
            throw new IOException(
                    "Proxmox closed the connection while uploading (file ~" + fileSize + " bytes). "
                            + "On the node check /var/log/pveproxy/access.log (403, disk, tmp), "
                            + "free space on /var/tmp, and storage permissions. Original: " + e.getMessage(),
                    e);
        }

        int status = conn.getResponseCode();
        String responseBody = readHttpUrlConnectionBody(conn, status);
        conn.disconnect();

        if (status < 200 || status >= 300) {
            throw new IOException(
                    "Proxmox upload failed HTTP " + status + " (uploadFilename=" + uploadFilename
                            + ", localFile=" + localFile.toAbsolutePath()
                            + ", proxmoxContent=" + proxmoxContent + "): " + responseBody);
        }
        JsonNode root = JSON.readTree(responseBody);
        JsonNode data = root.get("data");
        if (data == null) {
            throw new IOException("Proxmox upload response missing data: " + responseBody);
        }
        String upid = data.isTextual() ? data.asText() : data.toString().replace("\"", "");
        if (upid == null || upid.isBlank()) {
            throw new IOException("Proxmox upload response missing UPID: " + responseBody);
        }
        return upid;
    }

    public void waitForTask(String endpoint, PveAuthSession session, String nodeName, String upid, int maxWaitSeconds)
            throws IOException {
        Endpoint ep = Endpoint.parse(endpoint);
        String encodedUpid = urlPathSegment(upid);
        long deadline = System.nanoTime() + Duration.ofSeconds(Math.max(1, maxWaitSeconds)).toNanos();
        while (System.nanoTime() < deadline) {
            String url = ep.baseApi() + "/nodes/" + urlPathSegment(nodeName) + "/tasks/" + encodedUpid + "/status";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Cookie", "PVEAuthCookie=" + session.pveAuthCookie())
                    .GET()
                    .build();
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IOException("Proxmox task status HTTP " + response.statusCode() + ": " + response.body());
                }
                JsonNode root = JSON.readTree(response.body());
                JsonNode data = root.get("data");
                if (data == null || !data.isObject()) {
                    Thread.sleep(2000);
                    continue;
                }
                String status = textOrNull(data.get("status"));
                if ("stopped".equalsIgnoreCase(status)) {
                    String exitStatus = textOrNull(data.get("exitstatus"));
                    if (exitStatus != null && !"OK".equalsIgnoreCase(exitStatus)) {
                        throw new IOException("Proxmox task failed: exitstatus=" + exitStatus + " body=" + response.body());
                    }
                    return;
                }
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Proxmox task wait interrupted", e);
            }
        }
        throw new IOException("Proxmox task timed out after " + maxWaitSeconds + "s: " + upid);
    }

    /**
     * Multipart preamble for {@code POST .../storage/.../upload}. Matches Proxmox web UI: {@code content}
     * text field first, then file part {@code name="filename"; filename="..."} with a concrete
     * {@code Content-Type} (e.g. ISOs use {@code application/x-cd-image}, not generic octet-stream).
     */
    private static byte[] buildMultipartPreamble(
            String boundary, String content, String filename, String catalogContentType) {
        String fn = filename == null ? "file" : filename;
        String fileContentType = multipartFileContentType(fn, catalogContentType);
        String part = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"content\"\r\n\r\n"
                + content + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"filename\"; filename=\"" + fn + "\"\r\n"
                + "Content-Type: " + fileContentType + "\r\n\r\n";
        return part.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Declared MIME for the file part body. Uses the upload filename when the extension is known; if not,
     * falls back to {@code catalogContentType} (Infron catalog) so mis-pathed artifacts still get a sane type.
     */
    private static String multipartFileContentType(String uploadFilename, String catalogContentType) {
        String fromLeaf = multipartFileContentTypeFromLeaf(uploadFilename);
        if (!"application/octet-stream".equals(fromLeaf)) {
            return fromLeaf;
        }
        if (catalogContentType == null || catalogContentType.isBlank()) {
            return fromLeaf;
        }
        return switch (catalogContentType.trim().toLowerCase(Locale.ROOT)) {
            case "iso" -> "application/x-cd-image";
            case "vm_template" -> "application/x-qemu-disk";
            case "container_image" -> "application/gzip";
            case "script" -> "text/plain";
            case "helm_chart" -> "application/gzip";
            default -> fromLeaf;
        };
    }

    private static String multipartFileContentTypeFromLeaf(String uploadFilename) {
        if (uploadFilename == null || uploadFilename.isBlank()) {
            return "application/octet-stream";
        }
        String lower = uploadFilename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".iso")) {
            return "application/x-cd-image";
        }
        if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) {
            return "application/gzip";
        }
        if (lower.endsWith(".tar.xz")) {
            return "application/x-xz";
        }
        if (lower.endsWith(".tar.zst")) {
            return "application/zstd";
        }
        if (lower.endsWith(".tar")) {
            return "application/x-tar";
        }
        if (lower.endsWith(".qcow2")) {
            return "application/x-qemu-disk";
        }
        if (lower.endsWith(".ova")) {
            return "application/x-tar";
        }
        return "application/octet-stream";
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** Encode a single URL path segment (e.g. node name, storage id, UPID). */
    private static String urlPathSegment(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String textOrNull(JsonNode n) {
        return n == null || n.isNull() ? null : n.asText();
    }

    private static HttpURLConnection openUploadConnection(Endpoint ep, String urlStr) throws IOException {
        var url = URI.create(urlStr).toURL();
        if (ep.https()) {
            HttpsURLConnection c = (HttpsURLConnection) url.openConnection();
            c.setSSLSocketFactory(INSECURE_SSL.getSocketFactory());
            c.setHostnameVerifier((hostname, session) -> true);
            return c;
        }
        return (HttpURLConnection) url.openConnection();
    }

    private static void copyStream(InputStream in, OutputStream out, int bufferSize) throws IOException {
        byte[] buf = new byte[bufferSize];
        int n;
        while ((n = in.read(buf)) >= 0) {
            if (n > 0) {
                out.write(buf, 0, n);
            }
        }
    }

    private static String readHttpUrlConnectionBody(HttpURLConnection conn, int status) throws IOException {
        InputStream raw = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (raw == null) {
            return "";
        }
        try (raw) {
            return new String(raw.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static SSLContext buildInsecureSslContext() {
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
            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(null, trust, new SecureRandom());
            return ssl;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to build insecure SSLContext", e);
        }
    }

    private static HttpClient buildInsecureHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(30))
                .sslContext(INSECURE_SSL)
                .build();
    }

    private record Endpoint(String host, int port, boolean https) {
        static Endpoint parse(String endpoint) {
            String e = endpoint.trim();
            if (!e.contains("://")) {
                e = "https://" + e;
            }
            URI uri = URI.create(e);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("Invalid Proxmox endpoint: " + endpoint);
            }
            int port = uri.getPort();
            if (port < 0) {
                port = 8006;
            }
            boolean https = "https".equalsIgnoreCase(uri.getScheme());
            return new Endpoint(host, port, https);
        }

        String baseApi() {
            String scheme = https ? "https" : "http";
            return scheme + "://" + host + ":" + port + "/api2/json";
        }

        /** {@code https://host:port} — same as the PVE UI Origin. */
        String webUiOrigin() {
            String scheme = https ? "https" : "http";
            return scheme + "://" + host + ":" + port;
        }
    }
}
