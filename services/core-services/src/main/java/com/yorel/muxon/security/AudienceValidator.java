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

import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

  private final String expectedAudience;

  public AudienceValidator(String expectedAudience) {
    this.expectedAudience = expectedAudience;
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt token) {
    List<String> aud = token.getAudience();
    if (aud != null && aud.contains(expectedAudience)) {
      return OAuth2TokenValidatorResult.success();
    }
    OAuth2Error error = new OAuth2Error("invalid_token", "The required audience is missing", null);
    return OAuth2TokenValidatorResult.failure(error);
  }
}
