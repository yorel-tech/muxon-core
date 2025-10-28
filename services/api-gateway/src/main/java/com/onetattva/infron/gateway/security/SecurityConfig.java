package com.onetattva.infron.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Configuration
public class SecurityConfig {

    private static final String REQUIRED_AUDIENCE = "infron-api"; // matches Keycloak clientId

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/healthz", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        //.requestMatchers(HttpMethod.GET, "/api/v1/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .cors(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation("http://localhost:8085/realms/infron-dev");
        // Enforce audience = infron-api
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer("http://localhost:8085/realms/infron-dev");
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            List<String> aud = jwt.getAudience();
            return (aud != null && aud.contains(REQUIRED_AUDIENCE))
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Missing required audience 'infron-api'", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator));
        return decoder;
    }

    @Bean
    org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        var c = new org.springframework.web.cors.CorsConfiguration();
        c.setAllowedOrigins(List.of("http://localhost:4000"));
        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true);
        var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return conv;
    }

    // Map Keycloak realm roles -> ROLE_<role>
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        Stream<String> roles = Stream.empty();
        if (realmAccess instanceof Map<?, ?> access) {
            Object r = access.get("roles");
            if (r instanceof List<?> list) {
                roles = list.stream().filter(String.class::isInstance).map(String.class::cast);
            }
        }
        return roles.map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                .toList();
    }
}
