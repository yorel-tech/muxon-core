package com.krito.muxon.controllers;

import com.krito.muxon.api.StorageClassesApi;
import com.krito.muxon.api.model.ProviderStorageList;
import com.krito.muxon.api.model.StorageClass;
import com.krito.muxon.api.model.StorageClassCreate;
import com.krito.muxon.api.model.StorageClassList;
import com.krito.muxon.api.model.StorageClassOverrides;
import com.krito.muxon.api.model.StorageClassUpdate;
import com.krito.muxon.services.storage.ProviderStorageApiConverter;
import com.krito.muxon.services.storage.ProviderStorageMappingService;
import com.krito.muxon.services.storage.StorageClassesService;
import com.krito.muxon.db.model.ProviderStorageEntity;
import com.krito.muxon.db.repository.ProviderStorageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class StorageClassController implements StorageClassesApi {

    private final StorageClassesService storageClassesService;
    private final ProviderStorageRepository providerStorageRepository;
    private final ProviderStorageMappingService providerStorageMappingService;
    private final ProviderStorageApiConverter providerStorageApiConverter;

    public StorageClassController(
            StorageClassesService storageClassesService,
            ProviderStorageRepository providerStorageRepository,
            ProviderStorageMappingService providerStorageMappingService,
            ProviderStorageApiConverter providerStorageApiConverter) {
        this.storageClassesService = storageClassesService;
        this.providerStorageRepository = providerStorageRepository;
        this.providerStorageMappingService = providerStorageMappingService;
        this.providerStorageApiConverter = providerStorageApiConverter;
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
    public ResponseEntity<ProviderStorageList> listProviderStorageForStorageClass(
            String name, Integer page, Integer perPage) {
        providerStorageMappingService.requireStorageClassExists(name);
        ProviderStorageMappingService.MappingBatchContext ctx = providerStorageMappingService.loadBatchContext();
        List<ProviderStorageEntity> filtered = providerStorageRepository.findAll().stream()
                .filter(e -> providerStorageMappingService.isMappedToStorageClass(e, name, ctx))
                .collect(Collectors.toList());
        return ResponseEntity.ok(providerStorageApiConverter.toPagedList(filtered, page, perPage, ctx));
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
