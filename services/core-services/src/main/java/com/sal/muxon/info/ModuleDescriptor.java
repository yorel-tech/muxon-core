package com.sal.muxon.info;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Describes a backend module or integration that contributes core behavior
 * (e.g. a hypervisor, storage backend, or identity provider).
 *
 * Basic operations provided by a module (such as VM CRUD for compute modules)
 * are considered implicit and are intentionally not exposed as capabilities.
 */
@Data
@Builder
public class ModuleDescriptor {

    /**
     * Stable identifier for the module, e.g. "compute.kvm" or "storage.ceph".
     */
    private String id;

    /**
     * Human-readable name suitable for display in the UI.
     */
    private String displayName;

    /**
     * High-level category such as "compute", "storage", "network", "identity".
     */
    private String category;

    /**
     * Indicates whether the module is built into the product or provided
     * by an external plugin.
     */
    private boolean builtin;

    /**
     * Arbitrary metadata that modules may contribute, such as version,
     * vendor, or configuration hints.
     */
    private Map<String, String> metadata;
}

