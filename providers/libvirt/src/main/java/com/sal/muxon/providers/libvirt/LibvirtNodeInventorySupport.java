package com.sal.muxon.providers.libvirt;

import com.sal.muxon.db.model.NodeEntity;
import org.libvirt.Connect;
import org.libvirt.NodeInfo;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Collects node hardware metrics from libvirt for inventory sync (orchestrator calls this to avoid a direct libvirt dependency there).
 */
public final class LibvirtNodeInventorySupport {

    public record Hardware(int cpuTotal, int memMb) {
    }

    private LibvirtNodeInventorySupport() {
    }

    public static String connectionUriForNode(NodeEntity node) {
        Map<String, String> credentials = node.getCredentials();
        if (credentials == null) {
            throw new IllegalArgumentException("Node credentials are required");
        }
        String uri = credentials.get("uri");
        if (uri != null && !uri.isBlank()) {
            return uri.trim();
        }
        String host = credentials.get("host");
        String user = credentials.get("user");
        if (host == null || user == null) {
            throw new IllegalArgumentException(
                    "Node credentials must contain either 'uri' or both 'host' and 'user' for Libvirt SSH");
        }
        return String.format(Locale.ROOT, "qemu+ssh://%s@%s/system", user, host);
    }

    /**
     * @return empty if libvirt is unreachable or node info cannot be read
     */
    public static Optional<Hardware> probe(String uri, Map<String, String> credentials) {
        Map<String, String> creds = credentials != null ? credentials : Map.of();
        LibvirtConnectionManager mgr = new LibvirtConnectionManager(uri, creds);
        try {
            Connect conn = mgr.getConnection();
            NodeInfo ni = conn.nodeInfo();
            int cpus = ni.maxCpus() > 0 ? ni.maxCpus() : ni.cpus;
            long memMbLong = ni.memory > 0 ? ni.memory / 1024L : 0L;
            int memMb = memMbLong > 0 ? (int) Math.min(Integer.MAX_VALUE, memMbLong) : 0;
            return Optional.of(new Hardware(Math.max(0, cpus), memMb));
        } catch (Exception e) {
            return Optional.empty();
        } finally {
            mgr.closeAll();
        }
    }
}
