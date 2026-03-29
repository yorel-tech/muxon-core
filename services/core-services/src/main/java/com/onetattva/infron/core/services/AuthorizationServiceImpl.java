package com.onetattva.infron.core.services;

import com.onetattva.infron.core.auth.AuthorizationService;
import com.onetattva.infron.core.auth.Scope;
import com.onetattva.infron.core.auth.UserPrincipal;
import com.onetattva.infron.db.model.UserRoleBindingViewEntity;
import com.onetattva.infron.db.repository.PermissionRepository;
import com.onetattva.infron.db.repository.RolePermissionRepository;
import com.onetattva.infron.db.repository.UserRoleBindingViewRepository;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service("authorizationService")
public class AuthorizationServiceImpl implements AuthorizationService {

    private final PermissionRepository permissionRepository; // your existing repo/logic

    private final UserRoleBindingViewRepository userRoleRepo;
    private final RolePermissionRepository rolePermissionRepo;
    private final Cache<String, List<String>> tenantCache;


    public AuthorizationServiceImpl(PermissionRepository permissionRepository,
                            UserRoleBindingViewRepository tenantUserRepository,
                            RolePermissionRepository rolePermissionRepo,
                            Cache<String, List<String>> tenantCache) {
        this.permissionRepository = permissionRepository;
        this.userRoleRepo = tenantUserRepository;
        this.rolePermissionRepo = rolePermissionRepo;
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
        List<String> tenants = getTenantsForExternalId(externalId);
        return tenants.contains(tenantId);
    }

    @Override
    public void evictCacheForExternalId(String externalId) {
        tenantCache.invalidate(externalId);
    }

    // cache user permissions via Spring Cache with TTL
    @Cacheable(cacheNames = "userPermissions", key = "#user.id")
    public List<String> getCachedPermissions(UserPrincipal user) {
        // Query database for user's role bindings to get role IDs with proper tenant scoping
        List<UserRoleBindingViewEntity> userBindings = userRoleRepo.findByExternalId(user.id());
        if (userBindings.isEmpty()) {
            return List.of();
        }

        // Extract role IDs from user's role bindings
        List<UUID> roleIds = userBindings.stream()
                .map(UserRoleBindingViewEntity::getRoleId)
                .distinct()
                .collect(Collectors.toList());

        // Get permissions for these specific role IDs
        List<String> permissions = rolePermissionRepo.findByRoleIds(roleIds)
                .stream()
                .map(rp -> rp.getPermission().getAction())
                .distinct()
                .collect(Collectors.toList());

        return permissions;
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
        List<String> perms = getPermissionsForTenant(user, tenantId);
        return perms.stream().anyMatch(p -> matchesPermissionPattern(p, action));
    }

    /**
     * Returns permissions for the user limited to role bindings in the given tenant.
     * Does not use global permission cache so that tenant scope is respected.
     */
    private List<String> getPermissionsForTenant(UserPrincipal user, String tenantId) {
        UUID tenantUuid;
        try {
            tenantUuid = UUID.fromString(tenantId);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
        List<UserRoleBindingViewEntity> bindings = userRoleRepo.findByExternalId(user.id());
        List<UUID> roleIds = bindings.stream()
                .filter(b -> "TENANT".equals(b.getScopeType()) && tenantUuid.equals(b.getScopeId()))
                .map(UserRoleBindingViewEntity::getRoleId)
                .distinct()
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return rolePermissionRepo.findByRoleIds(roleIds)
                .stream()
                .map(rp -> rp.getPermission().getAction())
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
