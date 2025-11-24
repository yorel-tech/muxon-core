package com.onetattva.infron.services;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.db.model.RoleBindingEntity;
import com.onetattva.infron.db.model.RoleEntity;
import com.onetattva.infron.db.repository.RoleBindingRepository;
import com.onetattva.infron.db.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BindingsService {

    @Autowired
    private RoleBindingRepository roleBindingRepository;

    @Autowired
    private RoleRepository roleRepository;

    public RoleBindingList bulkCreateRoleBindings(RoleBindingBulkCreate request) {
        List<RoleBindingEntity> entities = new ArrayList<>();
        for (RoleBindingCreateItem item : request.getBindings()) {
            RoleEntity role = roleRepository.findById(item.getRoleId()).orElse(null);
            if (role == null) continue; // skip invalid role
            RoleBindingEntity entity = new RoleBindingEntity();
            entity.setId(UUID.randomUUID());
            entity.setRole(role);
            entity.setSubjectType(item.getSubjectType().getValue());
            entity.setSubjectId(item.getSubjectId());
            entity.setScopeType(item.getScopeType().getValue());
            entity.setScopeId(item.getScopeId());
            entity.setExpiresAt(item.getExpiresAt() != null ? item.getExpiresAt().toInstant() : null);
            entity.setCreatedAt(Instant.now());
            entities.add(entity);
        }
        List<RoleBindingEntity> saved = roleBindingRepository.saveAll(entities);
        RoleBindingList result = new RoleBindingList();
        result.setItems(saved.stream()
                .map(this::mapEntityToApi)
                .collect(Collectors.toList()));
        return result;
    }

    public RoleBindingList createRoleBindings(UUID roleId, RoleBindingBulkCreate request) {
        RoleEntity role = roleRepository.findById(roleId).orElseThrow(() -> new RuntimeException("Role not found"));
        List<RoleBindingEntity> entities = new ArrayList<>();
        for (RoleBindingCreateItem item : request.getBindings()) {
            RoleBindingEntity entity = new RoleBindingEntity();
            entity.setId(UUID.randomUUID());
            entity.setRole(role);
            entity.setSubjectType(item.getSubjectType().getValue());
            entity.setSubjectId(item.getSubjectId());
            entity.setScopeType(item.getScopeType().getValue());
            entity.setScopeId(item.getScopeId());
            entity.setExpiresAt(item.getExpiresAt() != null ? item.getExpiresAt().toInstant() : null);
            entity.setCreatedAt(Instant.now());
            entities.add(entity);
        }
        List<RoleBindingEntity> saved = roleBindingRepository.saveAll(entities);
        RoleBindingList result = new RoleBindingList();
        result.setItems(saved.stream()
                .map(this::mapEntityToApi)
                .collect(Collectors.toList()));
        return result;
    }

    public RoleBindingList createTenantRoleBindings(UUID tenantId, UUID roleId, RoleBindingBulkCreate request) {
        RoleEntity role = roleRepository.findById(roleId).orElseThrow(() -> new RuntimeException("Role not found"));
        List<RoleBindingEntity> entities = new ArrayList<>();
        for (RoleBindingCreateItem item : request.getBindings()) {
            RoleBindingEntity entity = new RoleBindingEntity();
            entity.setId(UUID.randomUUID());
            entity.setRole(role);
            entity.setSubjectType(item.getSubjectType().getValue());
            entity.setSubjectId(item.getSubjectId());
            entity.setScopeType("tenant");
            entity.setScopeId(tenantId);
            entity.setExpiresAt(item.getExpiresAt() != null ? item.getExpiresAt().toInstant() : null);
            entity.setCreatedAt(Instant.now());
            entities.add(entity);
        }
        List<RoleBindingEntity> saved = roleBindingRepository.saveAll(entities);
        RoleBindingList result = new RoleBindingList();
        result.setItems(saved.stream()
                .map(this::mapEntityToApi)
                .collect(Collectors.toList()));
        return result;
    }

    public RoleBindingList listRoleBindings(UUID roleId) {
        List<RoleBindingEntity> entities = roleBindingRepository.findByRole_Id(roleId);
        RoleBindingList result = new RoleBindingList();
        result.setItems(entities.stream()
                .map(this::mapEntityToApi)
                .collect(Collectors.toList()));
        return result;
    }

    public RoleBindingList listTenantRoleBindings(UUID tenantId, UUID roleId) {
        List<RoleBindingEntity> entities = roleBindingRepository.findByRole_IdAndScopeTypeAndScopeId(roleId, "tenant", tenantId);
        RoleBindingList result = new RoleBindingList();
        result.setItems(entities.stream()
                .map(this::mapEntityToApi)
                .collect(Collectors.toList()));
        return result;
    }

    public RoleBindingList listUserBindings(UUID userId) {
        List<RoleBindingEntity> entities = roleBindingRepository.findBySubjectTypeAndSubjectId("user", userId.toString());
        RoleBindingList response = new RoleBindingList();
        response.setItems(entities.stream()
                .map(this::mapEntityToApi)
                .collect(Collectors.toList()));
        return response;
    }

    private RoleBinding mapEntityToApi(RoleBindingEntity entity) {
        RoleBinding binding = new RoleBinding();
        binding.setId(entity.getId());
        binding.setRoleId(entity.getRole().getId());
        binding.setSubjectType(entity.getSubjectType());
        binding.setSubjectId(entity.getSubjectId());
        binding.setScopeType(entity.getScopeType());
        binding.setScopeId(entity.getScopeId());
        binding.setExpiresAt(entity.getExpiresAt() != null ? entity.getExpiresAt().atOffset(ZoneOffset.UTC) : null);
        binding.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        return binding;
    }
}
