package com.krito.muxon.core.controllers;

import com.krito.muxon.api.DatacentersApi;
import com.krito.muxon.api.model.*;
import com.krito.muxon.core.services.DatacentersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class DatacentersController implements DatacentersApi {

    @Autowired
    private DatacentersService datacentersService;

    @Override
    public ResponseEntity<Datacenter> createDatacenter(DatacenterCreate datacenterCreate) {
        Datacenter datacenter = datacentersService.createDatacenter(datacenterCreate);
        return ResponseEntity.status(201).body(datacenter);
    }

    @Override
    public ResponseEntity<Void> deleteDatacenter(UUID datacenterId) {
        datacentersService.deleteDatacenter(datacenterId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Datacenter> getDatacenter(UUID datacenterId) {
        Datacenter datacenter = datacentersService.getDatacenter(datacenterId);
        return ResponseEntity.ok(datacenter);
    }

    @Override
    public ResponseEntity<DatacenterSettings> getDatacenterSettings(UUID datacenterId) {
        DatacenterSettings settings = datacentersService.getDatacenterSettings(datacenterId);
        return ResponseEntity.ok(settings);
    }

    @Override
    public ResponseEntity<DatacenterList> listDatacenters(Integer page, Integer perPage, ProviderType providerType) {
        DatacenterList datacenterList = datacentersService.listDatacenters(page, perPage, providerType);
        return ResponseEntity.ok(datacenterList);
    }

    @Override
    public ResponseEntity<Datacenter> replaceDatacenter(UUID datacenterId, DatacenterUpdate datacenterUpdate) {
        Datacenter datacenter = datacentersService.replaceDatacenter(datacenterId, datacenterUpdate);
        return ResponseEntity.ok(datacenter);
    }

    @Override
    public ResponseEntity<DatacenterSettings> replaceDatacenterSettings(UUID datacenterId, DatacenterSettings datacenterSettings) {
        DatacenterSettings settings = datacentersService.replaceDatacenterSettings(datacenterId, datacenterSettings);
        return ResponseEntity.ok(settings);
    }

    @Override
    public ResponseEntity<Datacenter> updateDatacenter(UUID datacenterId, DatacenterUpdate datacenterUpdate) {
        Datacenter datacenter = datacentersService.updateDatacenter(datacenterId, datacenterUpdate);
        return ResponseEntity.ok(datacenter);
    }

    @Override
    public ResponseEntity<DatacenterSettings> updateDatacenterSettings(UUID datacenterId, DatacenterSettings datacenterSettings) {
        DatacenterSettings settings = datacentersService.updateDatacenterSettings(datacenterId, datacenterSettings);
        return ResponseEntity.ok(settings);
    }

    //@PutMapping("/datacenters/{datacenterId}/capacity")
    public ResponseEntity<DatacenterCapacity> updateDatacenterCapacity(UUID datacenterId, DatacenterCapacity capacity) {
        DatacenterCapacity updated = datacentersService.updateDatacenterCapacity(datacenterId, capacity);
        return ResponseEntity.ok(updated);
    }

    @Override
    public ResponseEntity<java.util.List<String>> getDatacenterStorageClasses(UUID datacenterId) {
        java.util.List<String> storageClasses = datacentersService.getAvailableStorageClasses(datacenterId);
        return ResponseEntity.ok(storageClasses);
    }

    @Override
    public ResponseEntity<StorageClassValidationResult> validateDatacenterStorageClasses(
            UUID datacenterId, 
            java.util.List<String> requestBody) {
        StorageClassValidationResult result = datacentersService.validateDatacenterStorageClasses(datacenterId, requestBody);
        return ResponseEntity.ok(result);
    }
}
