package com.krito.muxon.controllers;

import com.krito.muxon.api.UsersApi;
import com.krito.muxon.api.model.OidcUserList;
import com.krito.muxon.api.model.RoleBinding;
import com.krito.muxon.api.model.RoleBindingBulkCreate;
import com.krito.muxon.api.model.RoleBindingList;
import com.krito.muxon.api.model.RoleUpdateRequest;
import com.krito.muxon.api.model.UserList;
import com.krito.muxon.auth.Permission;
import com.krito.muxon.auth.RequiresPermission;
import com.krito.muxon.services.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for user management operations.
 * Handles listing, adding, updating, and deleting users for system and tenant scopes.
 */
@RestController
public class UsersController implements UsersApi {

    @Autowired
    private UsersService usersService;

    @Override
    @RequiresPermission(Permission.SYSTEM_USER_READ)
    public ResponseEntity<UserList> listSystemUsers(
            Integer page,
            Integer perPage,
            String q) {
        UserList result = usersService.listSystemUsers(page, perPage, q);
        return ResponseEntity.ok(result);
    }

    @Override
    @RequiresPermission(Permission.USER_READ)
    public ResponseEntity<UserList> listTenantUsers(
            UUID tenantId,
            Integer page,
            Integer perPage,
            String q) {
        UserList result = usersService.listTenantUsers(tenantId, page, perPage, q);
        return ResponseEntity.ok(result);
    }

    @Override
    @RequiresPermission(Permission.USER_READ)
    public ResponseEntity<OidcUserList> listIdpUsers(
            @PathVariable UUID idpId,
            Integer page,
            Integer perPage,
            String q) {
        OidcUserList result = usersService.listIdpUsers(idpId, page, perPage, q);
        return ResponseEntity.ok(result);
    }

    @Override
    @RequiresPermission(Permission.SYSTEM_USER_MANAGE)
    public ResponseEntity<RoleBindingList> addSystemUsers(@RequestBody RoleBindingBulkCreate roleBindingBulkCreate) {
        RoleBindingList result = usersService.addSystemUsers(roleBindingBulkCreate);
        return ResponseEntity.status(201).body(result);
    }

    @Override
    @RequiresPermission(Permission.USER_MANAGE)
    public ResponseEntity<RoleBindingList> addTenantUsers(
            @PathVariable UUID tenantId,
            @RequestBody RoleBindingBulkCreate roleBindingBulkCreate) {
        RoleBindingList result = usersService.addTenantUsers(tenantId, roleBindingBulkCreate);
        return ResponseEntity.status(201).body(result);
    }

    @Override
    @RequiresPermission(Permission.SYSTEM_USER_MANAGE)
    public ResponseEntity<Void> deleteSystemUser(@PathVariable UUID userId) {
        usersService.deleteSystemUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresPermission(Permission.USER_MANAGE)
    public ResponseEntity<Void> deleteTenantUser(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId) {
        usersService.deleteTenantUser(tenantId, userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresPermission(Permission.SYSTEM_USER_MANAGE)
    public ResponseEntity<RoleBinding> updateSystemUserRole(
            @PathVariable UUID userId,
            @RequestBody RoleUpdateRequest roleUpdateRequest) {
        RoleBinding result = usersService.updateSystemUserRole(userId, roleUpdateRequest);
        return ResponseEntity.ok(result);
    }

    @Override
    @RequiresPermission(Permission.USER_MANAGE)
    public ResponseEntity<RoleBinding> updateTenantUserRole(
            @PathVariable UUID tenantId,
            @PathVariable UUID userId,
            @RequestBody RoleUpdateRequest roleUpdateRequest) {
        RoleBinding result = usersService.updateTenantUserRole(tenantId, userId, roleUpdateRequest);
        return ResponseEntity.ok(result);
    }
}
