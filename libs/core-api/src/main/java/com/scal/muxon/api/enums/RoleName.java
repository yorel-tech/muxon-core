package com.scal.muxon.api.enums;

/**
 * Enum representing predefined role names in the system.
 * These are the default roles used for access control.
 */
public enum RoleName {
    /**
     * System administrator role with full system-level access.
     */
    SYSTEM_ADMIN("system:admin"),

    /**
     * Tenant administrator role with full tenant-level access.
     */
    TENANT_ADMIN("tenant:admin"),

    /**
     * Workload operator role with ability to manage workloads.
     */
    WORKLOAD_OPERATOR("workload:operator"),

    /**
     * Workload user role with read-only access to workloads.
     */
    WORKLOAD_USER("workload:user");

    private final String value;

    RoleName(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
