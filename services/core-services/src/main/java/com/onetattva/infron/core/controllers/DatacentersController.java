package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.DatacentersApi;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.services.DatacentersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.lang.Nullable;
import java.util.UUID;

@RestController
public class DatacentersController implements DatacentersApi {

    @Autowired
    private DatacentersService datacentersService;

    @Override
    public ResponseEntity<Datacenter> createDatacenter(@Valid @RequestBody DatacenterCreate datacenterCreate) {
        Datacenter datacenter = datacentersService.createDatacenter(datacenterCreate);
        return ResponseEntity.status(201).body(datacenter);
    }

    @Override
    public ResponseEntity<Void> deleteDatacenter(@NotNull @PathVariable("datacenterId") UUID datacenterId) {
        datacentersService.deleteDatacenter(datacenterId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Datacenter> getDatacenter(@NotNull @PathVariable("datacenterId") UUID datacenterId) {
        Datacenter datacenter = datacentersService.getDatacenter(datacenterId);
        return ResponseEntity.ok(datacenter);
    }

    @Override
    public ResponseEntity<DatacenterSettings> getDatacenterSettings(@NotNull @PathVariable("datacenterId") UUID datacenterId) {
        DatacenterSettings settings = datacentersService.getDatacenterSettings(datacenterId);
        return ResponseEntity.ok(settings);
    }

    @Override
    public ResponseEntity<DatacenterList> listDatacenters(
        @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @RequestParam(value = "perPage", required = false, defaultValue = "20") Integer perPage,
        @RequestParam(value = "providerType", required = false) @Nullable DatacenterType providerType
    ) {
        DatacenterList datacenterList = datacentersService.listDatacenters(page, perPage, providerType);
        return ResponseEntity.ok(datacenterList);
    }

    @Override
    public ResponseEntity<Datacenter> replaceDatacenter(
        @NotNull @PathVariable("datacenterId") UUID datacenterId,
        @Valid @RequestBody DatacenterUpdate datacenterUpdate
    ) {
        Datacenter datacenter = datacentersService.replaceDatacenter(datacenterId, datacenterUpdate);
        return ResponseEntity.ok(datacenter);
    }

    @Override
    public ResponseEntity<DatacenterSettings> replaceDatacenterSettings(
        @NotNull @PathVariable("datacenterId") UUID datacenterId,
        @Valid @RequestBody DatacenterSettings datacenterSettings
    ) {
        DatacenterSettings settings = datacentersService.replaceDatacenterSettings(datacenterId, datacenterSettings);
        return ResponseEntity.ok(settings);
    }

    @Override
    public ResponseEntity<Datacenter> updateDatacenter(
        @NotNull @PathVariable("datacenterId") UUID datacenterId,
        @Valid @RequestBody DatacenterUpdate datacenterUpdate
    ) {
        Datacenter datacenter = datacentersService.updateDatacenter(datacenterId, datacenterUpdate);
        return ResponseEntity.ok(datacenter);
    }

    @Override
    public ResponseEntity<DatacenterSettings> updateDatacenterSettings(
        @NotNull @PathVariable("datacenterId") UUID datacenterId,
        @Valid @RequestBody DatacenterSettings datacenterSettings
    ) {
        DatacenterSettings settings = datacentersService.updateDatacenterSettings(datacenterId, datacenterSettings);
        return ResponseEntity.ok(settings);
    }
}
