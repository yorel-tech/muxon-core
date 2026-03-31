package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.VmManagementApi;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.services.VmsService;
import com.onetattva.infron.core.auth.RequiresPermission;
import com.onetattva.infron.core.auth.Permission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class VmsController implements VmManagementApi {

    @Autowired
    private VmsService vmsService;

    @Override
    @RequiresPermission(Permission.VM_MANAGE)
    public ResponseEntity<VmCreateResponse> createVm(UUID tenantId, VmCreateRequest request) {
        VmCreateResponse response = vmsService.createVm(tenantId, request);
        return ResponseEntity.status(202).body(response);
    }

    @Override
    @RequiresPermission(Permission.VM_READ)
    public ResponseEntity<VmListResponse> listVms(UUID tenantId, Integer page, Integer perPage, VmStatus status,
                                                UUID tenantDatacenterGrantId, String tags, String sort, String order) {
        VmListResponse result = vmsService.listVms(tenantId, page, perPage, status, tenantDatacenterGrantId, tags, sort, order);
        return ResponseEntity.ok(result);
    }

    @Override
    @RequiresPermission(Permission.VM_READ)
    public ResponseEntity<Vm> getVm(UUID tenantId, UUID vmId) {
        Vm vm = vmsService.getVm(tenantId, vmId);
        return ResponseEntity.ok(vm);
    }

    @Override
    @RequiresPermission(Permission.VM_EDIT)
    public ResponseEntity<Vm> patchVm(UUID tenantId, UUID vmId, VmUpdateRequest request) {
        Vm vm = vmsService.patchVm(tenantId, vmId, request);
        return ResponseEntity.ok(vm);
    }

    @Override
    @RequiresPermission(Permission.VM_MANAGE)
    public ResponseEntity<VmOperationResponse> deleteVm(UUID tenantId, UUID vmId) {
        VmOperationResponse response = vmsService.deleteVm(tenantId, vmId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.VM_MANAGE)
    public ResponseEntity<VmOperationResponse> startVm(UUID tenantId, UUID vmId) {
        VmOperationResponse response = vmsService.startVm(tenantId, vmId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.VM_MANAGE)
    public ResponseEntity<VmOperationResponse> stopVm(UUID tenantId, UUID vmId) {
        VmOperationResponse response = vmsService.stopVm(tenantId, vmId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.VM_MANAGE)
    public ResponseEntity<VmOperationResponse> restartVm(UUID tenantId, UUID vmId) {
        VmOperationResponse response = vmsService.restartVm(tenantId, vmId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.VM_MANAGE)
    public ResponseEntity<VmOperationResponse> suspendVm(UUID tenantId, UUID vmId) {
        VmOperationResponse response = vmsService.suspendVm(tenantId, vmId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.VM_MANAGE)
    public ResponseEntity<VmOperationResponse> resumeVm(UUID tenantId, UUID vmId) {
        VmOperationResponse response = vmsService.resumeVm(tenantId, vmId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.VM_CONSOLE)
    public ResponseEntity<VmConsoleResponse> getVmConsole(UUID tenantId, UUID vmId) {
        VmConsoleResponse response = vmsService.getVmConsole(tenantId, vmId);
        return ResponseEntity.ok(response);
    }
}
