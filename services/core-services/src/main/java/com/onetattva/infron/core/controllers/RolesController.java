package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.RolesApi;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.services.RolesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.UUID;

@RestController
public class RolesController implements RolesApi {

    @Autowired
    private RolesService rolesService;

    @Override
    public ResponseEntity<RolePermissionList> addRolePermissions(@NotNull @PathVariable("roleId") UUID roleId, @Valid @RequestBody RolePermissionsUpdate rolePermissionsUpdate) {
        RolePermissionList result = rolesService.addRolePermissions(roleId, rolePermissionsUpdate);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<RolePermissionList> addTenantRolePermissions(@NotNull @PathVariable("tenantId") UUID tenantId, @NotNull @PathVariable("roleId") UUID roleId, @Valid @RequestBody RolePermissionsUpdate rolePermissionsUpdate) {
        RolePermissionList result = rolesService.addTenantRolePermissions(tenantId, roleId, rolePermissionsUpdate);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<Role> createRole(@Valid @RequestBody RoleCreate roleCreate) {
        Role role = rolesService.createRole(roleCreate);
        return ResponseEntity.status(201).body(role);
    }

    @Override
    public ResponseEntity<Role> createTenantRole(@NotNull @PathVariable("tenantId") UUID tenantId, @Valid @RequestBody RoleCreate roleCreate) {
        Role role = rolesService.createTenantRole(tenantId, roleCreate);
        return ResponseEntity.status(201).body(role);
    }

    @Override
    public ResponseEntity<Void> deleteRole(@NotNull @PathVariable("roleId") UUID roleId) {
        rolesService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteTenantRole(@NotNull @PathVariable("tenantId") UUID tenantId, @NotNull @PathVariable("roleId") UUID roleId) {
        rolesService.deleteTenantRole(tenantId, roleId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Role> getRole(@NotNull @PathVariable("roleId") UUID roleId) {
        Role role = rolesService.getRole(roleId);
        return ResponseEntity.ok(role);
    }

    @Override
    public ResponseEntity<Role> getTenantRole(@NotNull @PathVariable("tenantId") UUID tenantId, @NotNull @PathVariable("roleId") UUID roleId) {
        Role role = rolesService.getTenantRole(tenantId, roleId);
        return ResponseEntity.ok(role);
    }

    @Override
    public ResponseEntity<RolePermissionList> listRolePermissions(@NotNull @PathVariable("roleId") UUID roleId, @Min(value = 1) @RequestParam(value = "page", required = false, defaultValue = "1") Integer page, @Min(value = 1) @Max(value = 200) @RequestParam(value = "perPage", required = false, defaultValue = "20") Integer perPage) {
        RolePermissionList result = rolesService.listRolePermissions(roleId, page, perPage);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<RoleList> listRoles(@RequestParam(value = "scope_type", required = false) String scopeType, @RequestParam(value = "scope_id", required = false) UUID scopeId, @Min(value = 1) @RequestParam(value = "page", required = false, defaultValue = "1") Integer page, @Min(value = 1) @Max(value = 200) @RequestParam(value = "perPage", required = false, defaultValue = "20") Integer perPage) {
        RoleList roleList = rolesService.listRoles(scopeType, scopeId, page, perPage);
        return ResponseEntity.ok(roleList);
    }

    @Override
    public ResponseEntity<RolePermissionList> listTenantRolePermissions(@NotNull @PathVariable("tenantId") UUID tenantId, @NotNull @PathVariable("roleId") UUID roleId, @Min(value = 1) @RequestParam(value = "page", required = false, defaultValue = "1") Integer page, @Min(value = 1) @Max(value = 200) @RequestParam(value = "perPage", required = false, defaultValue = "20") Integer perPage) {
        RolePermissionList result = rolesService.listTenantRolePermissions(tenantId, roleId, page, perPage);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<RoleList> listTenantRoles(@NotNull @PathVariable("tenantId") UUID tenantId, @Min(value = 1) @RequestParam(value = "page", required = false, defaultValue = "1") Integer page, @Min(value = 1) @Max(value = 200) @RequestParam(value = "perPage", required = false, defaultValue = "20") Integer perPage) {
        RoleList roleList = rolesService.listTenantRoles(tenantId, page, perPage);
        return ResponseEntity.ok(roleList);
    }

    @Override
    public ResponseEntity<RolePermissionList> removeRolePermissions(@NotNull @PathVariable("roleId") UUID roleId, @Valid @RequestBody RolePermissionsUpdate rolePermissionsUpdate) {
        RolePermissionList result = rolesService.removeRolePermissions(roleId, rolePermissionsUpdate);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<RolePermissionList> removeTenantRolePermissions(@NotNull @PathVariable("tenantId") UUID tenantId, @NotNull @PathVariable("roleId") UUID roleId, @Valid @RequestBody RolePermissionsUpdate rolePermissionsUpdate) {
        RolePermissionList result = rolesService.removeTenantRolePermissions(tenantId, roleId, rolePermissionsUpdate);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<Role> updateRole(@NotNull @PathVariable("roleId") UUID roleId, @Valid @RequestBody RoleUpdate roleUpdate) {
        Role role = rolesService.updateRole(roleId, roleUpdate);
        return ResponseEntity.ok(role);
    }

    @Override
    public ResponseEntity<Role> updateTenantRole(@NotNull @PathVariable("tenantId") UUID tenantId, @NotNull @PathVariable("roleId") UUID roleId, @Valid @RequestBody RoleUpdate roleUpdate) {
        Role role = rolesService.updateTenantRole(tenantId, roleId, roleUpdate);
        return ResponseEntity.ok(role);
    }
}
