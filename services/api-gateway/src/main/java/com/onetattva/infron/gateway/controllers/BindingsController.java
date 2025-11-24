package com.onetattva.infron.gateway.controllers;

import com.onetattva.infron.api.BindingsApi;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.services.BindingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class BindingsController implements BindingsApi {

    @Autowired
    private BindingsService bindingsService;

    @Override
    public ResponseEntity<RoleBindingList> bulkCreateRoleBindings(RoleBindingBulkCreate roleBindingBulkCreate) {
        RoleBindingList response = bindingsService.bulkCreateRoleBindings(roleBindingBulkCreate);
        return ResponseEntity.status(201).body(response);
    }

    @Override
    public ResponseEntity<RoleBindingList> createRoleBindings(UUID roleId, RoleBindingBulkCreate createRoleBindingsRequest) {
        RoleBindingList response = bindingsService.createRoleBindings(roleId, createRoleBindingsRequest);
        return ResponseEntity.status(201).body(response);
    }

    @Override
    public ResponseEntity<RoleBindingList> createTenantRoleBindings(UUID tenantId, UUID roleId, RoleBindingBulkCreate createRoleBindingsRequest) {
        RoleBindingList response = bindingsService.createTenantRoleBindings(tenantId, roleId, createRoleBindingsRequest);
        return ResponseEntity.status(201).body(response);
    }

    @Override
    public ResponseEntity<RoleBindingList> listRoleBindings(UUID roleId) {
        RoleBindingList result = bindingsService.listRoleBindings(roleId);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<RoleBindingList> listTenantRoleBindings(UUID tenantId, UUID roleId) {
        RoleBindingList result = bindingsService.listTenantRoleBindings(tenantId, roleId);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<RoleBindingList> listUserBindings(UUID userId) {
        RoleBindingList response = bindingsService.listUserBindings(userId);
        return ResponseEntity.ok(response);
    }
}
