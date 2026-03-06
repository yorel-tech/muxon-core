package com.onetattva.infron.core.security;

import com.onetattva.infron.db.model.IdentityProviderEntity;
import com.onetattva.infron.db.model.OidcIdentityProviderEntity;
import com.onetattva.infron.db.model.IdentityProviderProtocol;
import com.onetattva.infron.core.services.IdentityProviderService;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.Optional;

@Configuration
@EnableWebSecurity        // use @EnableWebSecurity for MVC (Servlet)
@EnableMethodSecurity        // enables @PreAuthorize if you want later
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityConfig {

    private final IdentityProviderService idpService;
    private final JwtDecoderFactory jwtDecoderFactory;
    private final JwtUserPrincipalConverter jwtUserPrincipalConverter;

    public SecurityConfig(IdentityProviderService idpService,
                         JwtDecoderFactory jwtDecoderFactory,
                         JwtUserPrincipalConverter jwtUserPrincipalConverter) {
        this.idpService = idpService;
        this.jwtDecoderFactory = jwtDecoderFactory;
        this.jwtUserPrincipalConverter = jwtUserPrincipalConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**") // Force this chain to handle all requests
                // 2. Set Stateless session (Required for Bearer tokens)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info", "/error", "/api/v1/status", "/api/v1/info").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()))
                .oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(authenticationManagerResolver()));
        return http.build();
    }

    @Bean
    public AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver() {
        return request -> {
            // Use default provider from database
            Optional<IdentityProviderEntity> maybeProvider = idpService.getDefaultProvider();

            IdentityProviderEntity provider = maybeProvider.orElseThrow(() -> new RuntimeException("No default identity provider configured"));

            // Only OIDC is supported for JWT authentication
            if (provider.getProtocol() != IdentityProviderProtocol.OIDC) {
                throw new RuntimeException("Only OIDC identity providers are supported for JWT authentication");
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
