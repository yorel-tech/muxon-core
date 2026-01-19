package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.BindingsApi;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.auth.Permission;
import com.onetattva.infron.core.auth.RequiresPermission;
import com.onetattva.infron.core.services.BindingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@RestController
public class BindingsController implements BindingsApi {

    @Autowired
    private BindingsService bindingsService;

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_MANAGE)
    public ResponseEntity<RoleBindingList> bulkCreateRoleBindings(@Valid @RequestBody RoleBindingBulkCreate roleBindingBulkCreate) {
        RoleBindingList response = bindingsService.bulkCreateRoleBindings(roleBindingBulkCreate);
        return ResponseEntity.status(201).body(response);
    }

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_MANAGE)
    public ResponseEntity<RoleBindingList> createRoleBindings(@NotNull @PathVariable("roleId") UUID roleId, @Valid @RequestBody RoleBindingBulkCreate createRoleBindingsRequest) {
        RoleBindingList response = bindingsService.createRoleBindings(roleId, createRoleBindingsRequest);
        return ResponseEntity.status(201).body(response);
    }

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_MANAGE)
    public ResponseEntity<RoleBindingList> createTenantRoleBindings(@NotNull @PathVariable("tenantId") UUID tenantId, @NotNull @PathVariable("roleId") UUID roleId, @Valid @RequestBody RoleBindingBulkCreate createRoleBindingsRequest) {
        RoleBindingList response = bindingsService.createTenantRoleBindings(tenantId, roleId, createRoleBindingsRequest);
        return ResponseEntity.status(201).body(response);
    }

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_READ)
    public ResponseEntity<RoleBindingList> listRoleBindings(@NotNull @PathVariable("roleId") UUID roleId) {
        RoleBindingList result = bindingsService.listRoleBindings(roleId);
        return ResponseEntity.ok(result);
    }

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_READ)
    public ResponseEntity<RoleBindingList> listTenantRoleBindings(@NotNull @PathVariable("tenantId") UUID tenantId, @NotNull @PathVariable("roleId") UUID roleId) {
        RoleBindingList result = bindingsService.listTenantRoleBindings(tenantId, roleId);
        return ResponseEntity.ok(result);
    }

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_READ)
    public ResponseEntity<RoleBindingList> listUserBindings(@NotNull @PathVariable("userId") UUID userId) {
        RoleBindingList response = bindingsService.listUserBindings(userId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_READ)
    public ResponseEntity<RoleBinding> getRoleBinding(@NotNull @PathVariable("bindingId") UUID bindingId) {
        RoleBinding response = bindingsService.getRoleBinding(bindingId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_EDIT)
    public ResponseEntity<RoleBinding> updateRoleBinding(@NotNull @PathVariable("bindingId") UUID bindingId, @Valid @RequestBody RoleBindingUpdate roleBindingUpdate) {
        RoleBinding response = bindingsService.updateRoleBinding(bindingId, roleBindingUpdate);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.ROLE_BINDING_MANAGE)
    public ResponseEntity<Void> deleteRoleBinding(@NotNull @PathVariable("bindingId") UUID bindingId) {
        bindingsService.deleteRoleBinding(bindingId);
        return ResponseEntity.noContent().build();
    }
}
