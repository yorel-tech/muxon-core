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
import java.util.Collection;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class UserPrincipalAuthenticationToken extends JwtAuthenticationToken {

  private final UserPrincipal userPrincipal;

  public UserPrincipalAuthenticationToken(
      Jwt jwt,
      Collection<? extends org.springframework.security.core.GrantedAuthority> authorities,
      UserPrincipal userPrincipal) {
    super(jwt, authorities);
    this.userPrincipal = userPrincipal;
    setAuthenticated(true);
  }

  @Override
  public UserPrincipal getPrincipal() {
    return userPrincipal;
  }

  @Override
  public String getName() {
    return userPrincipal.username();
  }
}
