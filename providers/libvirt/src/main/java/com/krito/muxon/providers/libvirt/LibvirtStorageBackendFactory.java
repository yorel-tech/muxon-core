package com.krito.muxon.providers.libvirt;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating Libvirt storage backend instances.
 * <p>
 * Manages backend instances for different storage types (Ceph RBD, LVM, ZFS)
 * and provides a unified interface for backend selection.
 * </p>
 */
public class LibvirtStorageBackendFactory {

    private final Map<String, LibvirtStorageBackend> backends;

    public LibvirtStorageBackendFactory() {
        this.backends = new HashMap<>();
        this.backends.put("ceph-rbd", new LibvirtCephRbdBackend());
        this.backends.put("lvm", new LibvirtLvmBackend());
        this.backends.put("zfs", new LibvirtZfsBackend());
    }

    /**
     * Get a storage backend by type.
     *
     * @param backendType backend type (e.g., "ceph-rbd", "lvm", "zfs")
     * @return storage backend implementation
     * @throws IllegalArgumentException if backend type is not supported
     */
    public LibvirtStorageBackend getBackend(String backendType) {
        LibvirtStorageBackend backend = backends.get(backendType);
        if (backend == null) {
            throw new IllegalArgumentException("Unsupported backend type: " + backendType);
        }
        return backend;
    }

    /**
     * Check if a backend type is supported.
     *
     * @param backendType backend type to check
     * @return true if supported
     */
    public boolean isSupported(String backendType) {
        return backends.containsKey(backendType);
    }
}
