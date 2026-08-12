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

import com.yorel.muxon.db.model.IdentityProviderEntity;
import com.yorel.muxon.db.model.IdentityProviderProtocol;
import com.yorel.muxon.db.model.OidcIdentityProviderEntity;
import com.yorel.muxon.services.IdentityProviderService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // use @EnableWebSecurity for MVC (Servlet)
@EnableMethodSecurity // enables @PreAuthorize if you want later
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityConfig {

  private final IdentityProviderService idpService;
  private final JwtDecoderFactory jwtDecoderFactory;
  private final JwtUserPrincipalConverter jwtUserPrincipalConverter;

  public SecurityConfig(
      IdentityProviderService idpService,
      JwtDecoderFactory jwtDecoderFactory,
      JwtUserPrincipalConverter jwtUserPrincipalConverter) {
    this.idpService = idpService;
    this.jwtDecoderFactory = jwtDecoderFactory;
    this.jwtUserPrincipalConverter = jwtUserPrincipalConverter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/**") // Force this chain to handle all requests
        // 2. Set Stateless session (Required for Bearer tokens)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/error",
                        "/api/v1/status",
                        "/api/v1/info")
                    .permitAll()
                    .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exceptionHandling ->
                exceptionHandling.authenticationEntryPoint(
                    new BearerTokenAuthenticationEntryPoint()))
        .oauth2ResourceServer(
            oauth2 -> oauth2.authenticationManagerResolver(authenticationManagerResolver()));
    return http.build();
  }

  @Bean
  public AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver() {
    return request -> {
      // Use default provider from database
      Optional<IdentityProviderEntity> maybeProvider = idpService.getDefaultProvider();

      IdentityProviderEntity provider =
          maybeProvider.orElseThrow(
              () -> new RuntimeException("No default identity provider configured"));

      // Only OIDC is supported for JWT authentication
      if (provider.getProtocol() != IdentityProviderProtocol.OIDC) {
        throw new RuntimeException(
            "Only OIDC identity providers are supported for JWT authentication");
      }

      OidcIdentityProviderEntity oidcProvider = (OidcIdentityProviderEntity) provider;

      // ensure we have a decoder for this provider
      String key = provider.getId().toString(); // ID of provider row
      String issuer = oidcProvider.getOidcMetadata().getIssuerUri();
      String jwksUri = oidcProvider.getOidcMetadata().getJwkSetUri();
      JwtDecoder jwtDecoder = jwtDecoderFactory.getOrCreateDecoder(key, issuer, jwksUri);

      JwtAuthenticationProvider jwtAuthProvider = new JwtAuthenticationProvider(jwtDecoder);
      // Set custom converter to create UserPrincipal
      jwtAuthProvider.setJwtAuthenticationConverter(jwtUserPrincipalConverter);

      return jwtAuthProvider::authenticate;
    };
  }
}
