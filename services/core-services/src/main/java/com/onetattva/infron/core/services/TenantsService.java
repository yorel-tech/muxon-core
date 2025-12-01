package com.onetattva.infron.core.services;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.db.TenantStatus;
import com.onetattva.infron.db.model.TenantEntity;
import com.onetattva.infron.db.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TenantsService {

    @Autowired
    private TenantRepository tenantRepository;

    public Tenant createTenant(TenantCreate tenantCreate) {
        TenantEntity entityTenant = new TenantEntity();
        entityTenant.setId(UUID.randomUUID());
        entityTenant.setName(tenantCreate.getName());
        entityTenant.setDisplayName(tenantCreate.getDisplayName());
        entityTenant.setStatus(TenantStatus.ACTIVE);
        try {
            entityTenant.setMetadata(tenantCreate.getMetadata());
        } catch (Exception e) {
            entityTenant.setMetadata(Map.of());
        }
        Instant now = Instant.now();
        entityTenant.setCreatedAt(now);
        entityTenant.setUpdatedAt(now);
        TenantEntity saved = tenantRepository.save(entityTenant);
        return mapEntityToApi(saved);
    }

    public void deleteTenant(UUID tenantId) {
        tenantRepository.deleteById(tenantId);
    }

    public Tenant getTenant(UUID tenantId) {
        TenantEntity entity = tenantRepository.findById(tenantId)
                .orElseThrow();
        return mapEntityToApi(entity);
    }

    public TenantList listTenants(Integer page, Integer perPage, String sort, String name, String status) {
        // TODO: Implement listing with filters and pagination
        List<TenantEntity> entities = tenantRepository.findAll().stream()
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
        TenantEntity entity = tenantRepository.findById(tenantId)
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
        TenantEntity saved = tenantRepository.save(entity);
        return mapEntityToApi(saved);
    }

    public Tenant updateTenant(UUID tenantId, TenantUpdate tenantUpdate) {
        TenantEntity entity = tenantRepository.findById(tenantId)
                .orElseThrow();
        entity.setDisplayName(tenantUpdate.getDisplayName());
        try {
            entity.setMetadata(tenantUpdate.getMetadata());
        } catch (Exception e) {
            // ignore
        }
        entity.setUpdatedAt(Instant.now());
        TenantEntity saved = tenantRepository.save(entity);
        return mapEntityToApi(saved);
    }

    public TenantSettings getTenantSettings(UUID tenantId) {
        TenantEntity entity = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        return entity.getSettings();
    }

    public TenantSettings replaceTenantSettings(UUID tenantId, TenantSettings tenantSettings) {
        TenantEntity entity = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        // TODO: Serialize settings to JSON and save
        entity.setUpdatedAt(Instant.now());
        tenantRepository.save(entity);
        return tenantSettings;
    }

    public TenantSettings updateTenantSettings(UUID tenantId, TenantSettings tenantSettings) {
        TenantEntity entity = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        // TODO: Merge settings JSON and save
        entity.setUpdatedAt(Instant.now());
        tenantRepository.save(entity);
        return tenantSettings;
    }

    private Tenant mapEntityToApi(TenantEntity entity) {
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

    private TenantStatus mapApiStatus(Tenant.StatusEnum status) {
        return TenantStatus.valueOf(status.name());
    }
}
