package com.yorel.muxon.providers.libvirt;

import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages connections to libvirtd with connection pooling.
 *
 * <p>Connection pooling improves performance by reusing connections
 * and ensures proper cleanup of resources.</p>
 */
public class LibvirtConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(LibvirtConnectionManager.class);

    private final String uri;
    private final Map<String, String> credentials;
    private final Map<String, Connect> connectionPool = new ConcurrentHashMap<>();

    /**
     * Creates a new Libvirt connection manager.
     *
     * @param uri The Libvirt connection URI (e.g., qemu+ssh://user@host/system)
     * @param credentials Connection credentials (SSH key, passphrase, etc.)
     */
    public LibvirtConnectionManager(String uri, Map<String, String> credentials) {
        this.uri = uri;
        this.credentials = credentials;
    }

    /**
     * Gets or creates a connection to libvirtd.
     * Connections are pooled per thread for efficiency.
     *
     * @return A Connect object
     * @throws LibvirtException if connection cannot be established
     */
    public Connect getConnection() throws LibvirtException {
        String key = uri + "-" + Thread.currentThread().getId();

        if (connectionPool.containsKey(key)) {
            Connect conn = connectionPool.get(key);
            if (conn != null && conn.isConnected()) {
                logger.debug("Reusing existing connection for URI: {}", uri);
                return conn;
            }
            // Remove stale connection
            connectionPool.remove(key);
        }

        // Allow custom certificate path via environment variable or URI parameter
        String connectUri = uri;
//        String customCertPath = System.getenv("LIBVIRT_CERT_PATH");
//        if (customCertPath != null && !customCertPath.isEmpty()) {
//            // Append pkipath parameter if not already present
//            if (connectUri.contains("?")) {
//                connectUri += "&pkipath=" + customCertPath;
//            } else {
//                connectUri += "?pkipath=" + customCertPath;
//            }
//            logger.debug("Using custom certificate path: {}", customCertPath);
//        }

        logger.debug("Creating new Libvirt connection for URI: {}", connectUri);
        Connect conn = new Connect(connectUri);
        connectionPool.put(key, conn);
        return conn;
    }

    /**
     * Tests if a connection can be established to libvirtd.
     *
     * @return true if connection is successful, false otherwise
     */
    public boolean testConnection() {
        Connect conn = null;
        try {
            conn = new Connect(uri);
            boolean connected = conn != null && conn.isConnected();
            if (connected) {
                logger.info("Successfully tested connection to Libvirt: {}", uri);
            } else {
                logger.warn("Connection test failed for Libvirt: {}", uri);
            }
            return connected;
        } catch (LibvirtException e) {
            logger.error("Connection test failed for Libvirt {}: {}", uri, e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (LibvirtException e) {
                    logger.warn("Error closing test connection: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Closes all open connections to libvirtd.
     * Should be called when provider is being shut down or reloaded.
     */
    public void closeAll() {
        logger.info("Closing all Libvirt connections ({} connections)", connectionPool.size());

        for (Map.Entry<String, Connect> entry : connectionPool.entrySet()) {
            try {
                Connect conn = entry.getValue();
                if (conn != null && conn.isConnected()) {
                    conn.close();
                    logger.debug("Closed Libvirt connection for URI: {}", uri);
                }
            } catch (LibvirtException e) {
                logger.error("Error closing Libvirt connection for URI: {}: {}",
                        uri, e.getMessage());
            }
        }

        connectionPool.clear();
        logger.info("All Libvirt connections closed");
    }

    /**
     * Gets the number of active connections.
     *
     * @return Number of active connections in the pool
     */
    public int getActiveConnectionCount() throws LibvirtException {
        int count = 0;
        for (Connect conn : connectionPool.values()) {
            if (conn != null && conn.isConnected()) {
                count++;
            }
        }
        return count;
    }
}
