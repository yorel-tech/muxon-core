/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.services;

import com.yorel.muxon.api.enums.RoleBindingSubjectType;
import com.yorel.muxon.api.enums.RoleScopeType;
import com.yorel.muxon.api.model.OidcUser;
import com.yorel.muxon.api.model.OidcUserList;
import com.yorel.muxon.api.model.RoleBinding;
import com.yorel.muxon.api.model.RoleBindingBulkCreate;
import com.yorel.muxon.api.model.RoleBindingCreateItem;
import com.yorel.muxon.api.model.RoleBindingList;
import com.yorel.muxon.api.model.RoleUpdateRequest;
import com.yorel.muxon.api.model.User;
import com.yorel.muxon.api.model.UserList;
import com.yorel.muxon.auth.AuthorizationService;
import com.yorel.muxon.auth.UserPrincipal;
import com.yorel.muxon.common.Constants;
import com.yorel.muxon.common.EncryptionUtil;
import com.yorel.muxon.common.UuidUtils;
import com.yorel.muxon.db.model.IdentityProviderEntity;
import com.yorel.muxon.db.model.IdpUserEntity;
import com.yorel.muxon.db.model.OidcIdentityProviderEntity;
import com.yorel.muxon.db.model.OidcMetadata;
import com.yorel.muxon.db.model.RoleBindingEntity;
import com.yorel.muxon.db.model.UserRoleBindingViewEntity;
import com.yorel.muxon.db.repository.IdentityProviderRepository;
import com.yorel.muxon.db.repository.IdpUserRepository;
import com.yorel.muxon.db.repository.RoleBindingRepository;
import com.yorel.muxon.db.repository.UserRoleBindingViewRepository;
import com.yorel.muxon.services.model.OidcUserInfo;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for user management operations. Handles listing, adding, updating, and deleting users for
 * system and tenant scopes.
 */
@Service
public class UsersService {

  @Autowired private UserRoleBindingViewRepository userRoleBindingViewRepository;

  @Autowired private IdpUserRepository idpUserRepository;

  @Autowired private IdentityProviderRepository identityProviderRepository;

  @Autowired private RoleBindingRepository roleBindingRepository;

  @Autowired private AuthorizationService authorizationService;

  @Autowired private OidcUserService oidcUserService;

  /** List system users. Returns users who have system-level role bindings. */
  public UserList listSystemUsers(Integer page, Integer perPage, String query) {
    Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("username").ascending());

    Page<UserRoleBindingViewEntity> resultPage;
    if (query != null && !query.isEmpty()) {
      // Search by username or email
      String searchPattern = "%" + query.toLowerCase() + "%";
      resultPage =
          userRoleBindingViewRepository.findByScopeTypeAndScopeIdAndUsernameOrEmail(
              "SYSTEM", UUID.fromString(Constants.SYSTEM_ID), searchPattern, pageable);
    } else {
      resultPage =
          userRoleBindingViewRepository.findByScopeTypeAndScopeId(
              "SYSTEM", UUID.fromString(Constants.SYSTEM_ID), pageable);
    }

    UserList response = new UserList();
    response.setItems(
        resultPage.getContent().stream()
            .map(this::mapViewEntityToApi)
            .collect(Collectors.toList()));
    response.setTotal((int) resultPage.getTotalElements());
    response.setPage(page);
    response.setPerPage(perPage);
    return response;
  }

  /**
   * List tenant users. Returns users who have tenant-level role bindings for the specified tenant.
   */
  public UserList listTenantUsers(UUID tenantId, Integer page, Integer perPage, String query) {
    Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("username").ascending());

    Page<UserRoleBindingViewEntity> resultPage;
    if (query != null && !query.isEmpty()) {
      // Search by username or email within tenant
      String searchPattern = "%" + query.toLowerCase() + "%";
      resultPage =
          userRoleBindingViewRepository.findByScopeTypeAndScopeIdAndUsernameOrEmail(
              "TENANT", tenantId, searchPattern, pageable);
    } else {
      resultPage =
          userRoleBindingViewRepository.findByScopeTypeAndScopeId("TENANT", tenantId, pageable);
    }

    UserList response = new UserList();
    response.setItems(
        resultPage.getContent().stream()
            .map(this::mapViewEntityToApi)
            .collect(Collectors.toList()));
    response.setTotal((int) resultPage.getTotalElements());
    response.setPage(page);
    response.setPerPage(perPage);
    return response;
  }

  /**
   * List IDP users from Keycloak. This is a discovery endpoint - users are NOT stored in the
   * database yet.
   */
  public OidcUserList listIdpUsers(UUID idpId, Integer page, Integer perPage, String query) {
    IdentityProviderEntity idp =
        identityProviderRepository
            .findById(idpId)
            .orElseThrow(() -> new RuntimeException("Identity provider not found: " + idpId));

    if (!(idp instanceof OidcIdentityProviderEntity)) {
      throw new RuntimeException("Identity provider is not an OIDC provider: " + idpId);
    }

    OidcIdentityProviderEntity oidcIdp = (OidcIdentityProviderEntity) idp;
    OidcMetadata oidcMetadata = oidcIdp.getOidcMetadata();
    if (oidcMetadata == null
        || oidcMetadata.getClientId() == null
        || oidcMetadata.getClientSecret() == null) {
      throw new RuntimeException("Identity provider credentials not configured for user lookup");
    }

    // Decrypt client secret before using it
    String decryptedClientSecret = EncryptionUtil.decrypt(oidcMetadata.getClientSecret());

    List<OidcUserInfo> oidcUsers =
        oidcUserService.getUsers(
            oidcMetadata.getIssuerUri(), oidcMetadata.getClientId(), decryptedClientSecret);

    // Filter by query if provided
    List<OidcUserInfo> filteredUsers;
    if (query != null && !query.isEmpty()) {
      String searchPattern = query.toLowerCase();
      filteredUsers =
          oidcUsers.stream()
              .filter(
                  u ->
                      (u.getPreferredUsername() != null
                              && u.getPreferredUsername().toLowerCase().contains(searchPattern))
                          || (u.getEmail() != null
                              && u.getEmail().toLowerCase().contains(searchPattern)))
              .collect(Collectors.toList());
    } else {
      filteredUsers = oidcUsers;
    }

    // Pagination
    int fromIndex = (page - 1) * perPage;
    int toIndex = Math.min(fromIndex + perPage, filteredUsers.size());
    List<OidcUserInfo> paginatedUsers = filteredUsers.subList(fromIndex, toIndex);

    OidcUserList response = new OidcUserList();
    response.setItems(
        paginatedUsers.stream().map(this::mapOidcUserToApi).collect(Collectors.toList()));
    response.setTotal(filteredUsers.size());
    response.setPage(page);
    response.setPerPage(perPage);
    return response;
  }

  /** Add users to system. Creates idp_user entries (if needed) and system-level role bindings. */
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
        createIdpUserIfNotExists(binding.getSubjectId());
      }
    }

    return persistBulkRoleBindings(request);
  }

  /** Add users to tenant. Creates idp_user entries (if needed) and tenant-level role bindings. */
  @Transactional
  public RoleBindingList addTenantUsers(UUID tenantId, RoleBindingBulkCreate request) {
    // Ensure all bindings have TENANT scope with correct tenantId
    for (var binding : request.getBindings()) {
      if (!RoleBindingCreateItem.ScopeTypeEnum.TENANT.equals(binding.getScopeType())) {
        throw new RuntimeException("Tenant user addition requires TENANT scope type");
      }
      if (!tenantId.equals(binding.getScopeId())) {
        throw new RuntimeException("Tenant scope must match tenantId parameter");
      }
    }

    // Create idp_user entries for users that don't exist
    for (var binding : request.getBindings()) {
      if (RoleBindingCreateItem.SubjectTypeEnum.USER.equals(binding.getSubjectType())) {
        createIdpUserIfNotExists(binding.getSubjectId());
      }
    }

    return persistBulkRoleBindings(request);
  }

  /** Delete system user. Removes user from system by deleting their system-level role bindings. */
  @Transactional
  public void deleteSystemUser(UUID userId) {
    List<RoleBindingEntity> bindings =
        roleBindingRepository.findBySubjectTypeAndSubjectId(
            RoleBindingSubjectType.USER, userId.toString());

    // Filter for SYSTEM scope only
    List<RoleBindingEntity> systemBindings =
        bindings.stream()
            .filter(b -> RoleScopeType.SYSTEM.equals(b.getScopeType()))
            .collect(Collectors.toList());

    if (systemBindings.isEmpty()) {
      throw new RuntimeException("No system role bindings found for user: " + userId);
    }

    roleBindingRepository.deleteAll(systemBindings);
    authorizationService.evictCacheForUser(userId.toString());
  }

  /** Delete tenant user. Removes user from tenant by deleting their tenant-level role bindings. */
  @Transactional
  public void deleteTenantUser(UUID tenantId, UUID userId) {
    List<RoleBindingEntity> bindings =
        roleBindingRepository.findBySubjectTypeAndSubjectId(
            RoleBindingSubjectType.USER, userId.toString());

    // Filter for TENANT scope with matching tenantId
    List<RoleBindingEntity> tenantBindings =
        bindings.stream()
            .filter(
                b ->
                    RoleScopeType.TENANT.equals(b.getScopeType())
                        && tenantId.equals(b.getScopeId()))
            .collect(Collectors.toList());

    if (tenantBindings.isEmpty()) {
      throw new RuntimeException(
          "No tenant role bindings found for user: " + userId + " in tenant: " + tenantId);
    }

    roleBindingRepository.deleteAll(tenantBindings);
    authorizationService.evictCacheForUser(userId.toString());
  }

  /** Update system user role. Updates user's role within the system. */
  @Transactional
  public RoleBinding updateSystemUserRole(UUID userId, RoleUpdateRequest request) {
    List<RoleBindingEntity> bindings =
        roleBindingRepository.findBySubjectTypeAndSubjectId(
            RoleBindingSubjectType.USER, userId.toString());

    // Find SYSTEM scope binding
    RoleBindingEntity systemBinding =
        bindings.stream()
            .filter(b -> RoleScopeType.SYSTEM.equals(b.getScopeType()))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException("No system role binding found for user: " + userId));

    systemBinding.setRoleName(request.getRoleName());
    if (request.getExpiresAt() != null) {
      systemBinding.setExpiresAt(request.getExpiresAt().toInstant());
    } else {
      systemBinding.setExpiresAt(null);
    }

    RoleBindingEntity saved = roleBindingRepository.save(systemBinding);
    authorizationService.evictCacheForUser(userId.toString());
    return mapRoleBindingEntityToApi(saved);
  }

  /** Update tenant user role. Updates user's role within a tenant. */
  @Transactional
  public RoleBinding updateTenantUserRole(UUID tenantId, UUID userId, RoleUpdateRequest request) {
    List<RoleBindingEntity> bindings =
        roleBindingRepository.findBySubjectTypeAndSubjectId(
            RoleBindingSubjectType.USER, userId.toString());

    // Find TENANT scope binding for this tenant
    RoleBindingEntity tenantBinding =
        bindings.stream()
            .filter(
                b ->
                    RoleScopeType.TENANT.equals(b.getScopeType())
                        && tenantId.equals(b.getScopeId()))
            .findFirst()
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "No tenant role binding found for user: "
                            + userId
                            + " in tenant: "
                            + tenantId));

    tenantBinding.setRoleName(request.getRoleName());
    if (request.getExpiresAt() != null) {
      tenantBinding.setExpiresAt(request.getExpiresAt().toInstant());
    } else {
      tenantBinding.setExpiresAt(null);
    }

    RoleBindingEntity saved = roleBindingRepository.save(tenantBinding);
    authorizationService.evictCacheForUser(userId.toString());
    return mapRoleBindingEntityToApi(saved);
  }

  private RoleBindingList persistBulkRoleBindings(RoleBindingBulkCreate request) {
    List<RoleBindingEntity> entities = new ArrayList<>();
    UUID createdBy = resolveCurrentUserIdOrNull();
    for (RoleBindingCreateItem item : request.getBindings()) {
      validateBindingCreateItem(item);
      RoleBindingEntity entity = new RoleBindingEntity();
      entity.setId(UUID.randomUUID());
      entity.setRoleName(item.getRoleName());
      entity.setSubjectType(RoleBindingSubjectType.valueOf(item.getSubjectType().name()));
      entity.setSubjectId(item.getSubjectId());
      entity.setScopeType(RoleScopeType.valueOf(item.getScopeType().name()));
      entity.setScopeId(item.getScopeId());
      entity.setExpiresAt(item.getExpiresAt() != null ? item.getExpiresAt().toInstant() : null);
      entity.setCreatedBy(createdBy);
      entity.setCreatedAt(Instant.now());
      entities.add(entity);
    }
    List<RoleBindingEntity> saved = roleBindingRepository.saveAll(entities);
    for (RoleBindingEntity e : saved) {
      if (e.getSubjectType() == RoleBindingSubjectType.USER) {
        authorizationService.evictCacheForUser(e.getSubjectId());
      }
    }
    RoleBindingList result = new RoleBindingList();
    result.setItems(
        saved.stream().map(this::mapRoleBindingEntityToApi).collect(Collectors.toList()));
    result.setTotal(saved.size());
    return result;
  }

  private static void validateBindingCreateItem(RoleBindingCreateItem item) {
    if (RoleBindingCreateItem.SubjectTypeEnum.USER.equals(item.getSubjectType())
        || RoleBindingCreateItem.SubjectTypeEnum.SERVICE_ACCOUNT.equals(item.getSubjectType())) {
      if (!UuidUtils.isValidUUID(item.getSubjectId())) {
        throw new RuntimeException("Invalid subject ID format: must be a valid UUID");
      }
    }
    if (RoleBindingCreateItem.ScopeTypeEnum.TENANT.equals(item.getScopeType())
        && item.getScopeId() == null) {
      throw new RuntimeException("scope_id is required for TENANT scope");
    }
    if (RoleBindingCreateItem.ScopeTypeEnum.SYSTEM.equals(item.getScopeType())
        && item.getScopeId() != null) {
      throw new RuntimeException("scope_id must be null for SYSTEM scope");
    }
  }

  private static UUID resolveCurrentUserIdOrNull() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
      try {
        return UUID.fromString(p.id());
      } catch (IllegalArgumentException ignored) {
        return null;
      }
    }
    return null;
  }

  private RoleBinding mapRoleBindingEntityToApi(RoleBindingEntity entity) {
    RoleBinding b = new RoleBinding();
    b.setId(entity.getId());
    b.setRoleName(entity.getRoleName());
    b.setSubjectType(subjectTypeToApi(entity.getSubjectType()));
    b.setSubjectId(entity.getSubjectId());
    b.setScopeType(scopeTypeToApi(entity.getScopeType()));
    b.setScopeId(entity.getScopeId());
    b.setExpiresAt(
        entity.getExpiresAt() != null ? entity.getExpiresAt().atOffset(ZoneOffset.UTC) : null);
    b.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
    return b;
  }

  private static String subjectTypeToApi(RoleBindingSubjectType subjectType) {
    return switch (subjectType) {
      case USER -> RoleBindingCreateItem.SubjectTypeEnum.USER.getValue();
      case GROUP -> RoleBindingCreateItem.SubjectTypeEnum.GROUP.getValue();
      case SERVICE_ACCOUNT -> RoleBindingCreateItem.SubjectTypeEnum.SERVICE_ACCOUNT.getValue();
    };
  }

  private static String scopeTypeToApi(RoleScopeType scopeType) {
    return switch (scopeType) {
      case SYSTEM -> RoleBindingCreateItem.ScopeTypeEnum.SYSTEM.getValue();
      case TENANT -> RoleBindingCreateItem.ScopeTypeEnum.TENANT.getValue();
      case TENANT_GLOBAL -> RoleBindingCreateItem.ScopeTypeEnum.TENANT_GLOBAL.getValue();
    };
  }

  /**
   * Create idp_user entry if it doesn't exist. This is needed when adding users from Keycloak to
   * system/tenant.
   */
  private void createIdpUserIfNotExists(String externalId) {
    // Check if idp_user already exists
    List<IdpUserEntity> existingUsers =
        idpUserRepository.findAll().stream()
            .filter(u -> externalId.equals(u.getExternalId()))
            .toList();

    if (!existingUsers.isEmpty()) {
      // User already exists, no need to create
      return;
    }

    // Get the identity provider from the role
    // For now, we'll use the first available OIDC provider
    IdentityProviderEntity idp =
        identityProviderRepository.findAll().stream()
            .filter(i -> i.getProtocol() != null && i.getProtocol().name().equals("OIDC"))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No OIDC identity provider found"));

    if (!(idp instanceof OidcIdentityProviderEntity)) {
      throw new RuntimeException("Identity provider is not an OIDC provider");
    }

    OidcIdentityProviderEntity oidcIdp = (OidcIdentityProviderEntity) idp;
    OidcMetadata oidcMetadata = oidcIdp.getOidcMetadata();
    if (oidcMetadata == null
        || oidcMetadata.getClientId() == null
        || oidcMetadata.getClientSecret() == null) {
      throw new RuntimeException("Identity provider credentials not configured for user lookup");
    }

    // Decrypt client secret before using it
    String decryptedClientSecret = EncryptionUtil.decrypt(oidcMetadata.getClientSecret());

    // Fetch user info from Keycloak to populate the idp_user entry
    OidcUserInfo userInfo =
        oidcUserService.getUserByExternalId(
            externalId,
            oidcMetadata.getIssuerUri(),
            oidcMetadata.getClientId(),
            decryptedClientSecret);

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

  /** Map UserRoleBindingViewEntity to API User model. */
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
    user.setRoleName(entity.getRoleName());
    user.setScopeType(entity.getScopeType());
    user.setScopeId(entity.getScopeId());
    user.setExpiresAt(
        entity.getExpiresAt() != null ? entity.getExpiresAt().atOffset(ZoneOffset.UTC) : null);
    return user;
  }

  /** Map OidcUserInfo to API OidcUser model. */
  private OidcUser mapOidcUserToApi(OidcUserInfo userInfo) {
    OidcUser user = new OidcUser();
    user.setSub(userInfo.getSub());
    user.setPreferredUsername(userInfo.getPreferredUsername());
    user.setEmail(userInfo.getEmail());
    user.setName(userInfo.getName());
    return user;
  }
}
