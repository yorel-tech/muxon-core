package com.scal.muxon.controllers;

import com.scal.muxon.services.plugin.UiExtensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/ui")
public class UiExtensionsController {

    @Autowired private UiExtensionService uiExtensionService;

    @GetMapping("/extensions")
    public ResponseEntity<Map<String, Object>> listUiExtensions(
            @RequestParam UUID tenantId,
            Authentication authentication) {

        Set<String> userPermissions = resolvePermissions(authentication);
        List<UiExtensionService.UiModuleEntry> modules =
                uiExtensionService.listExtensionsForUser(tenantId, userPermissions);

        return ResponseEntity.ok(Map.of("modules", modules));
    }

    private Set<String> resolvePermissions(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("PERMISSION_", "").replace("_", ":").toLowerCase())
                .collect(Collectors.toSet());
    }
}
