package com.yorel.muxon.services;

import com.yorel.muxon.db.model.SubnetEntity;
import com.yorel.muxon.db.model.SubnetRbacEntity;
import com.yorel.muxon.db.model.SubnetRbacEntity.SubnetPrincipalType;
import com.yorel.muxon.db.repository.SubnetRbacRepository;
import com.yorel.muxon.db.repository.SubnetRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SubnetRbacService {

    @Autowired
    private SubnetRbacRepository rbacRepository;

    @Autowired
    private SubnetRepository subnetRepository;

    public List<SubnetRbacEntity> listBySubnet(UUID subnetId) {
        return rbacRepository.findBySubnetId(subnetId);
    }

    @Transactional
    public SubnetRbacEntity create(UUID subnetId, SubnetPrincipalType principalType,
                                    UUID principalId, List<String> permissions) {
        SubnetEntity subnet = subnetRepository.findById(subnetId)
                .orElseThrow(() -> new EntityNotFoundException("Subnet not found: " + subnetId));

        rbacRepository.findBySubnetIdAndPrincipalTypeAndPrincipalId(subnetId, principalType, principalId)
                .ifPresent(existing -> {
                    throw new IllegalStateException("CONFLICT: RBAC binding already exists for this principal on subnet " + subnetId);
                });

        SubnetRbacEntity entity = new SubnetRbacEntity();
        entity.setSubnet(subnet);
        entity.setPrincipalType(principalType);
        entity.setPrincipalId(principalId);
        entity.setPermissions(permissions);
        return rbacRepository.save(entity);
    }

    @Transactional
    public void delete(UUID rbacId) {
        if (!rbacRepository.existsById(rbacId)) {
            throw new EntityNotFoundException("RBAC binding not found: " + rbacId);
        }
        rbacRepository.deleteById(rbacId);
    }

    public boolean hasPermission(UUID subnetId, UUID principalId, String permission) {
        return rbacRepository.hasPermission(subnetId, principalId, permission);
    }

    public List<SubnetEntity> getAuthorizedSubnets(UUID tenantId, UUID vpcId, UUID stackId) {
        List<UUID> authorizedIds = rbacRepository.findAuthorizedSubnetIds(stackId, vpcId);
        return subnetRepository.findByVpcId(vpcId).stream()
                .filter(s -> authorizedIds.contains(s.getId()))
                .toList();
    }
}
