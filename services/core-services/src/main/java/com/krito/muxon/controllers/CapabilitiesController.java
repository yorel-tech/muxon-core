package com.krito.muxon.controllers;

import com.krito.muxon.auth.RoleRegistry;
import com.krito.muxon.auth.UserPrincipal;
import com.krito.muxon.db.repository.UserRoleBindingViewRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/me")
public class CapabilitiesController {

    private final UserRoleBindingViewRepository userRoleBindingViewRepository;
    private final RoleRegistry roleRegistry;

    public CapabilitiesController(UserRoleBindingViewRepository userRoleBindingViewRepository, RoleRegistry roleRegistry) {
        this.userRoleBindingViewRepository = userRoleBindingViewRepository;
        this.roleRegistry = roleRegistry;
    }

    @GetMapping("/capabilities")
    public ResponseEntity<Object> getMyCapabilities() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(401).build();
        }

        List<String> roleNames = userRoleBindingViewRepository.findByExternalId(principal.id()).stream()
                .map(b -> b.getRoleName())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return ResponseEntity.ok(roleRegistry.getCapabilitiesForRoles(roleNames));
    }
}

