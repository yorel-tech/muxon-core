package com.onetattva.infron.core.auth;

import com.onetattva.infron.core.common.UuidUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public enum Permission {
    // System-level permissions
    SYSTEM_SETTINGS("system:settings", "Update system settings", Scope.SYSTEM),

    SYSTEM_USER_READ("system:user:read", "Read system user", Scope.SYSTEM),
    SYSTEM_USER_EDIT("system:user:edit", "Edit system user", Scope.SYSTEM),
    SYSTEM_USER_MANAGE("system:user:manage", "Manage system user", Scope.SYSTEM),

    PROVIDER_READ("provider:read", "Read provider", Scope.SYSTEM),
    PROVIDER_EDIT("provider:edit", "Edit provider", Scope.SYSTEM),
    PROVIDER_MANAGE("provider:manage", "Manage provider", Scope.SYSTEM),

    USER_READ("user:read", "Read user", Scope.TENANT),
    USER_EDIT("user:edit", "Edit user", Scope.TENANT),
    USER_MANAGE("user:manage", "Manage user", Scope.TENANT),

    TENANT_READ("tenant:read", "Read tenant", Scope.SYSTEM),
    TENANT_EDIT("tenant:edit", "Edit tenant", Scope.SYSTEM),
    TENANT_MANAGE("tenant:manage", "Manage tenant", Scope.SYSTEM),

    DATACENTER_READ("datacenter:read", "Read datacenter", Scope.SYSTEM),
    DATACENTER_EDIT("datacenter:edit", "Edit datacenter", Scope.SYSTEM),
    DATACENTER_MANAGE("datacenter:manage", "Manage datacenter", Scope.SYSTEM),

    // Role binding permissions
    ROLE_BINDING_READ("role_binding:read", "Read role bindings", Scope.SYSTEM),
    ROLE_BINDING_EDIT("role_binding:edit", "Edit role bindings", Scope.SYSTEM),
    ROLE_BINDING_MANAGE("role_binding:manage", "Manage role bindings", Scope.SYSTEM),

    // Tenant-level permissions
    TENANT_SETTINGS("tenant:settings", "Update tenant settings", Scope.TENANT),

    // VM permissions
    VM_READ("vm:read", "Read vm", Scope.TENANT),
    VM_EDIT("vm:edit", "Edit vm", Scope.TENANT),
    VM_MANAGE("vm:manage", "Manage vm", Scope.TENANT),
    VM_CONSOLE("vm:console", "View vm console", Scope.TENANT),
    VM_DELETE("vm:delete", "Delete vm", Scope.TENANT),

    // Compute profile permissions
    COMPUTE_PROFILE_CREATE("compute_profile:create", "Create compute profile", Scope.TENANT),
    COMPUTE_PROFILE_READ("compute_profile:read", "Read compute profile", Scope.TENANT),
    COMPUTE_PROFILE_EDIT("compute_profile:edit", "Edit compute profile", Scope.TENANT),
    COMPUTE_PROFILE_MANAGE("compute_profile:manage", "Manage compute profile", Scope.TENANT);

    private static final UUID NAMESPACE = UUID.fromString("696e6672-6f6e-636f-7265-111111111111");
    private static final Map<UUID, Permission> PERMISSION_BY_ID = new HashMap<>();

    private final String action;
    private final String description;
    private final Scope scope;
    private final UUID id;

    static {
        for (Permission permission : values()) {
            PERMISSION_BY_ID.put(permission.id, permission);
        }
    }

    Permission(String action, String description, Scope scope) {
        this.action = action;
        this.description = description;
        this.scope = scope;
        this.id = generateDeterministicId(action);
    }

    public String getAction() {
        return action;
    }

    public String getDescription() {
        return description;
    }

    public Scope getScope() {
        return scope;
    }

    public UUID getId() {
        return id;
    }

    public static Permission fromAction(String action) {
        for (Permission permission : values()) {
            if (permission.action.equals(action)) {
                return permission;
            }
        }
        return null;
    }

    public static Permission fromId(UUID id) {
        return PERMISSION_BY_ID.get(id);
    }

    private static UUID generateDeterministicId(String action) {
        // Use UUID v5 to generate deterministic UUID from action string
        // This matches the database migration logic
        return UuidUtils.generateUuid5(NAMESPACE, action);
    }

    public enum Scope {
        SYSTEM,
        TENANT,
        TENANT_GLOBAL
    }
}
