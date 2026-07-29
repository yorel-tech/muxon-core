package com.yorel.muxon.api.dto;

import com.yorel.muxon.api.enums.BootstrapStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for bootstrap status response.
 * Returns only the bootstrap status enum, not detailed configuration data.
 * This is used by the public /status endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BootstrapStatusDto {

    /**
     * Current bootstrap status indicating the system initialization state.
     * NOTREADY - No bootstrap configuration found
     * BOOTSTRAPPED - Bootstrap data has been inserted (IDP, system admin, tenant)
     * READY - All required setup steps have been completed
     */
    private BootstrapStatus systemStatus;

    /**
     * Creates a BootstrapStatusDto with the specified system status.
     *
     * @param systemStatus the bootstrap status
     * @return a new BootstrapStatusDto instance
     */
    public static BootstrapStatusDto of(BootstrapStatus systemStatus) {
        return new BootstrapStatusDto(systemStatus);
    }
}
