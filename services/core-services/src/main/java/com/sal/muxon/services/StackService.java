package com.sal.muxon.services;

import com.sal.muxon.db.model.StackEntity;
import com.sal.muxon.db.model.StackEntity.StackStatus;
import com.sal.muxon.db.model.TenantDatacenterGrantEntity;
import com.sal.muxon.db.repository.StackRepository;
import com.sal.muxon.db.repository.TenantDatacenterGrantRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class StackService {

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private TenantDatacenterGrantRepository grantRepository;

    public List<StackEntity> listByGrant(UUID tenantId, UUID datacenterId) {
        TenantDatacenterGrantEntity grant = findGrant(tenantId, datacenterId);
        return stackRepository.findByTenantDatacenterGrantId(grant.getId());
    }

    @Transactional
    public StackEntity create(UUID tenantId, UUID datacenterId, String name, String description,
                               Map<String, String> metadata) {
        TenantDatacenterGrantEntity grant = findGrant(tenantId, datacenterId);

        if (stackRepository.existsByTenantDatacenterGrantIdAndName(grant.getId(), name)) {
            throw new IllegalStateException("CONFLICT: Stack with name '" + name + "' already exists in this grant");
        }

        StackEntity entity = new StackEntity();
        entity.setTenantDatacenterGrant(grant);
        entity.setName(name);
        entity.setDescription(description);
        entity.setMetadata(metadata);
        entity.setStatus(StackStatus.ACTIVE);
        return stackRepository.save(entity);
    }

    public StackEntity getById(UUID stackId) {
        return stackRepository.findById(stackId)
                .orElseThrow(() -> new EntityNotFoundException("Stack not found: " + stackId));
    }

    public StackEntity getByIdAndTenant(UUID stackId, UUID tenantId) {
        StackEntity stack = getById(stackId);
        if (!stack.getTenantDatacenterGrant().getTenant().getId().equals(tenantId)) {
            throw new EntityNotFoundException("Stack not found: " + stackId);
        }
        return stack;
    }

    @Transactional
    public StackEntity update(UUID stackId, String name, String description, Map<String, String> metadata) {
        StackEntity entity = getById(stackId);
        if (name != null) entity.setName(name);
        if (description != null) entity.setDescription(description);
        if (metadata != null) entity.setMetadata(metadata);
        return stackRepository.save(entity);
    }

    @Transactional
    public void delete(UUID stackId) {
        StackEntity entity = getById(stackId);
        if (stackRepository.hasActiveVms(stackId)) {
            throw new IllegalStateException("CONFLICT: Stack has active VMs; delete or terminate them first");
        }
        entity.setStatus(StackStatus.DELETING);
        stackRepository.save(entity);
    }

    private TenantDatacenterGrantEntity findGrant(UUID tenantId, UUID datacenterId) {
        return grantRepository.findByTenant_IdAndDatacenter_Id(tenantId, datacenterId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No active grant found for tenant " + tenantId + " in datacenter " + datacenterId));
    }
}
