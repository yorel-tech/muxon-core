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
package com.yorel.muxon.providers.libvirt;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating Libvirt storage backend instances.
 *
 * <p>Manages backend instances for different storage types (Ceph RBD, LVM, ZFS) and provides a
 * unified interface for backend selection.
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
