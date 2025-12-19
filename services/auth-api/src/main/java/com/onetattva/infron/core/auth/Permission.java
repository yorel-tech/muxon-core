package com.onetattva.infron.core.auth;

public enum Permission {
    // System-level permissions
    SYSTEM_SETTINGS("system:settings"),

    PROVIDER_READ("provider:read"),
    PROVIDER_EDIT("provider:edit"),
    PROVIDER_MANAGE("provider:manage"),

    USER_READ("user:read"),
    USER_EDIT("user:edit"),
    USER_MANAGE("user:manage"),

    TENANT_READ("tenant:read"),
    TENANT_EDIT("tenant:edit"),
    TENANT_MANAGE("tenant:manage"),

    DATACENTER_READ("datacenter:read"),
    DATACENTER_EDIT("datacenter:edit"),
    DATACENTER_MANAGE("datacenter:manage"),

    // Tenant-level permissions
    TENANT_SETTINGS("tenant:settings"),

    VM_READ("vm:read"),
    VM_EDIT("vm:edit"),
    VM_MANAGE("vm:manage"),
    VM_CONSOLE("vm:console");

    private final String action;

    Permission(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public static Permission fromAction(String action) {
        for (Permission permission : values()) {
            if (permission.action.equals(action)) {
                return permission;
            }
        }
        return null;
    }
}
