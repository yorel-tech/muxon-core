package com.yorel.muxon.controllers;

import com.yorel.muxon.api.RolesApi;
import com.yorel.muxon.api.model.Role;
import com.yorel.muxon.api.model.RoleList;
import com.yorel.muxon.auth.RoleRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class RolesController implements RolesApi {

    private final RoleRegistry roleRegistry;

    public RolesController(RoleRegistry roleRegistry) {
        this.roleRegistry = roleRegistry;
    }

    @Override
    public ResponseEntity<RoleList> listRoles() {
        RoleList result = new RoleList();
        result.setItems(roleRegistry.getRoles().stream().map(this::mapRole).toList());
        result.setTotal(result.getItems() == null ? 0 : result.getItems().size());
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<RoleList> listTenantRoles(UUID tenantId) {
        RoleList result = new RoleList();
        // OSS: tenant-visible roles are the built-in registry roles (tenant_global + any system roles
        // the platform chooses to expose in UI). For now we return the full registry list and let UI filter.
        result.setItems(roleRegistry.getRoles().stream().map(this::mapRole).toList());
        result.setTotal(result.getItems() == null ? 0 : result.getItems().size());
        return ResponseEntity.ok(result);
    }

    private Role mapRole(RoleRegistry.Role r) {
        Role api = new Role();
        api.setId(r.id());
        api.setName(r.name());
        api.setDescription(r.description());
        api.setScopeType(r.scopeType());
        api.setImmutable(r.immutable());
        return api;
    }
}
