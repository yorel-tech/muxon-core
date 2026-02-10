package com.onetattva.infron.core.services;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.common.Constants;
import com.onetattva.infron.core.common.EncryptionUtil;
import com.onetattva.infron.core.services.model.OidcUserInfo;
import com.onetattva.infron.db.model.IdentityProviderEntity;
import com.onetattva.infron.db.model.IdpUserEntity;
import com.onetattva.infron.db.model.OidcIdentityProviderEntity;
import com.onetattva.infron.db.model.OidcMetadata;
import com.onetattva.infron.db.model.RoleBindingEntity;
import com.onetattva.infron.db.model.UserRoleBindingViewEntity;
import com.onetattva.infron.db.repository.IdpUserRepository;
import com.onetattva.infron.db.repository.IdentityProviderRepository;
import com.onetattva.infron.db.repository.RoleBindingRepository;
import com.onetattva.infron.db.repository.RoleRepository;
import com.onetattva.infron.db.repository.UserRoleBindingViewRepository;
import com.onetattva.infron.api.enums.RoleScopeType;
import com.onetattva.infron.api.enums.RoleBindingSubjectType;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for user management operations.
 * Handles listing, adding, updating, and deleting users for system and tenant scopes.
 */
@Service
public class UsersService {

    @Autowired
    private UserRoleBindingViewRepository userRoleBindingViewRepository;

    @Autowired
    private IdpUserRepository idpUserRepository;

    @Autowired
    private IdentityProviderRepository identityProviderRepository;

    @Autowired
    private RoleBindingRepository roleBindingRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BindingsService bindingsService;

    @Autowired
    private OidcUserService oidcUserService;

    /**
     * List system users.
     * Returns users who have system-level role bindings.
     */
    public UserList listSystemUsers(Integer page, Integer perPage, String query) {
        Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("username").ascending());
        
        Page<UserRoleBindingViewEntity> resultPage;
        if (query != null && !query.isEmpty()) {
            // Search by username or email
            String searchPattern = "%" + query.toLowerCase() + "%";
            resultPage = userRoleBindingViewRepository.findByScopeTypeAndScopeIdAndUsernameOrEmail(
                "SYSTEM", UUID.fromString(Constants.SYSTEM_ID), searchPattern, pageable);
        } else {
            resultPage = userRoleBindingViewRepository.findByScopeTypeAndScopeId(
                "SYSTEM", UUID.fromString(Constants.SYSTEM_ID), pageable);
        }

        UserList response = new UserList();
        response.setItems(resultPage.getContent().stream()
                .map(this::mapViewEntityToApi)
                .collect(Collectors.toList()));
        response.setTotal((int) resultPage.getTotalElements());
        response.setPage(page);
        response.setPerPage(perPage);
        return response;
    }

    /**
     * List tenant users.
     * Returns users who have tenant-level role bindings for the specified tenant.
     */
    public UserList listTenantUsers(UUID tenantId, Integer page, Integer perPage, String query) {
        Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("username").ascending());
        
        Page<UserRoleBindingViewEntity> resultPage;
        if (query != null && !query.isEmpty()) {
            // Search by username or email within tenant
            String searchPattern = "%" + query.toLowerCase() + "%";
            resultPage = userRoleBindingViewRepository.findByScopeTypeAndScopeIdAndUsernameOrEmail(
                "TENANT", tenantId, searchPattern, pageable);
        } else {
            resultPage = userRoleBindingViewRepository.findByScopeTypeAndScopeId(
                "TENANT", tenantId, pageable);
        }

        UserList response = new UserList();
        response.setItems(resultPage.getContent().stream()
                .map(this::mapViewEntityToApi)
                .collect(Collectors.toList()));
        response.setTotal((int) resultPage.getTotalElements());
        response.setPage(page);
        response.setPerPage(perPage);
        return response;
    }

    /**
     * List IDP users from Keycloak.
     * This is a discovery endpoint - users are NOT stored in the database yet.
     */
    public OidcUserList listIdpUsers(UUID idpId, Integer page, Integer perPage, String query) {
        IdentityProviderEntity idp = identityProviderRepository.findById(idpId)
                .orElseThrow(() -> new RuntimeException("Identity provider not found: " + idpId));

        if (!(idp instanceof OidcIdentityProviderEntity)) {
            throw new RuntimeException("Identity provider is not an OIDC provider: " + idpId);
        }

        OidcIdentityProviderEntity oidcIdp = (OidcIdentityProviderEntity) idp;
        OidcMetadata oidcMetadata = oidcIdp.getOidcMetadata();
        if (oidcMetadata == null || oidcMetadata.getClientId() == null || oidcMetadata.getClientSecret() == null) {
            throw new RuntimeException("Identity provider credentials not configured for user lookup");
        }

        // Decrypt client secret before using it
        String decryptedClientSecret = EncryptionUtil.decrypt(oidcMetadata.getClientSecret());

        List<OidcUserInfo> oidcUsers = oidcUserService.getUsers(
                oidcMetadata.getIssuerUri(), oidcMetadata.getClientId(), decryptedClientSecret);

        // Filter by query if provided
        List<OidcUserInfo> filteredUsers;
        if (query != null && !query.isEmpty()) {
            String searchPattern = query.toLowerCase();
            filteredUsers = oidcUsers.stream()
                    .filter(u -> (u.getPreferredUsername() != null && u.getPreferredUsername().toLowerCase().contains(searchPattern)) ||
                            (u.getEmail() != null && u.getEmail().toLowerCase().contains(searchPattern)))
                    .collect(Collectors.toList());
        } else {
            filteredUsers = oidcUsers;
        }

        // Pagination
        int fromIndex = (page - 1) * perPage;
        int toIndex = Math.min(fromIndex + perPage, filteredUsers.size());
        List<OidcUserInfo> paginatedUsers = filteredUsers.subList(fromIndex, toIndex);

        OidcUserList response = new OidcUserList();
        response.setItems(paginatedUsers.stream()
                .map(this::mapOidcUserToApi)
                .collect(Collectors.toList()));
        response.setTotal(filteredUsers.size());
        response.setPage(page);
        response.setPerPage(perPage);
        return response;
    }

    /**
     * Add users to system.
     * Creates idp_user entries (if needed) and system-level role bindings.
     */
    @Transactional
    public RoleBindingList addSystemUsers(RoleBindingBulkCreate request) {
        // Ensure all bindings have SYSTEM scope
        for (var binding : request.getBindings()) {
            if (!RoleBindingCreateItem.ScopeTypeEnum.SYSTEM.equals(binding.getScopeType())) {
                throw new RuntimeException("System user addition requires SYSTEM scope type");
            }
            if (binding.getScopeId() != null) {
                throw new RuntimeException("System scope must have null scope_id");
            }
        }

        // Create idp_user entries for users that don't exist
        for (var binding : request.getBindings()) {
            if (RoleBindingCreateItem.SubjectTypeEnum.USER.equals(binding.getSubjectType())) {
                createIdpUserIfNotExists(binding.getSubjectId(), binding.getRoleId());
            }
        }

        // Use BindingsService to create role bindings
        return bindingsService.bulkCreateRoleBindings(request);
    }

    /**
     * Add users to tenant.
     * Creates idp_user entries (if needed) and tenant-level role bindings.
     */
    @Transactional
    public RoleBindingList addTenantUsers(UUID tenantId, RoleBindingBulkCreate request) {
        // Ensure all bindings have TENANT scope with correct tenantId
        for (var binding : request.getBindings()) {
            if (!"TENANT".equals(binding.getScopeType())) {
                throw new RuntimeException("Tenant user addition requires TENANT scope type");
            }
            if (!tenantId.equals(binding.getScopeId())) {
                throw new RuntimeException("Tenant scope must match tenantId parameter");
            }
        }

        // Create idp_user entries for users that don't exist
        for (var binding : request.getBindings()) {
            if ("USER".equals(binding.getSubjectType())) {
                createIdpUserIfNotExists(binding.getSubjectId(), binding.getRoleId());
            }
        }

        // Use BindingsService to create role bindings
        return bindingsService.bulkCreateRoleBindings(request);
    }

    /**
     * Delete system user.
     * Removes user from system by deleting their system-level role bindings.
     */
    @Transactional
    public void deleteSystemUser(UUID userId) {
        List<RoleBindingEntity> bindings = roleBindingRepository.findBySubjectTypeAndSubjectId(
                RoleBindingSubjectType.USER, userId.toString());

        // Filter for SYSTEM scope only
        List<RoleBindingEntity> systemBindings = bindings.stream()
                .filter(b -> RoleScopeType.SYSTEM.equals(b.getScopeType()))
                .collect(Collectors.toList());

        if (systemBindings.isEmpty()) {
            throw new RuntimeException("No system role bindings found for user: " + userId);
        }

        roleBindingRepository.deleteAll(systemBindings);
    }

    /**
     * Delete tenant user.
     * Removes user from tenant by deleting their tenant-level role bindings.
     */
    @Transactional
    public void deleteTenantUser(UUID tenantId, UUID userId) {
        List<RoleBindingEntity> bindings = roleBindingRepository.findBySubjectTypeAndSubjectId(
                RoleBindingSubjectType.USER, userId.toString());

        // Filter for TENANT scope with matching tenantId
        List<RoleBindingEntity> tenantBindings = bindings.stream()
                .filter(b -> RoleScopeType.TENANT.equals(b.getScopeType()) &&
                             tenantId.equals(b.getScopeId()))
                .collect(Collectors.toList());

        if (tenantBindings.isEmpty()) {
            throw new RuntimeException("No tenant role bindings found for user: " + userId + " in tenant: " + tenantId);
        }

        roleBindingRepository.deleteAll(tenantBindings);
    }

    /**
     * Update system user role.
     * Updates user's role within the system.
     */
    @Transactional
    public RoleBinding updateSystemUserRole(UUID userId, RoleUpdateRequest request) {
        List<RoleBindingEntity> bindings = roleBindingRepository.findBySubjectTypeAndSubjectId(
                RoleBindingSubjectType.USER, userId.toString());

        // Find SYSTEM scope binding
        RoleBindingEntity systemBinding = bindings.stream()
                .filter(b -> RoleScopeType.SYSTEM.equals(b.getScopeType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No system role binding found for user: " + userId));

        // Find the new role (system roles have null scope_id)
        var role = roleRepository.findByNameAndScopeIdIsNull(request.getRoleName());
        if (role == null) {
            throw new RuntimeException("Role not found: " + request.getRoleName());
        }

        // Update the role
        systemBinding.setRole(role);
        if (request.getExpiresAt() != null) {
            systemBinding.setExpiresAt(request.getExpiresAt().toInstant());
        } else {
            systemBinding.setExpiresAt(null);
        }

        RoleBindingEntity saved = roleBindingRepository.save(systemBinding);
        return bindingsService.getRoleBinding(saved.getId());
    }

    /**
     * Update tenant user role.
     * Updates user's role within a tenant.
     */
    @Transactional
    public RoleBinding updateTenantUserRole(UUID tenantId, UUID userId, RoleUpdateRequest request) {
        List<RoleBindingEntity> bindings = roleBindingRepository.findBySubjectTypeAndSubjectId(
                RoleBindingSubjectType.USER, userId.toString());

        // Find TENANT scope binding for this tenant
        RoleBindingEntity tenantBinding = bindings.stream()
                .filter(b -> RoleScopeType.TENANT.equals(b.getScopeType()) &&
                             tenantId.equals(b.getScopeId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No tenant role binding found for user: " + userId + " in tenant: " + tenantId));

        // Find the new role (tenant roles have specific scope_id)
        var role = roleRepository.findByNameAndScopeId(request.getRoleName(), tenantId);
        if (role == null) {
            throw new RuntimeException("Role not found: " + request.getRoleName());
        }

        // Update the role
        tenantBinding.setRole(role);
        if (request.getExpiresAt() != null) {
            tenantBinding.setExpiresAt(request.getExpiresAt().toInstant());
        } else {
            tenantBinding.setExpiresAt(null);
        }

        RoleBindingEntity saved = roleBindingRepository.save(tenantBinding);
        return bindingsService.getRoleBinding(saved.getId());
    }

    /**
     * Create idp_user entry if it doesn't exist.
     * This is needed when adding users from Keycloak to system/tenant.
     */
    private void createIdpUserIfNotExists(String externalId, UUID roleId) {
        // Find the role to get the identity provider
        var role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleId));

        // Check if idp_user already exists
        List<IdpUserEntity> existingUsers = idpUserRepository.findAll().stream()
                .filter(u -> externalId.equals(u.getExternalId()))
                .toList();

        if (!existingUsers.isEmpty()) {
            // User already exists, no need to create
            return;
        }

        // Get the identity provider from the role
        // For now, we'll use the first available OIDC provider
        IdentityProviderEntity idp = identityProviderRepository.findAll().stream()
                .filter(i -> i.getProtocol() != null && i.getProtocol().name().equals("OIDC"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No OIDC identity provider found"));

        if (!(idp instanceof OidcIdentityProviderEntity)) {
            throw new RuntimeException("Identity provider is not an OIDC provider");
        }

        OidcIdentityProviderEntity oidcIdp = (OidcIdentityProviderEntity) idp;
        OidcMetadata oidcMetadata = oidcIdp.getOidcMetadata();
        if (oidcMetadata == null || oidcMetadata.getClientId() == null || oidcMetadata.getClientSecret() == null) {
            throw new RuntimeException("Identity provider credentials not configured for user lookup");
        }

        // Decrypt client secret before using it
        String decryptedClientSecret = EncryptionUtil.decrypt(oidcMetadata.getClientSecret());

        // Fetch user info from Keycloak to populate the idp_user entry
        OidcUserInfo userInfo = oidcUserService.getUserByExternalId(
                externalId, oidcMetadata.getIssuerUri(), oidcMetadata.getClientId(), decryptedClientSecret);

        if (userInfo == null) {
            throw new RuntimeException("User not found in Keycloak: " + externalId);
        }

        // Create idp_user entry
        IdpUserEntity idpUser = new IdpUserEntity();
        idpUser.setId(UUID.randomUUID());
        idpUser.setExternalId(userInfo.getSub());
        idpUser.setIdentityProviderId(idp.getId());
        idpUser.setUsername(userInfo.getPreferredUsername());
        idpUser.setEmail(userInfo.getEmail());
        idpUser.setDisplayName(userInfo.getName());
        idpUser.setCreatedAt(Instant.now());
        idpUser.setUpdatedAt(Instant.now());

        idpUserRepository.save(idpUser);
    }

    /**
     * Map UserRoleBindingViewEntity to API User model.
     */
    private User mapViewEntityToApi(UserRoleBindingViewEntity entity) {
        User user = new User();
        user.setUserId(entity.getUserId());
        user.setExternalId(entity.getExternalId());
        user.setIdentityProviderId(UUID.fromString(entity.getIdentityProviderId()));
        user.setUsername(entity.getUsername());
        user.setEmail(entity.getEmail());
        user.setDisplayName(entity.getDisplayName());
        user.setUserMetadata(entity.getUserMetadata());
        user.setUserCreatedAt(entity.getUserCreatedAt().atOffset(ZoneOffset.UTC));
        user.setUserUpdatedAt(entity.getUserUpdatedAt().atOffset(ZoneOffset.UTC));
        user.setRoleId(entity.getRoleId());
        user.setRoleName(entity.getRoleName());
        user.setScopeType(entity.getScopeType());
        user.setScopeId(entity.getScopeId());
        user.setExpiresAt(entity.getExpiresAt() != null ? entity.getExpiresAt().atOffset(ZoneOffset.UTC) : null);
        return user;
    }

    /**
     * Map OidcUserInfo to API OidcUser model.
     */
    private OidcUser mapOidcUserToApi(OidcUserInfo userInfo) {
        OidcUser user = new OidcUser();
        user.setSub(userInfo.getSub());
        user.setPreferredUsername(userInfo.getPreferredUsername());
        user.setEmail(userInfo.getEmail());
        user.setName(userInfo.getName());
        return user;
    }
}
