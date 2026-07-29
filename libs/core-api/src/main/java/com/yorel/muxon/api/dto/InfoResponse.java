package com.yorel.muxon.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Top-level response model for the /api/v1/info endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InfoResponse {

    /**
     * Product name, e.g. "Muxon".
     */
    private String product;

    /**
     * Edition identifier, serialized as a simple string (e.g. "core", "enterprise").
     */
    private String edition;

    /**
     * Product version string.
     */
    private String version;

    /**
     * High-level license summary.
     */
    private LicenseView license;

    /**
     * Unified capability set represented as string identifiers.
     */
    private List<String> capabilities;

    /**
     * List of backend modules currently available (compute, storage, network, etc).
     */
    private List<ModuleView> modules;

    /**
     * Optional additional attributes for convenience flags
     * (e.g. multipleDatacentersPerTenant).
     */
    private Map<String, Object> extras;
}

