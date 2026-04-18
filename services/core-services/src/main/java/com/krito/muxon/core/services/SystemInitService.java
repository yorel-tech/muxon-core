package com.krito.muxon.core.services;

import com.krito.muxon.api.dto.AddSystemAdminRequest;
import com.krito.muxon.api.dto.BootstrapStatusDto;
import com.krito.muxon.api.enums.BootstrapStatus;
import com.krito.muxon.api.enums.RoleBindingSubjectType;
import com.krito.muxon.api.enums.RoleName;
import com.krito.muxon.api.enums.RoleScopeType;
import com.krito.muxon.core.auth.Permission;
import com.krito.muxon.core.auth.RequiresPermission;
import com.krito.muxon.db.model.IdpUserEntity;
import com.krito.muxon.db.model.IdentityProviderEntity;
import com.krito.muxon.db.model.RoleBindingEntity;
import com.krito.muxon.db.model.TenantEntity;
import com.krito.muxon.db.repository.IdpUserRepository;
import com.krito.muxon.db.repository.IdentityProviderRepository;
import com.krito.muxon.db.repository.RoleBindingRepository;
import com.krito.muxon.db.repository.SystemInitRepository;
import com.krito.muxon.db.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing system initialization status and bootstrap-related operations.
 * Provides methods to check bootstrap status and retrieve pre-configured bootstrap data.
 * Uses caching strategy for one-time operations to improve performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemInitService {

    private static final String BOOTSTRAP_STATUS_KEY = "bootstrap_status";

    private final SystemInitRepository systemInitRepository;
    private final IdentityProviderRepository idpRepository;
    private final IdpUserRepository idpUserRepository;
    private final RoleBindingRepository roleBindingRepository;
    private final TenantRepository tenantRepository;

    /** Matches legacy system-admin binding scope_id used when roles lived in DB. */
    private static final UUID SYSTEM_ADMIN_BINDING_SCOPE_ID = UUID.fromString("215012d9-8b1e-5dc5-b54f-89022875fe1e");

    /**
     * Get the current bootstrap status from system_init table.
     * This is a one-time cached operation.
     *
     * @return BootstrapStatusDto containing only the system status
     */
    public BootstrapStatusDto getBootstrapStatus() {
        return systemInitRepository.getBootstrapStatus()
                .map(BootstrapStatusDto::of)
                .orElse(BootstrapStatusDto.of(BootstrapStatus.NOTREADY));
    }

    /**
     * Check if IDP has been configured (system IDP with is_system=true).
     * This is a one-time cached operation.
     *
     * @return true if system IDP exists, false otherwise
     */
    public boolean checkIdpStatus() {
        return systemInitRepository.getBootstrapStatus()
                .map(status -> status.equals(BootstrapStatus.BOOTSTRAPPED) || status.equals(BootstrapStatus.READY))
                .orElse(false);
    }

    /**
     * Check if system admin users exist (users with system:admin role).
     * This is a one-time cached operation.
     *
     * @return true if at least one system admin user exists, false otherwise
     */
    public boolean checkSystemAdminUsers() {
        return systemInitRepository.getBootstrapStatus()
                .map(status -> status.equals(BootstrapStatus.BOOTSTRAPPED) || status.equals(BootstrapStatus.READY))
                .flatMap(status -> {
                    if (!status) {
                        return Optional.empty();
                    }
                    List<RoleBindingEntity> adminRoleBindings = roleBindingRepository
                            .findByRoleName(RoleName.SYSTEM_ADMIN.getValue())
                            .stream()
                            .filter(rb -> rb.getSubjectType() == RoleBindingSubjectType.USER
                                    && rb.getScopeType() == RoleScopeType.SYSTEM)
                            .toList();
                    return Optional.of(!adminRoleBindings.isEmpty());
                })
                .orElse(false);
    }

    /**
     * Check if tenant has been configured.
     * This is a one-time cached operation.
     *
     * @return true if tenant exists, false otherwise
     */
    public boolean checkTenantStatus() {
        return systemInitRepository.getBootstrapStatus()
                .map(status -> status.equals(BootstrapStatus.BOOTSTRAPPED) || status.equals(BootstrapStatus.READY))
                .flatMap(_ -> {
                    List<TenantEntity> tenants = tenantRepository.findAll();
                    return Optional.of(!tenants.isEmpty());
                })
                .orElse(false);
    }

    /**
     * Get system IDP settings for the protected /api/system/init/idpsettings endpoint.
     * Returns the system IDP if it exists (is_system=true).
     *
     * @return IdentityProviderEntity if found, empty otherwise
     */
    public IdentityProviderEntity getSystemIdp() {
        List<IdentityProviderEntity> systemProviders = idpRepository.findSystemProvider();
        return systemProviders.isEmpty() ? null : systemProviders.getFirst();
    }

    /**
     * Get system admin users for the protected /api/system/init/system-admin endpoint.
     * Returns all users with system:admin role.
     *
     * @return List of IdpUserEntity with system:admin role
     */
    public List<IdpUserEntity> getSystemAdminUsers() {
        Optional<BootstrapStatus> statusOpt = systemInitRepository.getBootstrapStatus();
        if (statusOpt.isEmpty()) {
            return List.of();
        }
        BootstrapStatus status = statusOpt.get();
        
        if (status.equals(BootstrapStatus.NOTREADY)) {
            return List.of();
        }
        
        List<RoleBindingEntity> adminRoleBindings = roleBindingRepository
                .findByRoleName(RoleName.SYSTEM_ADMIN.getValue())
                .stream()
                .filter(rb -> rb.getSubjectType() == RoleBindingSubjectType.USER
                        && rb.getScopeType() == RoleScopeType.SYSTEM)
                .toList();
        if (adminRoleBindings.isEmpty()) {
            return List.of();
        }
        
        // Find users by subjectId from role bindings
        List<UUID> userIds = adminRoleBindings.stream()
                .map(RoleBindingEntity::getSubjectId)
                .map(UUID::fromString)
                .toList();
        return userIds.stream()
                .map(idpUserRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * Get tenant for the protected /api/system/init/tenant endpoint.
     * Returns the tenant if it exists.
     *
     * @return TenantEntity if found, null otherwise
     */
    public TenantEntity getTenant() {
        List<TenantEntity> tenants = tenantRepository.findAll();
        return tenants.isEmpty() ? null : tenants.getFirst();
    }

    /**
     * Add a new system admin user with system:admin role.
     * This can be called when bootstrap has been performed and additional admins need to be added.

     * Note: This implementation assumes users are managed by an external IDP.
     * The user must already exist in the external IDP before calling this method.
     * The password field in the request is ignored as authentication is handled by the IDP.
     *
     * @param request DTO containing username, email, password, first name, last name
     * @return the created IdpUserEntity
     */
    @Transactional
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public IdpUserEntity addSystemAdminUser(AddSystemAdminRequest request) {
        // Find system IDP
        List<IdentityProviderEntity> systemProviders = idpRepository.findSystemProvider();
        if (systemProviders.isEmpty()) {
            throw new IllegalStateException("System IDP not found. Please run muxon-initializer first.");
        }
        UUID systemIdpId = systemProviders.getFirst().getId();

        // Check if user already exists
        Optional<IdpUserEntity> existingUser = idpUserRepository.findAll().stream()
                .filter(u -> u.getUsername().equals(request.getUsername())
                        && u.getIdentityProviderId().equals(systemIdpId))
                .findFirst();

        IdpUserEntity userEntity;
        if (existingUser.isPresent()) {
            userEntity = existingUser.get();
            log.info("User already exists, updating: {}", userEntity.getUsername());
        } else {
            // Create new IDP user
            userEntity = new IdpUserEntity();
            userEntity.setId(UUID.randomUUID());
            userEntity.setIdentityProviderId(systemIdpId);
            userEntity.setExternalId(request.getUsername()); // Use username as externalId for local IDP
            userEntity.setCreatedAt(Instant.now());
            log.info("Creating new user: {}", request.getUsername());
        }

        // Update fields
        userEntity.setUsername(request.getUsername());
        userEntity.setEmail(request.getEmail());
        // Combine first and last name for display name
        String displayName = request.getFirstName() + " " + request.getLastName();
        userEntity.setDisplayName(displayName);
        userEntity.setUpdatedAt(Instant.now());

        // Save user
        IdpUserEntity savedUser = idpUserRepository.save(userEntity);

        // Create role binding if it doesn't exist
        Optional<RoleBindingEntity> existingBinding =
                roleBindingRepository.findBySubjectTypeAndSubjectId(RoleBindingSubjectType.USER, savedUser.getId().toString())
                        .stream()
                        .filter(rb -> RoleName.SYSTEM_ADMIN.getValue().equals(rb.getRoleName())
                                && rb.getScopeType() == RoleScopeType.SYSTEM)
                        .findFirst();

        if (existingBinding.isEmpty()) {
            RoleBindingEntity roleBinding = new RoleBindingEntity();
            roleBinding.setId(UUID.randomUUID());
            roleBinding.setRoleName(RoleName.SYSTEM_ADMIN.getValue());
            roleBinding.setSubjectType(RoleBindingSubjectType.USER);
            roleBinding.setSubjectId(savedUser.getId().toString());
            roleBinding.setScopeType(RoleScopeType.SYSTEM);
            roleBinding.setScopeId(SYSTEM_ADMIN_BINDING_SCOPE_ID);
            roleBinding.setCreatedAt(Instant.now());
            roleBindingRepository.save(roleBinding);
            log.info("Created role binding for user {} with system:admin role", savedUser.getUsername());
        }

        return savedUser;
    }

    /**
     * Mark bootstrap as completed (READY).
     * This is called after all required setup steps are completed.
     *
     * @return number of rows updated
     */
    @Transactional
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public int markBootstrapAsReady() {
        return systemInitRepository.markAsReady();
    }
}
