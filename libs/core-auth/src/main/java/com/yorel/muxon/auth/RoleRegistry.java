package com.yorel.muxon.auth;

import com.yorel.muxon.common.UuidUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class RoleRegistry {
    // Keep stable across releases; used only for deterministic role IDs.
    private static final UUID ROLE_NAMESPACE = UUID.fromString("696e6672-6f6e-636f-7265-222222222222");

    public record Role(
            UUID id,
            String name,
            String description,
            String scopeType,
            boolean immutable
    ) {}

    private static final Map<String, RoleDefinition> ROLES_BY_NAME;

    static {
        Map<String, RoleDefinition> m = new LinkedHashMap<>();

        // Mirrors the OSS built-in roles that were previously seeded in V202604150701__rbac.sql.
        m.put("system:admin", new RoleDefinition(
                role("system:admin", "Platform Administrator", "system", true),
                concat(
                        // all SYSTEM-scoped permissions
                        permissionsWithScope(Permission.Scope.SYSTEM),
                        // plus specific cross-scope actions from the legacy seed
                        List.of(
                                Permission.VM_CREATE, Permission.VM_READ, Permission.VM_EDIT, Permission.VM_MANAGE, Permission.VM_CONSOLE, Permission.VM_DELETE,
                                Permission.COMPUTE_PROFILE_CREATE, Permission.COMPUTE_PROFILE_READ, Permission.COMPUTE_PROFILE_EDIT, Permission.COMPUTE_PROFILE_MANAGE,
                                Permission.TENANT_ROLE_READ, Permission.TENANT_ROLE_EDIT, Permission.TENANT_ROLE_MANAGE,
                                Permission.CONTENT_LIBRARY_READ, Permission.CONTENT_LIBRARY_WRITE, Permission.CONTENT_LIBRARY_PUBLISH_TEMPLATE,
                                Permission.TENANT_READ_SETTINGS,
                                Permission.TENANT_DATACENTER_READ,
                                Permission.SYSTEM_USER_READ, Permission.SYSTEM_USER_EDIT, Permission.SYSTEM_USER_MANAGE
                        )
                )
        ));

        m.put("tenant:admin", new RoleDefinition(
                role("tenant:admin", "Tenant Administrator", "tenant_global", true),
                concat(
                        permissionsWithScope(Permission.Scope.TENANT),
                        List.of(
                                Permission.TENANT_DATACENTER_READ,
                                Permission.CONTENT_LIBRARY_READ, Permission.CONTENT_LIBRARY_WRITE, Permission.CONTENT_LIBRARY_PUBLISH_TEMPLATE
                        )
                )
        ));

        m.put("workload:operator", new RoleDefinition(
                role("workload:operator", "Workload Operator", "tenant_global", true),
                List.of(
                        Permission.VM_CREATE, Permission.VM_MANAGE, Permission.VM_EDIT, Permission.VM_READ, Permission.VM_CONSOLE,
                        Permission.COMPUTE_PROFILE_READ, Permission.COMPUTE_PROFILE_EDIT,
                        Permission.CONTENT_LIBRARY_READ
                )
        ));

        m.put("workload:user", new RoleDefinition(
                role("workload:user", "Workload User", "tenant_global", true),
                List.of(
                        Permission.VM_READ, Permission.VM_CONSOLE,
                        Permission.COMPUTE_PROFILE_READ,
                        Permission.CONTENT_LIBRARY_READ
                )
        ));

        ROLES_BY_NAME = Collections.unmodifiableMap(m);
    }

    private record RoleDefinition(Role role, List<Permission> permissions) {}

    private static Role role(String name, String description, String scopeType, boolean immutable) {
        return new Role(
                UuidUtils.generateUuid5(ROLE_NAMESPACE, name),
                name,
                description,
                scopeType,
                immutable
        );
    }

    private static List<Permission> permissionsWithScope(Permission.Scope scope) {
        return Arrays.stream(Permission.values())
                .filter(p -> p.getScope() == scope)
                .collect(Collectors.toList());
    }

    private static List<Permission> concat(List<Permission> a, List<Permission> b) {
        ArrayList<Permission> out = new ArrayList<>(a.size() + b.size());
        out.addAll(a);
        out.addAll(b);
        return out;
    }

    public List<Role> getRoles() {
        return ROLES_BY_NAME.values().stream().map(RoleDefinition::role).toList();
    }

    public Role getRoleByName(String roleName) {
        RoleDefinition def = ROLES_BY_NAME.get(roleName);
        return def == null ? null : def.role();
    }

    public List<Permission> getPermissionsForRole(String roleName) {
        RoleDefinition def = ROLES_BY_NAME.get(roleName);
        return def == null ? List.of() : def.permissions();
    }

    public Map<String, Map<String, Boolean>> getCapabilitiesForRoles(Collection<String> roleNames) {
        Set<String> actions = roleNames.stream()
                .flatMap(rn -> getPermissionsForRole(rn).stream())
                .map(Permission::getAction)
                .collect(Collectors.toSet());

        return capabilitiesFromActions(actions);
    }

    private static boolean any(Set<String> actions, String... required) {
        for (String r : required) {
            if (actions.contains(r)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Map<String, Boolean>> capabilitiesFromActions(Set<String> actions) {
        Map<String, Map<String, Boolean>> out = new LinkedHashMap<>();

        out.put("infra", Map.of(
                "view", any(actions, "node:read", "node_cluster:read", "datacenter:read"),
                "manage", any(actions, "node:manage", "provider:manage", "datacenter:manage")
        ));

        out.put("vm", Map.of(
                "create", actions.contains("vm:create"),
                "start", any(actions, "vm:manage", "vm:edit"),
                "stop", any(actions, "vm:manage", "vm:edit"),
                "delete", actions.contains("vm:delete"),
                "view", actions.contains("vm:read"),
                "consoleAccess", actions.contains("vm:console")
        ));

        // Reserved for future: core currently has no container-specific permissions.
        out.put("container", Map.of(
                "deploy", false,
                "update", false,
                "delete", false,
                "view", false
        ));

        // Reserved / best-effort mapping.
        out.put("network", Map.of(
                "create", false,
                "attach", actions.contains("vm:manage"),
                "detach", actions.contains("vm:manage"),
                "view", actions.contains("vm:read")
        ));

        out.put("storage", Map.of(
                "create", false,
                "attach", actions.contains("vm:manage"),
                "detach", actions.contains("vm:manage"),
                "view", any(actions, "storage_class:read", "provider_storage:read")
        ));

        // Nexus-only today; OSS returns false for these.
        out.put("project", Map.of(
                "view", false,
                "update", false,
                "delete", false,
                "manageMembers", false
        ));

        out.put("orchestration", Map.of(
                "executeTask", any(actions, "vm:create", "vm:manage"),
                "retryTask", any(actions, "vm:create", "vm:manage"),
                "cancelTask", actions.contains("vm:manage"),
                "viewTasks", true
        ));

        return out;
    }
}

