package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.StorageClassesApi;
import com.onetattva.infron.api.model.StorageClass;
import com.onetattva.infron.api.model.StorageClassCreate;
import com.onetattva.infron.api.model.StorageClassList;
import com.onetattva.infron.api.model.StorageClassOverrides;
import com.onetattva.infron.api.model.StorageClassUpdate;
import com.onetattva.infron.core.services.storage.StorageClassesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StorageClassController implements StorageClassesApi {

    private final StorageClassesService storageClassesService;

    public StorageClassController(StorageClassesService storageClassesService) {
        this.storageClassesService = storageClassesService;
    }

    @Override
    public ResponseEntity<StorageClass> createStorageClass(StorageClassCreate storageClassCreate) {
        StorageClass created = storageClassesService.createStorageClass(storageClassCreate);
        return ResponseEntity.status(201).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteStorageClass(String name) {
        storageClassesService.deleteStorageClass(name);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<StorageClass> getStorageClass(String name) {
        return ResponseEntity.ok(storageClassesService.getStorageClass(name));
    }

    @Override
    public ResponseEntity<StorageClassOverrides> getStorageClassOverrides(String name) {
        return ResponseEntity.ok(storageClassesService.getStorageClassOverrides(name));
    }

    @Override
    public ResponseEntity<StorageClassList> listStorageClasses(Integer page, Integer perPage) {
        return ResponseEntity.ok(storageClassesService.listStorageClasses(page, perPage));
    }

    @Override
    public ResponseEntity<StorageClass> replaceStorageClass(String name, StorageClassUpdate storageClassUpdate) {
        return ResponseEntity.ok(storageClassesService.replaceStorageClass(name, storageClassUpdate));
    }

    @Override
    public ResponseEntity<StorageClassOverrides> replaceStorageClassOverrides(
            String name, StorageClassOverrides storageClassOverrides) {
        return ResponseEntity.ok(storageClassesService.replaceStorageClassOverrides(name, storageClassOverrides));
    }

    @Override
    public ResponseEntity<StorageClass> updateStorageClass(String name, StorageClassUpdate storageClassUpdate) {
        return ResponseEntity.ok(storageClassesService.updateStorageClass(name, storageClassUpdate));
    }
}
