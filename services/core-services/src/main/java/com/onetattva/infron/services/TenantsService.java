package com.onetattva.infron.services;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.db.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TenantsService {

    @Autowired
    private TenantRepository tenantRepository;

    public Tenant createTenant(TenantCreate tenantCreate) {
        com.onetattva.infron.db.model.Tenant entityTenant = new com.onetattva.infron.db.model.Tenant();
        entityTenant.setId(UUID.randomUUID());
        entityTenant.setName(tenantCreate.getName());
        entityTenant.setDisplayName(tenantCreate.getDisplayName());
        entityTenant.setStatus(com.onetattva.infron.db.TenantStatus.ACTIVE);
        try {
            entityTenant.setMetadata(tenantCreate.getMetadata());
        } catch (Exception e) {
            entityTenant.setMetadata(Map.of());
        }
        Instant now = Instant.now();
        entityTenant.setCreatedAt(now);
        entityTenant.setUpdatedAt(now);
        com.onetattva.infron.db.model.Tenant saved = tenantRepository.save(entityTenant);
        return mapEntityToApi(saved);
    }

    public void deleteTenant(UUID tenantId) {
        tenantRepository.deleteById(tenantId);
    }

    public Tenant getTenant(UUID tenantId) {
        com.onetattva.infron.db.model.Tenant entity = tenantRepository.findById(tenantId)
                .orElseThrow();
        return mapEntityToApi(entity);
    }

    public TenantList listTenants(Integer page, Integer perPage, String sort, String name, String status) {
        // TODO: Implement listing with filters and pagination
        List<com.onetattva.infron.db.model.Tenant> entities = tenantRepository.findAll().stream()
                .limit(perPage)
                .toList();
        List<Tenant> apiTenants = entities.stream()
                .map(this::mapEntityToApi)
                .toList();
        TenantList tenantList = new TenantList();
        tenantList.setTotal(apiTenants.size());
        tenantList.setPage(page);
        tenantList.setPerPage(perPage);
        tenantList.setItems(apiTenants);
        return tenantList;
    }

    public Tenant patchTenant(UUID tenantId, TenantUpdate tenantUpdate) {
        com.onetattva.infron.db.model.Tenant entity = tenantRepository.findById(tenantId)
                .orElseThrow();
        if (tenantUpdate.getDisplayName() != null) {
            entity.setDisplayName(tenantUpdate.getDisplayName());
        }
        if (tenantUpdate.getMetadata() != null) {
            try {
                entity.setMetadata(tenantUpdate.getMetadata());
            } catch (Exception e) {
                // keep existing
            }
        }
        entity.setUpdatedAt(Instant.now());
        com.onetattva.infron.db.model.Tenant saved = tenantRepository.save(entity);
        return mapEntityToApi(saved);
    }

    public Tenant updateTenant(UUID tenantId, TenantUpdate tenantUpdate) {
        com.onetattva.infron.db.model.Tenant entity = tenantRepository.findById(tenantId)
                .orElseThrow();
        entity.setDisplayName(tenantUpdate.getDisplayName());
        try {
            entity.setMetadata(tenantUpdate.getMetadata());
        } catch (Exception e) {
            // ignore
        }
        entity.setUpdatedAt(Instant.now());
        com.onetattva.infron.db.model.Tenant saved = tenantRepository.save(entity);
        return mapEntityToApi(saved);
    }

    public TenantSettings getTenantSettings(UUID tenantId) {
        com.onetattva.infron.db.model.Tenant entity = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        // TODO: Parse settings JSON and return proper TenantSettings object
        return new TenantSettings();
    }

    public TenantSettings replaceTenantSettings(UUID tenantId, TenantSettings tenantSettings) {
        com.onetattva.infron.db.model.Tenant entity = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        // TODO: Serialize settings to JSON and save
        entity.setUpdatedAt(Instant.now());
        tenantRepository.save(entity);
        return tenantSettings;
    }

    public TenantSettings updateTenantSettings(UUID tenantId, TenantSettings tenantSettings) {
        com.onetattva.infron.db.model.Tenant entity = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        // TODO: Merge settings JSON and save
        entity.setUpdatedAt(Instant.now());
        tenantRepository.save(entity);
        return tenantSettings;
    }

    private Tenant mapEntityToApi(com.onetattva.infron.db.model.Tenant entity) {
        Tenant api = new Tenant();
        api.setId(entity.getId());
        api.setName(entity.getName());
        api.setDisplayName(entity.getDisplayName());
        api.setStatus(Tenant.StatusEnum.fromValue(entity.getStatus().toString().toLowerCase()));
        try {
            api.setMetadata(entity.getMetadata());
        } catch (Exception e) {
            api.setMetadata(Map.of());
        }
        if (entity.getCreatedAt() != null) {
            api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getUpdatedAt() != null) {
            api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }
        return api;
    }

    private com.onetattva.infron.db.TenantStatus mapApiStatus(Tenant.StatusEnum status) {
        return com.onetattva.infron.db.TenantStatus.valueOf(status.name());
    }
}
