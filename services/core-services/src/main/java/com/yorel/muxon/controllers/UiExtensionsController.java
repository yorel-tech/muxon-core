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
package com.yorel.muxon.controllers;

import com.yorel.muxon.services.plugin.UiExtensionService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ui")
public class UiExtensionsController {

  @Autowired private UiExtensionService uiExtensionService;

  @GetMapping("/extensions")
  public ResponseEntity<Map<String, Object>> listUiExtensions(
      @RequestParam UUID tenantId, Authentication authentication) {

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
