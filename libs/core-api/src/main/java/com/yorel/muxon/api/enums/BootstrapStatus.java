package com.yorel.muxon.api.enums;

/**
 * Enum representing the three-state bootstrap lifecycle.
 * 
 * <p>The lifecycle progresses as follows:</p>
 * <ul>
 *   <li>NOTREADY - Initial state, no bootstrap configuration found</li>
 *   <li>BOOTSTRAPPED - Bootstrap data has been inserted (IDP, system admin, tenant)</li>
 *   <li>READY - All required setup steps have been completed via the setup wizard</li>
 * </ul>
 */
public enum BootstrapStatus {
    /**
     * Initial state - no bootstrap configuration has been performed.
     * The system is waiting for bootstrap initialization.
     */
    NOTREADY,

    /**
     * Bootstrap has been performed via muxon-initializer.
     * IDP (with is_system=true), system admin users, and tenant have been created.
     * Setup wizard can now proceed to complete remaining steps.
     */
    BOOTSTRAPPED,

    /**
     * All required setup steps have been completed.
     * The system is ready for normal operation.
     * The "skip to dashboard" button is enabled.
     */
    READY
}
