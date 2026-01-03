package com.onetattva.infron.core.services;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.auth.AuthorizationService;
import com.onetattva.infron.core.auth.Permission;
import com.onetattva.infron.db.model.PermissionEntity;
import com.onetattva.infron.db.model.RoleEntity;
import com.onetattva.infron.db.model.RolePermissionEntity;
import com.onetattva.infron.db.repository.PermissionRepository;
import com.onetattva.infron.db.repository.RolePermissionRepository;
import com.onetattva.infron.db.repository.RoleRepository;
import com.onetattva.infron.db.RoleScopeType;
import com.onetattva.infron.db.repository.TenantRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RolesService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private AuthorizationService authorizationService;

    public Role createRole(RoleCreate roleCreate) {
        RoleEntity entity = new RoleEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(roleCreate.getName());
        entity.setDescription(roleCreate.getDescription());
        entity.setScopeType(RoleScopeType.valueOf(roleCreate.getScopeType().toUpperCase()));
        entity.setScopeId(roleCreate.getScopeId());
        entity.setImmutable(roleCreate.getImmutable() != null ? roleCreate.getImmutable() : false);
        entity.setCreatedAt(Instant.now());
        RoleEntity saved = roleRepository.save(entity);
        return mapEntityToApi(saved);
    }

    public Role createTenantRole(UUID tenantId, RoleCreate roleCreate) {
        // Validate tenant exists
        tenantRepository.findById(tenantId).orElseThrow();
        RoleEntity entity = new RoleEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(roleCreate.getName());
        entity.setDescription(roleCreate.getDescription());
        entity.setScopeType(RoleScopeType.TENANT);
        entity.setScopeId(tenantId);
        entity.setImmutable(roleCreate.getImmutable() != null ? roleCreate.getImmutable() : false);
        entity.setCreatedAt(Instant.now());
        RoleEntity saved = roleRepository.save(entity);
        return mapEntityToApi(saved);
    }

    public void deleteRole(UUID roleId) {
        roleRepository.deleteById(roleId);
        authorizationService.evictAllUserPermissions();
    }

    public void deleteTenantRole(UUID tenantId, UUID roleId) {
        RoleEntity role = roleRepository.findById(roleId).orElseThrow();
        if (!role.getScopeType().equals(RoleScopeType.TENANT) || !role.getScopeId().equals(tenantId)) {
            throw new RuntimeException("Role not found in tenant scope");
        }
        roleRepository.delete(role);
        authorizationService.evictAllUserPermissions();
    }

    public Role getRole(UUID roleId) {
        RoleEntity entity = roleRepository.findById(roleId).orElseThrow();
        return mapEntityToApi(entity);
    }

    public Role getTenantRole(UUID tenantId, UUID roleId) {
        RoleEntity entity = roleRepository.findById(roleId).orElseThrow();
        if (!entity.getScopeType().equals(RoleScopeType.TENANT) || !entity.getScopeId().equals(tenantId)) {
            throw new RuntimeException("Role not found in tenant scope");
        }
        return mapEntityToApi(entity);
    }

    public RoleList listRoles(String scopeTypeStr, UUID scopeId, Integer page, Integer perPage) {
        if (page == null) page = 1;
        if (perPage == null) perPage = 20;
        if (perPage < 1) perPage = 1;
        if (perPage > 200) perPage = 200;
        Pageable pageable = PageRequest.of(page - 1, perPage);

        Page<RoleEntity> entities;
        if (scopeTypeStr != null && scopeId != null) {
            entities = roleRepository.findByScopeTypeAndScopeId(RoleScopeType.valueOf(scopeTypeStr.toUpperCase()), scopeId, pageable);
        } else if (scopeTypeStr != null) {
            entities = roleRepository.findByScopeType(RoleScopeType.valueOf(scopeTypeStr.toUpperCase()), pageable);
        } else {
            // TODO: Add findAll with pageable if needed, or restrict to specific scoped roles for performance
            // For now, list all
            entities = roleRepository.findAll(pageable);
        }

        RoleList result = new RoleList();
        result.setTotal((int) entities.getTotalElements());
        result.setPage(page);
        result.setPerPage(perPage);
        result.setItems(entities.getContent().stream().map(this::mapEntityToApi).collect(Collectors.toList()));
        return result;
    }

    public RoleList listTenantRoles(UUID tenantId, Integer page, Integer perPage) {
        if (page == null) page = 1;
        if (perPage == null) perPage = 20;
        if (perPage < 1) perPage = 1;
        if (perPage > 200) perPage = 200;
        Pageable pageable = PageRequest.of(page - 1, perPage);

        Page<RoleEntity> entities = roleRepository.findByScopeTypeIn(Set.of(RoleScopeType.TENANT_GLOBAL, RoleScopeType.TENANT), pageable);

        RoleList result = new RoleList();
        // TODO: Calculate total properly after filtering, but for simplicity, set total as is
        result.setTotal((int) entities.getTotalElements());
        result.setPage(page);
        result.setPerPage(perPage);
        result.setItems(entities.getContent().stream()
                .filter(entity -> entity.getScopeType().equals(RoleScopeType.TENANT_GLOBAL) ||
                                 (entity.getScopeType().equals(RoleScopeType.TENANT) && entity.getScopeId() != null && entity.getScopeId().equals(tenantId)))
                .map(this::mapEntityToApi)
                .collect(Collectors.toList()));
        return result;
    }

    public Role updateRole(UUID roleId, RoleUpdate roleUpdate) {
        RoleEntity entity = roleRepository.findById(roleId).orElseThrow();
        if (roleUpdate.getName() != null) {
            entity.setName(roleUpdate.getName());
        }
        if (roleUpdate.getDescription() != null) {
            entity.setDescription(roleUpdate.getDescription());
        }
        if (roleUpdate.getImmutable() != null) {
            entity.setImmutable(roleUpdate.getImmutable());
        }
        RoleEntity saved = roleRepository.save(entity);
        authorizationService.evictAllUserPermissions();
        return mapEntityToApi(saved);
    }

    public Role updateTenantRole(UUID tenantId, UUID roleId, RoleUpdate roleUpdate) {
        RoleEntity entity = roleRepository.findById(roleId).orElseThrow();
        if (!entity.getScopeType().equals(RoleScopeType.TENANT) || !entity.getScopeId().equals(tenantId)) {
            throw new RuntimeException("Role not found in tenant scope");
        }
        if (roleUpdate != null && roleUpdate.getName() != null) {
            entity.setName(roleUpdate.getName());
        }
        if (roleUpdate != null && roleUpdate.getDescription() != null) {
            entity.setDescription(roleUpdate.getDescription());
        }
        if (roleUpdate != null && roleUpdate.getImmutable() != null) {
            entity.setImmutable(roleUpdate.getImmutable());
        }
        RoleEntity saved = roleRepository.save(entity);
        authorizationService.evictAllUserPermissions();
        return mapEntityToApi(saved);
    }

    public RolePermissionList listRolePermissions(UUID roleId, Integer page, Integer perPage) {
        // Validate role exists
        RoleEntity role = roleRepository.findById(roleId).orElseThrow();

        if (page == null) page = 1;
        if (perPage == null || perPage < 1) perPage = 20;
        if (perPage > 200) perPage = 200;

        // Get all role permissions for this role
        List<UUID> rolePermissions = rolePermissionRepository.findPermissionIdsByRoleIds(List.of(roleId));

        // Extract permission IDs and convert to Permission enum
        List<Permission> permissions = rolePermissions.stream()
                .map(Permission::fromId)
                .filter(java.util.Objects::nonNull) // Filter out nulls in case of unknown permissions
                .toList();

        // Calculate pagination
        int total = permissions.size();
        int startIndex = (page - 1) * perPage;
        int endIndex = Math.min(startIndex + perPage, total);

        // Get paginated permissions
        List<Permission> paginatedPermissions = permissions.subList(Math.min(startIndex, total), endIndex);

        RolePermissionList result = new RolePermissionList();
        result.setTotal(total);
        result.setPage(page);
        result.setPerPage(perPage);
        result.setItems(paginatedPermissions.stream().map(this::mapPermissionEnumToApi).collect(Collectors.toList()));
        return result;
    }

    public RolePermissionList listTenantRolePermissions(UUID tenantId, UUID roleId, Integer page, Integer perPage) {
        // Validate tenant role exists
        RoleEntity role = roleRepository.findById(roleId).orElseThrow();
        if (!role.getScopeType().equals(RoleScopeType.TENANT) || !role.getScopeId().equals(tenantId)) {
            throw new RuntimeException("Role not found in tenant scope");
        }

        // Reuse the same logic as listRolePermissions
        return listRolePermissions(roleId, page, perPage);
    }

    public RolePermissionList addRolePermissions(UUID roleId, RolePermissionsUpdate rolePermissionsUpdate) {
        // Validate role exists
        RoleEntity role = roleRepository.findById(roleId).orElseThrow();

        List<String> permissionActions = rolePermissionsUpdate.getPermissions();
        if (permissionActions == null || permissionActions.isEmpty()) {
            throw new IllegalArgumentException("Permissions list cannot be empty");
        }

        // Convert permission actions to Permission enum and then to entities
        List<PermissionEntity> permissions = permissionActions.stream()
                .map(Permission::fromAction)
                .filter(java.util.Objects::nonNull)
                .map(permEnum -> {
                    PermissionEntity entity = new PermissionEntity();
                    entity.setId(permEnum.getId());
                    entity.setAction(permEnum.getAction());
                    entity.setDescription(permEnum.getDescription());
                    entity.setScope(RoleScopeType.valueOf(permEnum.getScope().name()));
                    return entity;
                })
                .distinct() // Remove duplicates in input
                .toList();

        if (permissions.size() != permissionActions.stream().distinct().count()) {
            throw new IllegalArgumentException("One or more permissions do not exist");
        }

        // Get existing permission IDs for the role
        List<UUID> existingPermissionIds = rolePermissionRepository.findPermissionIdsByRoleIds(List.of(roleId));

        // Filter out permissions already assigned to the role
        List<PermissionEntity> newPermissions = permissions.stream()
                .filter(perm -> !existingPermissionIds.contains(perm.getId()))
                .toList();

        // Create role-permission associations
        List<RolePermissionEntity> rolePermissionsToSave = newPermissions.stream()
                .map(permission -> {
                    RolePermissionEntity rolePermission = new RolePermissionEntity();
                    rolePermission.setId(UUID.randomUUID());
                    rolePermission.setRole(role);
                    rolePermission.setPermission(permission);
                    rolePermission.setCreatedAt(Instant.now());
                    return rolePermission;
                })
                .collect(Collectors.toList());

        // Save all in a single batch
        if (!rolePermissionsToSave.isEmpty()) {
            rolePermissionRepository.saveAll(rolePermissionsToSave);
        }

        // Evict user permissions cache
        authorizationService.evictAllUserPermissions();

        // Return updated list
        return listRolePermissions(roleId, null, null);
    }

    public RolePermissionList addTenantRolePermissions(UUID tenantId, UUID roleId, RolePermissionsUpdate rolePermissionsUpdate) {
        // Validate tenant role exists
        RoleEntity role = roleRepository.findById(roleId).orElseThrow();
        if (!role.getScopeType().equals(RoleScopeType.TENANT) || !role.getScopeId().equals(tenantId)) {
            throw new RuntimeException("Role not found in tenant scope");
        }

        // Reuse the same logic as addRolePermissions
        return addRolePermissions(roleId, rolePermissionsUpdate);
    }

    public RolePermissionList removeRolePermissions(UUID roleId, RolePermissionsUpdate rolePermissionsUpdate) {
        // Validate role exists
        RoleEntity role = roleRepository.findById(roleId).orElseThrow();

        List<String> permissionActions = rolePermissionsUpdate.getPermissions();
        if (permissionActions == null || permissionActions.isEmpty()) {
            throw new IllegalArgumentException("Permissions list cannot be empty");
        }

        // Convert permission actions to IDs
        List<UUID> permissionIds = permissionActions.stream()
                .map(action -> Permission.fromAction(action))
                .filter(java.util.Objects::nonNull)
                .map(Permission::getId)
                .collect(Collectors.toList());

        if (permissionIds.size() != permissionActions.size()) {
            throw new IllegalArgumentException("One or more permissions do not exist");
        }

        // Find and delete role-permission associations
        List<RolePermissionEntity> rolePermissions = rolePermissionRepository.findByRoleIds(List.of(roleId));
        List<RolePermissionEntity> toDelete = rolePermissions.stream()
                .filter(rp -> permissionIds.contains(rp.getPermission().getId()))
                .collect(Collectors.toList());

        rolePermissionRepository.deleteAll(toDelete);

        // Evict user permissions cache
        authorizationService.evictAllUserPermissions();

        // Return updated list
        return listRolePermissions(roleId, null, null);
    }

    public RolePermissionList removeTenantRolePermissions(UUID tenantId, UUID roleId, RolePermissionsUpdate rolePermissionsUpdate) {
        // Validate tenant role exists
        RoleEntity role = roleRepository.findById(roleId).orElseThrow();
        if (!role.getScopeType().equals(RoleScopeType.TENANT) || !role.getScopeId().equals(tenantId)) {
            throw new RuntimeException("Role not found in tenant scope");
        }

        // Reuse the same logic as removeRolePermissions
        return removeRolePermissions(roleId, rolePermissionsUpdate);
    }

    private Role mapEntityToApi(RoleEntity entity) {
        Role role = new Role();
        role.setId(entity.getId());
        role.setName(entity.getName());
        role.setDescription(entity.getDescription());
        role.setScopeType(entity.getScopeType().toString().toLowerCase());
        role.setScopeId(entity.getScopeId());
        role.setImmutable(entity.getImmutable());
        role.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        return role;
    }


    private com.onetattva.infron.api.model.Permission mapPermissionEnumToApi(Permission permissionEnum) {
        com.onetattva.infron.api.model.Permission permission = new com.onetattva.infron.api.model.Permission();
        permission.setId(permissionEnum.getId());
        permission.setAction(permissionEnum.getAction());
        permission.setDescription(permissionEnum.getDescription());
        permission.setScope(permissionEnum.getScope().toString().toLowerCase());
        return permission;
    }
}
