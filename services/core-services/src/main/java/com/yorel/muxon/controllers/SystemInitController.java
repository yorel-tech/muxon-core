package com.yorel.muxon.controllers;

import com.yorel.muxon.api.dto.AddSystemAdminRequest;
import com.yorel.muxon.services.SystemInitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Protected controller for system initialization data.
 * Provides endpoints to retrieve pre-configured IDP, system admin users, and tenant.
 * All endpoints require SYSTEM_SETTINGS permission.
 */
@RestController
@RequestMapping("/api/system/init")
@RequiredArgsConstructor
public class SystemInitController {

    private final SystemInitService systemInitService;

    /**
     * Get system IDP settings for the IDP configuration step.
     * Returns the system IDP if it exists (is_system=true), otherwise 404.
     *
     * @return IdentityProviderEntity if found, 404 otherwise
     */
    @GetMapping("/idpsettings")
    @PreAuthorize("hasAuthority('SYSTEM_SETTINGS')")
    public ResponseEntity<?> getSystemIdpSettings() {
        var idp = systemInitService.getSystemIdp();
        if (idp == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(idp);
    }

    /**
     * Get system admin users for the System Users step.
     * Returns list of users with system:admin role.
     *
     * @return List of IdpUserEntity if users exist, empty list otherwise
     */
    @GetMapping("/system-admin")
    @PreAuthorize("hasAuthority('SYSTEM_SETTINGS')")
    public ResponseEntity<List<?>> getSystemAdminUsers() {
        var users = systemInitService.getSystemAdminUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Get tenant for the Tenant step.
     * Returns the tenant if it exists, otherwise 404.
     *
     * @return TenantEntity if found, 404 otherwise
     */
    @GetMapping("/tenant")
    @PreAuthorize("hasAuthority('SYSTEM_SETTINGS')")
    public ResponseEntity<?> getTenant() {
        var tenant = systemInitService.getTenant();
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(tenant);
    }

    /**
     * Add a new system admin user.
     * Creates a new user with system:admin role.
     *
     * @return Created IdpUserEntity
     */
    @PostMapping("/system-admin")
    @PreAuthorize("hasAuthority('SYSTEM_SETTINGS')")
    public ResponseEntity<?> addSystemAdminUser(@RequestBody AddSystemAdminRequest request) {
        var newUser = systemInitService.addSystemAdminUser(request);
        return ResponseEntity.status(201).body(newUser);
    }
}
