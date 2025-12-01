package com.onetattva.infron.core.services;

import com.onetattva.infron.api.model.Role;
import com.onetattva.infron.api.model.RoleCreate;
import com.onetattva.infron.api.model.RoleList;
import com.onetattva.infron.api.model.RoleUpdate;
import com.onetattva.infron.db.model.RoleEntity;
import com.onetattva.infron.db.repository.RoleRepository;
import com.onetattva.infron.db.RoleScopeType;
import com.onetattva.infron.db.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RolesService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TenantRepository tenantRepository;

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
    }

    public void deleteTenantRole(UUID tenantId, UUID roleId) {
        RoleEntity role = roleRepository.findById(roleId).orElseThrow();
        if (!role.getScopeType().equals(RoleScopeType.TENANT) || !role.getScopeId().equals(tenantId)) {
            throw new RuntimeException("Role not found in tenant scope");
        }
        roleRepository.delete(role);
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
        return mapEntityToApi(saved);
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
}
