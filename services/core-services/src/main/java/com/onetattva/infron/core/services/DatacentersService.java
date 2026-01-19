package com.onetattva.infron.core.services;

import com.onetattva.infron.api.*;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.db.model.DatacenterEntity;
import java.util.Map;
import com.onetattva.infron.db.repository.DatacenterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class DatacentersService {

    @Autowired
    private DatacenterRepository datacenterRepository;

    public Datacenter createDatacenter(DatacenterCreate datacenterCreate) {
        DatacenterEntity entity = new DatacenterEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(datacenterCreate.getName());
        entity.setDescription(datacenterCreate.getDescription());
        entity.setSettings(datacenterCreate.getSettings());
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        DatacenterEntity saved = datacenterRepository.save(entity);
        return mapEntityToApi(saved);
    }

    public void deleteDatacenter(UUID datacenterId) {
        datacenterRepository.deleteById(datacenterId);
    }

    public Datacenter getDatacenter(UUID datacenterId) {
        DatacenterEntity entity = datacenterRepository.findById(datacenterId)
                .orElseThrow(() -> new RuntimeException("Datacenter not found"));
        return mapEntityToApi(entity);
    }

    public DatacenterSettings getDatacenterSettings(UUID datacenterId) {
        DatacenterEntity entity = datacenterRepository.findById(datacenterId)
                .orElseThrow(() -> new RuntimeException("Datacenter not found"));
        // TODO: Parse settings JSON and return proper DatacenterSettings object
        return new DatacenterSettings();
    }

    public DatacenterList listDatacenters(Integer page, Integer perPage, DatacenterType providerType) {
        Pageable pageable = PageRequest.of(page - 1, perPage);
        Page<DatacenterEntity> entityPage = datacenterRepository.findAll(pageable);

        List<Datacenter> apiDatacenters = entityPage.getContent().stream()
                .map(this::mapEntityToApi)
                .toList();

        DatacenterList datacenterList = new DatacenterList();
        datacenterList.setTotal((int) entityPage.getTotalElements());
        datacenterList.setPage(page);
        datacenterList.setPerPage(perPage);
        datacenterList.setItems(apiDatacenters);
        return datacenterList;
    }

    public Datacenter replaceDatacenter(UUID datacenterId, DatacenterUpdate datacenterUpdate) {
        DatacenterEntity entity = datacenterRepository.findById(datacenterId)
                .orElseThrow(() -> new RuntimeException("Datacenter not found"));
        entity.setName(datacenterUpdate.getName());
        entity.setDescription(datacenterUpdate.getDescription());
        entity.setSettings(datacenterUpdate.getSettings());
        // TODO: Handle capacity and settings updates
        entity.setUpdatedAt(Instant.now());
        DatacenterEntity saved = datacenterRepository.save(entity);
        return mapEntityToApi(saved);
    }

    public DatacenterSettings replaceDatacenterSettings(UUID datacenterId, DatacenterSettings datacenterSettings) {
        DatacenterEntity entity = datacenterRepository.findById(datacenterId)
                .orElseThrow(() -> new RuntimeException("Datacenter not found"));
        // TODO: Serialize settings to JSON and save
        entity.setUpdatedAt(Instant.now());
        datacenterRepository.save(entity);
        return datacenterSettings;
    }

    public Datacenter updateDatacenter(UUID datacenterId, DatacenterUpdate datacenterUpdate) {
        DatacenterEntity entity = datacenterRepository.findById(datacenterId)
                .orElseThrow(() -> new RuntimeException("Datacenter not found"));
        if (datacenterUpdate.getName() != null) {
            entity.setName(datacenterUpdate.getName());
        }
        if (datacenterUpdate.getDescription() != null) {
            entity.setDescription(datacenterUpdate.getDescription());
        }
        entity.setSettings(datacenterUpdate.getSettings());
        // TODO: Handle partial capacity and settings updates
        entity.setUpdatedAt(Instant.now());
        DatacenterEntity saved = datacenterRepository.save(entity);
        return mapEntityToApi(saved);
    }

    public DatacenterSettings updateDatacenterSettings(UUID datacenterId, DatacenterSettings datacenterSettings) {
        DatacenterEntity entity = datacenterRepository.findById(datacenterId)
                .orElseThrow(() -> new RuntimeException("Datacenter not found"));
        // TODO: Merge settings JSON and save
        entity.setUpdatedAt(Instant.now());
        datacenterRepository.save(entity);
        return datacenterSettings;
    }

    private Datacenter mapEntityToApi(DatacenterEntity entity) {
        Datacenter api = new Datacenter();
        api.setId(entity.getId());
        api.setName(entity.getName());
        api.setDescription(entity.getDescription());
        // Convert DB DatacenterCapacity to API DatacenterCapacity
        if (entity.getCapacity() != null) {
            DatacenterCapacity apiCapacity = new DatacenterCapacity();
            // Note: Would need proper mapping from DB capacity to API capacity
            api.setCapacity(apiCapacity);
        }
        // Convert DB DatacenterSettings to API DatacenterSettings
        api.setSettings(entity.getSettings());
        if (entity.getCreatedAt() != null) {
            api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getUpdatedAt() != null) {
            api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }
        return api;
    }
}
