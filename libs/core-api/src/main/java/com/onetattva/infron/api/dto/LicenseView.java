package com.onetattva.infron.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * View model for exposing high-level license information via /api/v1/info.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LicenseView {

    /**
     * License type or edition label, e.g. "core", "enterprise".
     */
    private String type;

    /**
     * Optional expiration date in ISO-8601 format (e.g. 2027-12-01).
     */
    private String expires;

    /**
     * Optional limits or quotas associated with the license,
     * such as node counts or cluster limits.
     */
    private Map<String, Object> limits;
}

