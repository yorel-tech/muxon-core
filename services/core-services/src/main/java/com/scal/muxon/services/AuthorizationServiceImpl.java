package com.scal.muxon.services;

import com.scal.muxon.auth.AuthorizationService;
import com.scal.muxon.auth.Permission;
import com.scal.muxon.auth.RoleRegistry;
import com.scal.muxon.auth.Scope;
import com.scal.muxon.auth.UserPrincipal;
import com.scal.muxon.db.model.UserRoleBindingViewEntity;
import com.scal.muxon.db.repository.UserRoleBindingViewRepository;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service("authorizationService")
public class AuthorizationServiceImpl implements AuthorizationService {

    private final UserRoleBindingViewRepository userRoleRepo;
    private final RoleRegistry roleRegistry;
    private final Cache<String, List<String>> tenantCache;


    public AuthorizationServiceImpl(UserRoleBindingViewRepository tenantUserRepository,
                                    RoleRegistry roleRegistry,
                                    Cache<String, List<String>> tenantCache) {
        this.userRoleRepo = tenantUserRepository;
        this.roleRegistry = roleRegistry;
        this.tenantCache = tenantCache;
    }

    @Override
    public List<String> getTenantsForExternalId(String externalId) {
        return tenantCache.get(externalId, this::loadTenantsForExternalId);
    }

    private List<String> loadTenantsForExternalId(String externalId) {
        List<UserRoleBindingViewEntity> rows = userRoleRepo.findByExternalId(externalId);
        return rows.stream()
                .map(UserRoleBindingViewEntity::getScopeId)
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasAccessToTenant(String externalId, String tenantId) {
        UUID tid;
        try {
            tid = UUID.fromString(tenantId);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return userRoleRepo.findByExternalId(externalId).stream()
                .anyMatch(b -> bindingAppliesToTenant(b, tid));
    }

    /**
     * True if this role binding grants access to APIs under the given tenant:
     * explicit tenant membership, a tenant-global template role, or a system (platform) role.
     */
    private static boolean bindingAppliesToTenant(UserRoleBindingViewEntity b, UUID tenantUuid) {
        String st = b.getScopeType();
        if (st == null) {
            return false;
        }
        return switch (st.toUpperCase(Locale.ROOT)) {
            case "SYSTEM", "TENANT_GLOBAL" -> true;
            case "TENANT" -> tenantUuid.equals(b.getScopeId());
            default -> false;
        };
    }

    @Override
    public void evictCacheForExternalId(String externalId) {
        tenantCache.invalidate(externalId);
    }

    // cache user permissions via Spring Cache with TTL
    @Cacheable(cacheNames = "userPermissions", key = "#user.id")
    public List<String> getCachedPermissions(UserPrincipal user) {
        List<UserRoleBindingViewEntity> userBindings = userRoleRepo.findByExternalId(user.id());
        if (userBindings.isEmpty()) {
            return List.of();
        }

        return userBindings.stream()
                .map(UserRoleBindingViewEntity::getRoleName)
                .filter(Objects::nonNull)
                .flatMap(roleName -> roleRegistry.getPermissionsForRole(roleName).stream())
                .map(Permission::getAction)
                .distinct()
                .collect(Collectors.toList());
    }

    @CacheEvict(cacheNames = "userPermissions", key = "#userId")
    public void evictCacheForUser(String userId) {
        // Spring Cache handles eviction
    }

    @CacheEvict(cacheNames = "userPermissions", allEntries = true)
    public void evictAllUserPermissions() {
        // Spring Cache handles eviction
    }

    @Override
    public boolean isAllowed(UserPrincipal user, String action, Scope resourceScope, Object resource) {
        // fetch cached perms
        List<String> perms = getCachedPermissions(user);

        // check if any permission matches action or has wildcard
        return perms.stream().anyMatch(p -> matchesPermissionPattern(p, action));
    }

    @Override
    public boolean isAllowedForTenant(UserPrincipal user, String action, String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return isAllowed(user, action, Scope.SYSTEM, null);
        }
        List<String> perms = getEffectivePermissionActionsForTenant(user, tenantId);
        return perms.stream().anyMatch(p -> matchesPermissionPattern(p, action));
    }

    /**
     * Permissions effective for requests scoped to a tenant: roles bound to that tenant,
     * plus tenant-global template roles and system (platform) roles.
     */
    private List<String> getEffectivePermissionActionsForTenant(UserPrincipal user, String tenantId) {
        UUID tenantUuid;
        try {
            tenantUuid = UUID.fromString(tenantId);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
        List<UserRoleBindingViewEntity> bindings = userRoleRepo.findByExternalId(user.id());
        List<String> roleNames = bindings.stream()
                .filter(b -> bindingAppliesToTenant(b, tenantUuid))
                .map(UserRoleBindingViewEntity::getRoleName)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (roleNames.isEmpty()) {
            return List.of();
        }
        return roleNames.stream()
                .flatMap(rn -> roleRegistry.getPermissionsForRole(rn).stream())
                .map(Permission::getAction)
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean matchesPermissionPattern(String pattern, String action) {
        // pattern may contain wildcard '*'
        // convert to regex: escape regex chars except *, replace * -> .*
        String escaped = Pattern.quote(pattern).replace("\\*", ".*");
        return Pattern.compile("^" + escaped + "$").matcher(action).matches();
    }
}
