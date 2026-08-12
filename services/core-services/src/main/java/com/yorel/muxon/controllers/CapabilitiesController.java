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

import com.yorel.muxon.auth.RoleRegistry;
import com.yorel.muxon.auth.UserPrincipal;
import com.yorel.muxon.db.repository.UserRoleBindingViewRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class CapabilitiesController {

  private final UserRoleBindingViewRepository userRoleBindingViewRepository;
  private final RoleRegistry roleRegistry;

  public CapabilitiesController(
      UserRoleBindingViewRepository userRoleBindingViewRepository, RoleRegistry roleRegistry) {
    this.userRoleBindingViewRepository = userRoleBindingViewRepository;
    this.roleRegistry = roleRegistry;
  }

  @GetMapping("/capabilities")
  public ResponseEntity<Object> getMyCapabilities() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
      return ResponseEntity.status(401).build();
    }

    List<String> roleNames =
        userRoleBindingViewRepository.findByExternalId(principal.id()).stream()
            .map(b -> b.getRoleName())
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    return ResponseEntity.ok(roleRegistry.getCapabilitiesForRoles(roleNames));
  }
}
