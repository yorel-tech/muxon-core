package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.BindingsApi;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.api.model.RoleBindingBulkCreate;
import com.onetattva.infron.api.model.RoleBindingList;
import com.onetattva.infron.core.auth.Permission;
import com.onetattva.infron.core.auth.RequiresPermission;
import com.onetattva.infron.core.services.BindingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class BindingsController implements BindingsApi {

    @Autowired
    private BindingsService bindingsService;

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_MANAGE)
    public ResponseEntity<RoleBindingList> bulkCreateRoleBindings(RoleBindingBulkCreate roleBindingBulkCreate) {
        RoleBindingList response = bindingsService.bulkCreateRoleBindings(roleBindingBulkCreate);
        return ResponseEntity.status(201).body(response);
    }

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_MANAGE)
    public ResponseEntity<RoleBindingList> createRoleBindings(UUID roleId, RoleBindingBulkCreate createRoleBindingsRequest) {
        RoleBindingList response = bindingsService.createRoleBindings(roleId, createRoleBindingsRequest);
        return ResponseEntity.status(201).body(response);
    }

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_MANAGE)
    public ResponseEntity<RoleBindingList> createTenantRoleBindings(UUID tenantId, UUID roleId, RoleBindingBulkCreate createRoleBindingsRequest) {
        RoleBindingList response = bindingsService.createTenantRoleBindings(tenantId, roleId, createRoleBindingsRequest);
        return ResponseEntity.status(201).body(response);
    }

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_READ)
    public ResponseEntity<RoleBindingList> listRoleBindings(UUID roleId) {
        RoleBindingList result = bindingsService.listRoleBindings(roleId);
        return ResponseEntity.ok(result);
    }

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_READ)
    public ResponseEntity<RoleBindingList> listTenantRoleBindings(UUID tenantId, UUID roleId) {
        RoleBindingList result = bindingsService.listTenantRoleBindings(tenantId, roleId);
        return ResponseEntity.ok(result);
    }

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_READ)
    public ResponseEntity<RoleBindingList> listUserBindings(UUID userId) {
        RoleBindingList response = bindingsService.listUserBindings(userId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_READ)
    public ResponseEntity<RoleBinding> getRoleBinding(UUID bindingId) {
        RoleBinding response = bindingsService.getRoleBinding(bindingId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_EDIT)
    public ResponseEntity<RoleBinding> updateRoleBinding(UUID bindingId, com.onetattva.infron.api.model.RoleBindingUpdate roleBindingUpdate) {
        RoleBinding response = bindingsService.updateRoleBinding(bindingId, roleBindingUpdate);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_MANAGE)
    public ResponseEntity<Void> deleteRoleBinding(UUID bindingId) {
        bindingsService.deleteRoleBinding(bindingId);
        return ResponseEntity.noContent().build();
    }
}
