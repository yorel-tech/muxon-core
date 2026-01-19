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
    public ResponseEntity<VmCreateResponse> createVm(VmCreateRequest request) {
        VmCreateResponse response = vmsService.createVm(request);
        return ResponseEntity.status(202).body(response);
    }

    @Override
    @RequiresPermission(Permission.VM_READ)
    public ResponseEntity<VmListResponse> listVms(Integer page, Integer perPage, VmStatus status,
                                                UUID tenantDatacenterGrantId, String tags, String sort, String order) {
        VmListResponse result = vmsService.listVms(page, perPage, status, tenantDatacenterGrantId, tags, sort);
        return ResponseEntity.ok(result);
    }

    @Override
    @RequiresPermission(Permission.VM_READ)
    public ResponseEntity<Vm> getVm(UUID vmId) {
        Vm vm = vmsService.getVm(vmId);
        return ResponseEntity.ok(vm);
    }

    @Override
    @RequiresPermission(Permission.VM_EDIT)
    public ResponseEntity<Vm> patchVm(UUID vmId, VmUpdateRequest request) {
        Vm vm = vmsService.patchVm(vmId, request);
        return ResponseEntity.ok(vm);
    }

    @Override
    @RequiresPermission(Permission.VM_MANAGE)
    public ResponseEntity<VmOperationResponse> deleteVm(UUID vmId) {
        VmOperationResponse response = vmsService.deleteVm(vmId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.VM_MANAGE)
    public ResponseEntity<VmOperationResponse> startVm(UUID vmId) {
        VmOperationResponse response = vmsService.startVm(vmId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.VM_MANAGE)
    public ResponseEntity<VmOperationResponse> stopVm(UUID vmId) {
        VmOperationResponse response = vmsService.stopVm(vmId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.VM_MANAGE)
    public ResponseEntity<VmOperationResponse> restartVm(UUID vmId) {
        VmOperationResponse response = vmsService.restartVm(vmId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.VM_MANAGE)
    public ResponseEntity<VmOperationResponse> suspendVm(UUID vmId) {
        VmOperationResponse response = vmsService.suspendVm(vmId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.VM_MANAGE)
    public ResponseEntity<VmOperationResponse> resumeVm(UUID vmId) {
        VmOperationResponse response = vmsService.resumeVm(vmId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.VM_CONSOLE)
    public ResponseEntity<VmConsoleResponse> getVmConsole(UUID vmId) {
        VmConsoleResponse response = vmsService.getVmConsole(vmId);
        return ResponseEntity.ok(response);
    }
}
