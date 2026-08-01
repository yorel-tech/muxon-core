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
package com.yorel.muxon.security;

import com.yorel.muxon.auth.UserPrincipal;
import com.yorel.muxon.db.repository.UserRoleBindingViewRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtUserPrincipalConverter implements Converter<Jwt, UserPrincipalAuthenticationToken> {

  private final UserRoleBindingViewRepository userRoleRepo;

  public JwtUserPrincipalConverter(UserRoleBindingViewRepository userRoleRepo) {
    this.userRoleRepo = userRoleRepo;
  }

  @Override
  public UserPrincipalAuthenticationToken convert(Jwt jwt) {
    // Extract user ID from JWT (typically 'sub' claim)
    String userId = jwt.getSubject();
    String username = jwt.getClaimAsString("preferred_username");
    if (username == null) {
      username = jwt.getClaimAsString("email");
    }
    if (username == null) {
      username = userId; // fallback
    }

    // Get user roles from database based on external ID
    List<String> roles =
        userRoleRepo.findByExternalId(userId).stream()
            .map(binding -> binding.getRoleName())
            .distinct()
            .collect(Collectors.toList());

    // Create UserPrincipal
    UserPrincipal userPrincipal = new UserPrincipal(userId, username, roles);

    // Return custom authentication token with UserPrincipal as principal
    // Authorities list is empty since we handle authorization separately
    return new UserPrincipalAuthenticationToken(
        jwt, new ArrayList<GrantedAuthority>(), userPrincipal);
  }
}
