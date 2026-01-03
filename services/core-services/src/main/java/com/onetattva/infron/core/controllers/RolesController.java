package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.RolesApi;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.api.model.Role;
import com.onetattva.infron.api.model.RoleCreate;
import com.onetattva.infron.api.model.RoleList;
import com.onetattva.infron.api.model.RoleUpdate;
import com.onetattva.infron.core.services.RolesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class RolesController implements RolesApi {

    @Autowired
    private RolesService rolesService;

    @Override
    public ResponseEntity<RolePermissionList> addRolePermissions(UUID roleId, RolePermissionsUpdate rolePermissionsUpdate) {
        RolePermissionList result = rolesService.addRolePermissions(roleId, rolePermissionsUpdate);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<RolePermissionList> addTenantRolePermissions(UUID tenantId, UUID roleId, RolePermissionsUpdate rolePermissionsUpdate) {
        RolePermissionList result = rolesService.addTenantRolePermissions(tenantId, roleId, rolePermissionsUpdate);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<Role> createRole(RoleCreate roleCreate) {
        Role role = rolesService.createRole(roleCreate);
        return ResponseEntity.status(201).body(role);
    }

    @Override
    public ResponseEntity<Role> createTenantRole(UUID tenantId, RoleCreate roleCreate) {
        Role role = rolesService.createTenantRole(tenantId, roleCreate);
        return ResponseEntity.status(201).body(role);
    }

    @Override
    public ResponseEntity<Void> deleteRole(UUID roleId) {
        rolesService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteTenantRole(UUID tenantId, UUID roleId) {
        rolesService.deleteTenantRole(tenantId, roleId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Role> getRole(UUID roleId) {
        Role role = rolesService.getRole(roleId);
        return ResponseEntity.ok(role);
    }

    @Override
    public ResponseEntity<Role> getTenantRole(UUID tenantId, UUID roleId) {
        Role role = rolesService.getTenantRole(tenantId, roleId);
        return ResponseEntity.ok(role);
    }

    @Override
    public ResponseEntity<RolePermissionList> listRolePermissions(UUID roleId, Integer page, Integer perPage) {
        RolePermissionList result = rolesService.listRolePermissions(roleId, page, perPage);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<RoleList> listRoles(String scopeType, UUID scopeId, Integer page, Integer perPage) {
        RoleList roleList = rolesService.listRoles(scopeType, scopeId, page, perPage);
        return ResponseEntity.ok(roleList);
    }

    @Override
    public ResponseEntity<RolePermissionList> listTenantRolePermissions(UUID tenantId, UUID roleId, Integer page, Integer perPage) {
        RolePermissionList result = rolesService.listTenantRolePermissions(tenantId, roleId, page, perPage);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<RoleList> listTenantRoles(UUID tenantId, Integer page, Integer perPage) {
        RoleList roleList = rolesService.listTenantRoles(tenantId, page, perPage);
        return ResponseEntity.ok(roleList);
    }

    @Override
    public ResponseEntity<RolePermissionList> removeRolePermissions(UUID roleId, RolePermissionsUpdate rolePermissionsUpdate) {
        RolePermissionList result = rolesService.removeRolePermissions(roleId, rolePermissionsUpdate);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<RolePermissionList> removeTenantRolePermissions(UUID tenantId, UUID roleId, RolePermissionsUpdate rolePermissionsUpdate) {
        RolePermissionList result = rolesService.removeTenantRolePermissions(tenantId, roleId, rolePermissionsUpdate);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<Role> updateRole(UUID roleId, RoleUpdate roleUpdate) {
        Role role = rolesService.updateRole(roleId, roleUpdate);
        return ResponseEntity.ok(role);
    }

    @Override
    public ResponseEntity<Role> updateTenantRole(UUID tenantId, UUID roleId, RoleUpdate roleUpdate) {
        Role role = rolesService.updateTenantRole(tenantId, roleId, roleUpdate);
        return ResponseEntity.ok(role);
    }
}
