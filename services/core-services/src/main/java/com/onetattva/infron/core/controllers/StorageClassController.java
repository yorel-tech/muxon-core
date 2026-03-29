package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.StorageClassesApi;
import com.onetattva.infron.api.model.ProviderStorageList;
import com.onetattva.infron.api.model.StorageClass;
import com.onetattva.infron.api.model.StorageClassCreate;
import com.onetattva.infron.api.model.StorageClassList;
import com.onetattva.infron.api.model.StorageClassOverrides;
import com.onetattva.infron.api.model.StorageClassUpdate;
import com.onetattva.infron.core.services.storage.ProviderStorageApiConverter;
import com.onetattva.infron.core.services.storage.ProviderStorageMappingService;
import com.onetattva.infron.core.services.storage.StorageClassesService;
import com.onetattva.infron.db.model.ProviderStorageEntity;
import com.onetattva.infron.db.repository.ProviderStorageRepository;
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
