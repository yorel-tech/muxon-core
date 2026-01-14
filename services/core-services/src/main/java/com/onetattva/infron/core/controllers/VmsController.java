package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.VmsApi;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.services.VmsService;
import com.onetattva.infron.core.auth.RequiresPermission;
import com.onetattva.infron.core.auth.Permission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class VmsController implements VmsApi {

    @Autowired
    private VmsService vmsService;

    @Override
    @RequiresPermission(Permission.VM_CREATE)
    public ResponseEntity<Vm> createVm(VmCreateRequest request) {
        Vm vm = vmsService.createVm(request);
        return ResponseEntity.status(201).body(vm);
    }

    @Override
    @RequiresPermission(Permission.VM_READ)
    public ResponseEntity<VmList> listVms(Integer page, Integer perPage, String status, 
                                                          UUID tenantDatacenterGrantId, String tags, 
                                                          String sort) {
        VmList result = vmsService.listVms(page, perPage, status, tenantDatacenterGrantId, tags, sort);
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
    public ResponseEntity<Vm> updateVm(UUID vmId, VmUpdateRequest request) {
        Vm vm = vmsService.updateVm(vmId, request);
        return ResponseEntity.ok(vm);
    }

    @Override
    @RequiresPermission(Permission.VM_MANAGE)
    public ResponseEntity<Vm> deleteVm(UUID vmId) {
        vmsService.deleteVm(vmId);
        return ResponseEntity.noContent().build();
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
