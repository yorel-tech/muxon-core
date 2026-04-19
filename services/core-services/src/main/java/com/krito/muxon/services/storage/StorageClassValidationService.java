package com.krito.muxon.services.storage;

import com.krito.muxon.api.model.StorageClassValidationResult;
import com.krito.muxon.api.model.StorageClassWarning;
import com.krito.muxon.db.model.DatacenterEntity;
import com.krito.muxon.db.model.StorageClassEntity;
import com.krito.muxon.db.model.TenantDatacenterGrantEntity;
import com.krito.muxon.db.repository.DatacenterRepository;
import com.krito.muxon.db.repository.StorageClassRepository;
import com.krito.muxon.db.repository.TenantDatacenterGrantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StorageClassValidationService {

    @Autowired
    private StorageClassRepository storageClassRepository;

    @Autowired
    private DatacenterRepository datacenterRepository;

    @Autowired
    private TenantDatacenterGrantRepository tenantDatacenterGrantRepository;

    public boolean storageClassExists(String storageClassName) {
        return storageClassRepository.findByName(storageClassName).isPresent();
    }

    public boolean isStorageClassAvailableInDatacenter(UUID datacenterId, String storageClassName) {
        DatacenterEntity datacenter = datacenterRepository.findById(datacenterId).orElse(null);
        if (datacenter == null || datacenter.getSettings() == null) {
            return false;
        }
        
        List<String> availableClasses = datacenter.getSettings().getStorageClasses();
        if (availableClasses == null || availableClasses.isEmpty()) {
            return true;
        }
        
        return availableClasses.contains(storageClassName);
    }

    public boolean isStorageClassAllowedForTenant(UUID tenantId, UUID datacenterId, String storageClassName) {
        List<String> effectiveClasses = getEffectiveStorageClasses(tenantId, datacenterId);
        return effectiveClasses.contains(storageClassName);
    }

    public List<String> getEffectiveStorageClasses(UUID tenantId, UUID datacenterId) {
        DatacenterEntity datacenter = datacenterRepository.findById(datacenterId).orElse(null);
        if (datacenter == null) {
            return Collections.emptyList();
        }

        List<String> datacenterClasses = datacenter.getSettings() != null 
            ? datacenter.getSettings().getStorageClasses() 
            : null;

        TenantDatacenterGrantEntity grant = tenantDatacenterGrantRepository
            .findByTenant_IdAndDatacenter_Id(tenantId, datacenterId)
            .orElse(null);

        if (grant == null) {
            return datacenterClasses != null ? datacenterClasses : Collections.emptyList();
        }

        if (grant.getOverrideSettings() != null && grant.getOverrideSettings().getStorageClasses() != null) {
            List<String> tenantClasses = grant.getOverrideSettings().getStorageClasses();
            if (datacenterClasses == null || datacenterClasses.isEmpty()) {
                return tenantClasses;
            }
            return tenantClasses.stream()
                .filter(datacenterClasses::contains)
                .collect(Collectors.toList());
        }

        return datacenterClasses != null ? datacenterClasses : Collections.emptyList();
    }

    public Map<String, Long> getStorageClassLimits(UUID tenantId, UUID datacenterId) {
        TenantDatacenterGrantEntity grant = tenantDatacenterGrantRepository
            .findByTenant_IdAndDatacenter_Id(tenantId, datacenterId)
            .orElse(null);

        if (grant == null || grant.getLimits() == null) {
            return Collections.emptyMap();
        }

        Object limitsGbObj = grant.getLimits().get("storageClassLimitsGb");
        if (!(limitsGbObj instanceof Map<?, ?> limitsGb)) {
            return Collections.emptyMap();
        }

        Map<String, Long> result = new HashMap<>();
        limitsGb.forEach((key, value) -> {
            if (key instanceof String keyString && value instanceof Number numberValue) {
                result.put(keyString, numberValue.longValue());
            }
        });
        return result;
    }

    public StorageClassValidationResult validateStorageClasses(List<String> storageClasses) {
        StorageClassValidationResult result = new StorageClassValidationResult();
        List<StorageClassWarning> warnings = new ArrayList<>();
        boolean allValid = true;

        for (String className : storageClasses) {
            if (!storageClassExists(className)) {
                StorageClassWarning warning = new StorageClassWarning();
                warning.setStorageClass(className);
                warning.setMessage("Storage class '" + className + "' not found in global registry");
                warning.setSeverity(StorageClassWarning.SeverityEnum.WARNING);
                warnings.add(warning);
                allValid = false;
            }
        }

        result.setValid(allValid);
        result.setWarnings(warnings);
        result.setAvailableStorageClasses(
            storageClassRepository.findAll().stream()
                .map(StorageClassEntity::getName)
                .collect(Collectors.toList())
        );

        return result;
    }

    public StorageClassValidationResult validateDatacenterStorageClasses(
            UUID datacenterId, 
            List<String> storageClasses) {
        
        StorageClassValidationResult result = validateStorageClasses(storageClasses);
        
        DatacenterEntity datacenter = datacenterRepository.findById(datacenterId).orElse(null);
        if (datacenter != null) {
            List<String> currentClasses = datacenter.getSettings() != null 
                ? datacenter.getSettings().getStorageClasses() 
                : null;
            
            if (currentClasses != null && !currentClasses.isEmpty()) {
                result.setAvailableStorageClasses(currentClasses);
            }
        }
        
        return result;
    }

    public StorageClassValidationResult validateTenantStorageClasses(
            UUID tenantId,
            UUID datacenterId,
            List<String> storageClasses) {
        
        List<String> datacenterClasses = getDatacenterStorageClasses(datacenterId);
        List<StorageClassWarning> warnings = new ArrayList<>();
        boolean allValid = true;

        for (String className : storageClasses) {
            if (!storageClassExists(className)) {
                StorageClassWarning warning = new StorageClassWarning();
                warning.setStorageClass(className);
                warning.setMessage("Storage class '" + className + "' not found in global registry");
                warning.setSeverity(StorageClassWarning.SeverityEnum.WARNING);
                warnings.add(warning);
                allValid = false;
            } else if (!datacenterClasses.isEmpty() && !datacenterClasses.contains(className)) {
                StorageClassWarning warning = new StorageClassWarning();
                warning.setStorageClass(className);
                warning.setMessage("Storage class '" + className + "' not available in datacenter");
                warning.setSeverity(StorageClassWarning.SeverityEnum.ERROR);
                warnings.add(warning);
                allValid = false;
            }
        }

        StorageClassValidationResult result = new StorageClassValidationResult();
        result.setValid(allValid);
        result.setWarnings(warnings);
        result.setAvailableStorageClasses(datacenterClasses);

        return result;
    }

    public List<String> getDatacenterStorageClasses(UUID datacenterId) {
        DatacenterEntity datacenter = datacenterRepository.findById(datacenterId).orElse(null);
        if (datacenter == null || datacenter.getSettings() == null) {
            return Collections.emptyList();
        }
        
        List<String> classes = datacenter.getSettings().getStorageClasses();
        return classes != null ? classes : Collections.emptyList();
    }

    public boolean checkStorageAllocation(
            UUID tenantId,
            UUID datacenterId,
            Map<String, Long> requestedStorageGb) {
        
        Map<String, Long> limits = getStorageClassLimits(tenantId, datacenterId);
        if (limits.isEmpty()) {
            return true;
        }

        Map<String, Long> currentUsage = getCurrentStorageUsage(tenantId, datacenterId);

        for (Map.Entry<String, Long> request : requestedStorageGb.entrySet()) {
            String storageClass = request.getKey();
            Long requestedGb = request.getValue();
            
            Long limit = limits.get(storageClass);
            if (limit != null) {
                Long used = currentUsage.getOrDefault(storageClass, 0L);
                if (used + requestedGb > limit) {
                    return false;
                }
            }
        }

        return true;
    }

    private Map<String, Long> getCurrentStorageUsage(UUID tenantId, UUID datacenterId) {
        return Collections.emptyMap();
    }
}
