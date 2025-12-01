package com.onetattva.infron.core.security;

import com.onetattva.infron.db.model.IdentityProviderEntity;
import com.onetattva.infron.db.model.OidcIdentityProviderEntity;
import com.onetattva.infron.db.model.IdentityProviderProtocol;
import com.onetattva.infron.core.services.IdentityProviderService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Optional;

@Configuration
@EnableWebSecurity        // use @EnableWebSecurity for MVC (Servlet)
@EnableMethodSecurity        // enables @PreAuthorize if you want later
public class SecurityConfig {

    private final IdentityProviderService idpService;
    private final JwtDecoderFactory jwtDecoderFactory;

    public SecurityConfig(IdentityProviderService idpService, JwtDecoderFactory jwtDecoderFactory) {
        this.idpService = idpService;
        this.jwtDecoderFactory = jwtDecoderFactory;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )
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
            // Optionally set JwtAuthenticationConverter to map claims->authorities
            //jwtAuthProvider.setJwtAuthenticationConverter(new JwtGrantedAuthoritiesConverter()); // or custom converter

            return authentication -> jwtAuthProvider.authenticate(authentication);
        };
    }
}
