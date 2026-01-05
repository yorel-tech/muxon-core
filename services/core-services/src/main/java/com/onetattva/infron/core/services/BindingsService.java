package com.onetattva.infron.core.services;

import com.onetattva.infron.api.model.RoleBinding;
import com.onetattva.infron.api.model.RoleBindingBulkCreate;
import com.onetattva.infron.api.model.RoleBindingCreateItem;
import com.onetattva.infron.api.model.RoleBindingList;
import com.onetattva.infron.core.auth.AuthorizationService;
import com.onetattva.infron.core.auth.UserPrincipal;
import com.onetattva.infron.core.common.Constants;
import com.onetattva.infron.core.common.EntityNotFoundException;
import com.onetattva.infron.core.common.UuidUtils;
import com.onetattva.infron.db.RoleBindingSubjectType;
import com.onetattva.infron.db.RoleScopeType;
import com.onetattva.infron.db.model.RoleBindingEntity;
import com.onetattva.infron.db.model.RoleEntity;
import com.onetattva.infron.db.repository.RoleBindingRepository;
import com.onetattva.infron.db.repository.RoleRepository;
import com.onetattva.infron.db.repository.TenantRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BindingsService {

    @Autowired
    private RoleBindingRepository roleBindingRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private AuthorizationService authorizationService;

    // Rate limiting: max 10 role binding changes per minute per user
    private static final int MAX_CHANGES_PER_MINUTE = 10;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);


    private UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) auth.getPrincipal();
        }
        throw new RuntimeException("No authenticated user found");
    }

    private void checkRateLimit(String userId) {
        String key = "rate_limit:role_binding:" + userId;
        String count = redis.opsForValue().get(key);
        int currentCount = count != null ? Integer.parseInt(count) : 0;

        if (currentCount >= MAX_CHANGES_PER_MINUTE) {
            throw new RuntimeException("Rate limit exceeded: maximum " + MAX_CHANGES_PER_MINUTE + " role binding changes per minute");
        }

        redis.opsForValue().increment(key);
        redis.expire(key, RATE_LIMIT_WINDOW);
    }

    private void validateRoleBindingCreateItem(RoleBindingCreateItem item) {
        // Validate subject ID format

        if (RoleBindingCreateItem.SubjectTypeEnum.USER.equals(item.getSubjectType())
            || RoleBindingCreateItem.SubjectTypeEnum.SERVICE_ACCOUNT.equals(item.getSubjectType())) {
            if (!UuidUtils.isValidUUID(item.getSubjectId())) {
                throw new RuntimeException("Invalid subject ID format: must be a valid UUID");
            }
        }

        // Validate scope
        if (RoleBindingCreateItem.ScopeTypeEnum.TENANT.equals(item.getScopeType())) {
            if (item.getScopeId() == null) {
                throw new RuntimeException("scope_id is required for TENANT scope");
            }
            // Validate tenant exists
            if (!tenantRepository.existsById(item.getScopeId())) {
                throw new RuntimeException("Tenant does not exist: " + item.getScopeId());
            }
        } else if (RoleBindingCreateItem.ScopeTypeEnum.SYSTEM.equals(item.getScopeType())) {
            if (item.getScopeId() != null) {
                throw new RuntimeException("scope_id must be null for SYSTEM scope");
            }
        }
    }

    private void validateSystemScopeAccess() {
        UserPrincipal user = getCurrentUser();
        // Check if user has system:admin role
        boolean isSystemAdmin = user.roles().stream()
                .anyMatch(Constants.ROLE_SYSTEM_ADMIN::equals);
        if (!isSystemAdmin) {
            throw new RuntimeException("Only system administrators can create system scope role bindings");
        }
    }

    @Transactional
    public RoleBindingList bulkCreateRoleBindings(RoleBindingBulkCreate request) {
        UserPrincipal currentUser = getCurrentUser();
        checkRateLimit(currentUser.id());

        List<RoleBindingEntity> entities = new ArrayList<>();
        for (RoleBindingCreateItem item : request.getBindings()) {
            // Validate each item
            validateRoleBindingCreateItem(item);

            // Check privilege escalation for system scope
            if (RoleBindingCreateItem.ScopeTypeEnum.SYSTEM.equals(item.getScopeType())) {
                validateSystemScopeAccess();
            }

            RoleEntity role = roleRepository.findById(item.getRoleId()).orElseThrow(() -> new RuntimeException("Role not found: " + item.getRoleId()));

            RoleBindingEntity entity = new RoleBindingEntity();
            entity.setId(UUID.randomUUID());
            entity.setRole(role);
            entity.setSubjectType(RoleBindingSubjectType.valueOf(item.getSubjectType().name()));
            entity.setSubjectId(item.getSubjectId());
            entity.setScopeType(RoleScopeType.valueOf(item.getScopeType().name()));
            entity.setScopeId(item.getScopeId());
            entity.setExpiresAt(item.getExpiresAt() != null ? item.getExpiresAt().toInstant() : null);
            entity.setCreatedBy(UUID.fromString(currentUser.id()));
            entity.setCreatedAt(Instant.now());
            entities.add(entity);
        }
        List<RoleBindingEntity> saved = roleBindingRepository.saveAll(entities);
        RoleBindingList result = new RoleBindingList();
        result.setItems(saved.stream()
                .map(this::mapEntityToApi)
                .collect(Collectors.toList()));

        // Evict permission cache for affected users
        for (RoleBindingEntity entity : saved) {
            if (entity.getSubjectType() == RoleBindingSubjectType.USER) {
                authorizationService.evictCacheForUser(entity.getSubjectId());
            }
        }

        return result;
    }

    @Transactional
    public RoleBindingList createRoleBindings(UUID roleId, RoleBindingBulkCreate request) {
        UserPrincipal currentUser = getCurrentUser();
        checkRateLimit(currentUser.id());

        RoleEntity role = roleRepository.findById(roleId).orElseThrow(() -> new RuntimeException("Role not found"));
        List<RoleBindingEntity> entities = new ArrayList<>();
        for (RoleBindingCreateItem item : request.getBindings()) {
            // Validate each item
            validateRoleBindingCreateItem(item);

            // Check privilege escalation for system scope
            if (RoleBindingCreateItem.ScopeTypeEnum.SYSTEM.equals(item.getScopeType())) {
                validateSystemScopeAccess();
            }

            RoleBindingEntity entity = new RoleBindingEntity();
            entity.setId(UUID.randomUUID());
            entity.setRole(role);
            entity.setSubjectType(RoleBindingSubjectType.valueOf(item.getSubjectType().name()));
            entity.setSubjectId(item.getSubjectId());
            entity.setScopeType(RoleScopeType.valueOf(item.getScopeType().name()));
            entity.setScopeId(item.getScopeId());
            entity.setExpiresAt(item.getExpiresAt() != null ? item.getExpiresAt().toInstant() : null);
            entity.setCreatedBy(UUID.fromString(currentUser.id()));
            entity.setCreatedAt(Instant.now());
            entities.add(entity);
        }
        List<RoleBindingEntity> saved = roleBindingRepository.saveAll(entities);
        RoleBindingList result = new RoleBindingList();
        result.setItems(saved.stream()
                .map(this::mapEntityToApi)
                .collect(Collectors.toList()));

        // Evict permission cache for affected users
        for (RoleBindingEntity entity : saved) {
            if (entity.getSubjectType() == RoleBindingSubjectType.USER) {
                authorizationService.evictCacheForUser(entity.getSubjectId());
            }
        }

        return result;
    }

    @Transactional
    public RoleBindingList createTenantRoleBindings(UUID tenantId, UUID roleId, RoleBindingBulkCreate request) {
        UserPrincipal currentUser = getCurrentUser();
        checkRateLimit(currentUser.id());

        // Validate tenant exists
        if (!tenantRepository.existsById(tenantId)) {
            throw new RuntimeException("Tenant does not exist: " + tenantId);
        }

        RoleEntity role = roleRepository.findById(roleId).orElseThrow(() -> new RuntimeException("Role not found"));
        List<RoleBindingEntity> entities = new ArrayList<>();
        for (RoleBindingCreateItem item : request.getBindings()) {
            // Basic validation for subject ID
            if (RoleBindingCreateItem.SubjectTypeEnum.USER.equals(item.getSubjectType())
                || RoleBindingCreateItem.SubjectTypeEnum.SERVICE_ACCOUNT.equals(item.getSubjectType())) {
                if (!UuidUtils.isValidUUID(item.getSubjectId())) {
                    throw new RuntimeException("Invalid subject ID format: must be a valid UUID");
                }
            }

            RoleBindingEntity entity = new RoleBindingEntity();
            entity.setId(UUID.randomUUID());
            entity.setRole(role);
            entity.setSubjectType(RoleBindingSubjectType.valueOf(item.getSubjectType().name()));
            entity.setSubjectId(item.getSubjectId());
            entity.setScopeType(RoleScopeType.TENANT);
            entity.setScopeId(tenantId);
            entity.setExpiresAt(item.getExpiresAt() != null ? item.getExpiresAt().toInstant() : null);
            entity.setCreatedBy(UUID.fromString(currentUser.id()));
            entity.setCreatedAt(Instant.now());
            entities.add(entity);
        }
        List<RoleBindingEntity> saved = roleBindingRepository.saveAll(entities);
        RoleBindingList result = new RoleBindingList();
        result.setItems(saved.stream()
                .map(this::mapEntityToApi)
                .collect(Collectors.toList()));

        // Evict permission cache for affected users
        for (RoleBindingEntity entity : saved) {
            if (entity.getSubjectType() == RoleBindingSubjectType.USER) {
                authorizationService.evictCacheForUser(entity.getSubjectId());
            }
        }

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
        List<RoleBindingEntity> entities = roleBindingRepository.findByRole_IdAndScopeTypeAndScopeId(roleId, RoleScopeType.TENANT, tenantId);
        RoleBindingList result = new RoleBindingList();
        result.setItems(entities.stream()
                .map(this::mapEntityToApi)
                .collect(Collectors.toList()));
        return result;
    }

    public RoleBindingList listUserBindings(UUID userId) {
        List<RoleBindingEntity> entities = roleBindingRepository.findBySubjectTypeAndSubjectId(RoleBindingSubjectType.USER, userId.toString());
        RoleBindingList response = new RoleBindingList();
        response.setItems(entities.stream()
                .map(this::mapEntityToApi)
                .collect(Collectors.toList()));
        return response;
    }

    public RoleBinding getRoleBinding(UUID bindingId) {
        RoleBindingEntity entity = roleBindingRepository.findById(bindingId)
                .orElseThrow(() -> new EntityNotFoundException("Role binding not found: " + bindingId));
        return mapEntityToApi(entity);
    }

    public RoleBinding updateRoleBinding(UUID bindingId, com.onetattva.infron.api.model.RoleBindingUpdate update) {
        UserPrincipal currentUser = getCurrentUser();
        checkRateLimit(currentUser.id());

        RoleBindingEntity entity = roleBindingRepository.findById(bindingId)
                .orElseThrow(() -> new EntityNotFoundException("Role binding not found: " + bindingId));

        // Check privilege escalation for system scope
        if ("SYSTEM".equals(entity.getScopeType().name())) {
            validateSystemScopeAccess();
        }

        // Validate new role exists
        RoleEntity newRole = roleRepository.findById(update.getRoleId())
                .orElseThrow(() -> new RuntimeException("New role not found: " + update.getRoleId()));

        // Update the role
        entity.setRole(newRole);

        RoleBindingEntity saved = roleBindingRepository.save(entity);
        if (saved.getSubjectType() == RoleBindingSubjectType.USER) {
            authorizationService.evictCacheForUser(saved.getSubjectId());
        }
        return mapEntityToApi(saved);
    }

    public void deleteRoleBinding(UUID bindingId) {
        UserPrincipal currentUser = getCurrentUser();
        checkRateLimit(currentUser.id());

        RoleBindingEntity entity = roleBindingRepository.findById(bindingId)
                .orElseThrow(() -> new EntityNotFoundException("Role binding not found: " + bindingId));

        // Check privilege escalation for system scope
        if ("SYSTEM".equals(entity.getScopeType().name())) {
            validateSystemScopeAccess();
        }

        if (entity.getSubjectType() == RoleBindingSubjectType.USER) {
            authorizationService.evictCacheForUser(entity.getSubjectId());
        }
        roleBindingRepository.delete(entity);
    }

    public RoleBinding getRoleBindingByRole(UUID roleId, UUID bindingId) {
        // First verify the binding exists and belongs to the role
        RoleBindingEntity entity = roleBindingRepository.findById(bindingId)
                .orElseThrow(() -> new RuntimeException("Role binding not found: " + bindingId));

        if (!entity.getRole().getId().equals(roleId)) {
            throw new RuntimeException("Role binding does not belong to the specified role");
        }

        return mapEntityToApi(entity);
    }

    public RoleBinding updateRoleBindingByRole(UUID roleId, UUID bindingId, com.onetattva.infron.api.model.RoleBindingUpdate update) {
        UserPrincipal currentUser = getCurrentUser();
        checkRateLimit(currentUser.id());

        RoleBindingEntity entity = roleBindingRepository.findById(bindingId)
                .orElseThrow(() -> new RuntimeException("Role binding not found: " + bindingId));

        if (!entity.getRole().getId().equals(roleId)) {
            throw new RuntimeException("Role binding does not belong to the specified role");
        }

        // Check privilege escalation for system scope
        if ("SYSTEM".equals(entity.getScopeType().name())) {
            validateSystemScopeAccess();
        }

        // Validate new role exists
        RoleEntity newRole = roleRepository.findById(update.getRoleId())
                .orElseThrow(() -> new RuntimeException("New role not found: " + update.getRoleId()));

        // Update the role
        entity.setRole(newRole);

        RoleBindingEntity saved = roleBindingRepository.save(entity);
        if (saved.getSubjectType() == RoleBindingSubjectType.USER) {
            authorizationService.evictCacheForUser(saved.getSubjectId());
        }
        return mapEntityToApi(saved);
    }

    public void deleteRoleBindingByRole(UUID roleId, UUID bindingId) {
        UserPrincipal currentUser = getCurrentUser();
        checkRateLimit(currentUser.id());

        RoleBindingEntity entity = roleBindingRepository.findById(bindingId)
                .orElseThrow(() -> new RuntimeException("Role binding not found: " + bindingId));

        if (!entity.getRole().getId().equals(roleId)) {
            throw new RuntimeException("Role binding does not belong to the specified role");
        }

        // Check privilege escalation for system scope
        if ("SYSTEM".equals(entity.getScopeType().name())) {
            validateSystemScopeAccess();
        }

        if (entity.getSubjectType() == RoleBindingSubjectType.USER) {
            authorizationService.evictCacheForUser(entity.getSubjectId());
        }
        roleBindingRepository.delete(entity);
    }

    private String convertSubjectTypeToApiValue(RoleBindingSubjectType subjectType) {
        return switch (subjectType) {
            case USER -> RoleBindingCreateItem.SubjectTypeEnum.USER.getValue();
            case GROUP -> RoleBindingCreateItem.SubjectTypeEnum.GROUP.getValue();
            case SERVICE_ACCOUNT -> RoleBindingCreateItem.SubjectTypeEnum.SERVICE_ACCOUNT.getValue();
        };
    }

    private String convertScopeTypeToApiValue(RoleScopeType scopeType) {
        return switch (scopeType) {
            case SYSTEM -> RoleBindingCreateItem.ScopeTypeEnum.SYSTEM.getValue();
            case TENANT -> RoleBindingCreateItem.ScopeTypeEnum.TENANT.getValue();
            case TENANT_GLOBAL -> RoleBindingCreateItem.ScopeTypeEnum.TENANT_GLOBAL.getValue();
        };
    }

    private RoleBinding mapEntityToApi(RoleBindingEntity entity) {
        RoleBinding binding = new RoleBinding();
        binding.setId(entity.getId());
        binding.setRoleId(entity.getRole().getId());
        binding.setSubjectType(convertSubjectTypeToApiValue(entity.getSubjectType()));
        binding.setSubjectId(entity.getSubjectId());
        binding.setScopeType(convertScopeTypeToApiValue(entity.getScopeType()));
        binding.setScopeId(entity.getScopeId());
        binding.setExpiresAt(entity.getExpiresAt() != null ? entity.getExpiresAt().atOffset(ZoneOffset.UTC) : null);
        binding.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        return binding;
    }
}
