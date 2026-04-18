package com.krito.muxon.core.services;

import tools.jackson.databind.ObjectMapper;
import com.krito.muxon.api.model.*;
import com.krito.muxon.core.common.EntityNotFoundException;
import com.krito.muxon.db.model.DatacenterEntity;
import com.krito.muxon.db.model.TenantDatacenterGrantEntity;
import com.krito.muxon.db.model.TenantEntity;
import com.krito.muxon.db.repository.DatacenterRepository;
import com.krito.muxon.db.repository.TenantDatacenterGrantRepository;
import com.krito.muxon.db.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TenantDatacenterGrantService {

    @Autowired
    private TenantDatacenterGrantRepository grantRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private DatacenterRepository datacenterRepository;

    @Autowired
    private DatacentersService datacentersService;

    @Autowired
    private com.krito.muxon.core.services.storage.StorageClassValidationService storageClassValidationService;

    @Autowired
    private ObjectMapper objectMapper;

    public TenantDatacenterGrant createTenantDatacenterGrant(UUID tenantId, TenantDatacenterGrantCreate create) {
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));
        DatacenterEntity datacenter = datacenterRepository.findById(create.getDatacenterId())
                .orElseThrow(() -> new EntityNotFoundException("Datacenter not found: " + create.getDatacenterId()));

        Optional<TenantDatacenterGrantEntity> existing = grantRepository.findByTenant_IdAndDatacenter_Id(tenantId, create.getDatacenterId());
        TenantDatacenterGrantEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            applyCreateToEntity(create, entity);
            entity.setUpdatedAt(Instant.now());
        } else {
            entity = new TenantDatacenterGrantEntity();
            entity.setId(UUID.randomUUID());
            entity.setTenant(tenant);
            entity.setDatacenter(datacenter);
            applyCreateToEntity(create, entity);
            Instant now = Instant.now();
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
        }
        TenantDatacenterGrantEntity saved = grantRepository.save(entity);
        return mapEntityToGrant(saved);
    }

    public TenantDatacenterGrantList listTenantDatacenters(UUID tenantId, Integer page, Integer perPage) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new EntityNotFoundException("Tenant not found: " + tenantId);
        }
        int p = page != null ? Math.max(1, page) : 1;
        int pp = perPage != null ? Math.min(Math.max(1, perPage), 200) : 20;
        Pageable pageable = PageRequest.of(p - 1, pp);
        Page<TenantDatacenterGrantEntity> entityPage = grantRepository.findByTenant_Id(tenantId, pageable);
        List<TenantDatacenterGrant> items = entityPage.getContent().stream()
                .map(this::mapEntityToGrant)
                .toList();

        TenantDatacenterGrantList list = new TenantDatacenterGrantList();
        list.setTotal((int) entityPage.getTotalElements());
        list.setPage(p);
        list.setPerPage(pp);
        list.setItems(items);
        return list;
    }

    public TenantDatacenterGrant getTenantDatacenterGrant(UUID tenantId, UUID datacenterId) {
        TenantDatacenterGrantEntity entity = grantRepository.findByTenant_IdAndDatacenter_Id(tenantId, datacenterId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant datacenter grant not found for tenant " + tenantId + " and datacenter " + datacenterId));
        return mapEntityToGrant(entity);
    }

    public TenantDatacenterGrant replaceTenantDatacenterGrant(UUID tenantId, UUID datacenterId, TenantDatacenterGrant grant) {
        TenantDatacenterGrantEntity entity = grantRepository.findByTenant_IdAndDatacenter_Id(tenantId, datacenterId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant datacenter grant not found for tenant " + tenantId + " and datacenter " + datacenterId));
        applyGrantToEntity(grant, entity);
        entity.setUpdatedAt(Instant.now());
        TenantDatacenterGrantEntity saved = grantRepository.save(entity);
        return mapEntityToGrant(saved);
    }

    public TenantDatacenterGrant updateTenantDatacenterGrant(UUID tenantId, UUID datacenterId, TenantDatacenterGrant grant) {
        TenantDatacenterGrantEntity entity = grantRepository.findByTenant_IdAndDatacenter_Id(tenantId, datacenterId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant datacenter grant not found for tenant " + tenantId + " and datacenter " + datacenterId));
        if (grant.getAccess() != null) entity.setAccess(grant.getAccess());
        if (grant.getLimits() != null) entity.setLimits(toMap(grant.getLimits()));
        if (grant.getEnabledFeatures() != null) entity.setEnabledFeatures(stringsFromFeatures(grant.getEnabledFeatures()));
        if (grant.getOverrideSettings() != null) entity.setOverrideSettings(grant.getOverrideSettings());
        entity.setUpdatedAt(Instant.now());
        TenantDatacenterGrantEntity saved = grantRepository.save(entity);
        return mapEntityToGrant(saved);
    }

    public void deleteTenantDatacenterGrant(UUID tenantId, UUID datacenterId) {
        TenantDatacenterGrantEntity entity = grantRepository.findByTenant_IdAndDatacenter_Id(tenantId, datacenterId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant datacenter grant not found for tenant " + tenantId + " and datacenter " + datacenterId));
        grantRepository.delete(entity);
    }

    public GetTenantDatacenterEffective200Response getTenantDatacenterEffective(UUID tenantId, UUID datacenterId) {
        TenantDatacenterGrantEntity grantEntity = grantRepository.findByTenant_IdAndDatacenter_Id(tenantId, datacenterId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant datacenter grant not found for tenant " + tenantId + " and datacenter " + datacenterId));
        DatacenterEntity dcEntity = datacenterRepository.findById(datacenterId)
                .orElseThrow(() -> new EntityNotFoundException("Datacenter not found: " + datacenterId));

        Datacenter datacenter = datacentersService.getDatacenter(datacenterId);
        TenantDatacenterGrant grant = mapEntityToGrant(grantEntity);
        DatacenterSettings effectiveSettings = mergeEffectiveSettings(
                dcEntity.getSettings(),
                grantEntity.getOverrideSettings()
        );

        GetTenantDatacenterEffective200Response response = new GetTenantDatacenterEffective200Response();
        response.setDatacenter(datacenter);
        response.setGrant(grant);
        response.setEffectiveSettings(effectiveSettings);
        return response;
    }

    private void applyCreateToEntity(TenantDatacenterGrantCreate create, TenantDatacenterGrantEntity entity) {
        entity.setAccess(create.getAccess() != null ? create.getAccess() : true);
        entity.setLimits(toMap(create.getLimits()));
        entity.setEnabledFeatures(create.getEnabledFeatures() != null ? stringsFromFeatures(create.getEnabledFeatures()) : null);
        entity.setOverrideSettings(create.getOverrideSettings());
    }

    private void applyGrantToEntity(TenantDatacenterGrant grant, TenantDatacenterGrantEntity entity) {
        entity.setAccess(grant.getAccess() != null ? grant.getAccess() : true);
        entity.setLimits(toMap(grant.getLimits()));
        entity.setEnabledFeatures(grant.getEnabledFeatures() != null ? stringsFromFeatures(grant.getEnabledFeatures()) : null);
        entity.setOverrideSettings(grant.getOverrideSettings());
    }

    private static List<String> stringsFromFeatures(List<DatacenterFeature> features) {
        if (features == null) return null;
        return features.stream().map(DatacenterFeature::getValue).collect(Collectors.toList());
    }

    private static List<DatacenterFeature> featuresFromStrings(List<String> strings) {
        if (strings == null || strings.isEmpty()) return new ArrayList<>();
        List<DatacenterFeature> result = new ArrayList<>();
        for (String s : strings) {
            try {
                result.add(DatacenterFeature.fromValue(s));
            } catch (IllegalArgumentException ignored) {
                // skip unknown feature values
            }
        }
        return result;
    }

    private TenantDatacenterGrant mapEntityToGrant(TenantDatacenterGrantEntity entity) {
        TenantDatacenterGrant grant = new TenantDatacenterGrant();
        grant.setId(entity.getId());
        TenantEntity tenant = entity.getTenant();
        EntityReference tenantRef = new EntityReference();
        tenantRef.setId(tenant.getId());
        tenantRef.setName(tenant.getName());
        tenantRef.setDescription(tenant.getDisplayName());
        grant.setTenant(tenantRef);
        DatacenterEntity dc = entity.getDatacenter();
        EntityReference datacenterRef = new EntityReference();
        datacenterRef.setId(dc.getId());
        datacenterRef.setName(dc.getName());
        datacenterRef.setDescription(dc.getDescription());
        grant.setDatacenter(datacenterRef);
        grant.setAccess(entity.getAccess() != null ? entity.getAccess() : true);
        grant.setLimits(fromMap(entity.getLimits()));
        grant.setEnabledFeatures(featuresFromStrings(entity.getEnabledFeatures()));
        grant.setOverrideSettings(entity.getOverrideSettings());
        if (entity.getCreatedAt() != null) {
            grant.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getUpdatedAt() != null) {
            grant.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }
        return grant;
    }

    private static DatacenterSettings mergeEffectiveSettings(DatacenterSettings base, DatacenterSettings override) {
        DatacenterSettings result = base != null ? base : new DatacenterSettings();
        if (override == null) return result;
        if (override.getVmClasses() != null) result.setVmClasses(override.getVmClasses());
        if (override.getStorageClasses() != null) result.setStorageClasses(override.getStorageClasses());
        if (override.getNetworkDomains() != null) result.setNetworkDomains(override.getNetworkDomains());
        if (override.getProviderSpecificSettings() != null) result.setProviderSpecificSettings(override.getProviderSpecificSettings());
        return result;
    }

    public List<String> getEffectiveStorageClasses(UUID tenantId, UUID datacenterId) {
        return storageClassValidationService.getEffectiveStorageClasses(tenantId, datacenterId);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object value) {
        if (value == null) {
            return null;
        }
        return objectMapper.convertValue(value, Map.class);
    }

    private ResourceLimits fromMap(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.convertValue(value, ResourceLimits.class);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Failed to deserialize tenant datacenter grant limits JSON", e);
        }
    }

    public java.util.Map<String, StorageUsage> getStorageUsageByClass(UUID tenantId, UUID datacenterId) {
        java.util.Map<String, Long> limits = storageClassValidationService.getStorageClassLimits(tenantId, datacenterId);
        java.util.Map<String, StorageUsage> result = new java.util.HashMap<>();
        
        for (java.util.Map.Entry<String, Long> entry : limits.entrySet()) {
            StorageUsage usage = new StorageUsage();
            usage.setStorageClass(entry.getKey());
            usage.setUsedGb(0L);
            usage.setLimitGb(entry.getValue());
            usage.setUtilizationPercent(0.0);
            result.put(entry.getKey(), usage);
        }
        
        return result;
    }
}
