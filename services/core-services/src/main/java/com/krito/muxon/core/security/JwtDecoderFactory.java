package com.krito.muxon.core.security;


import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@Component
public class JwtDecoderFactory {

    // In-memory cache per JVM; decoders are refreshed on a fixed sweep interval.
    private final ConcurrentMap<String, JwtDecoder> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService sweeper = Executors.newScheduledThreadPool(1);

    public JwtDecoderFactory() {
        // optional: periodically clear cache or implement TTL eviction logic
        sweeper.scheduleAtFixedRate(cache::clear, 60, 60, TimeUnit.MINUTES);
    }

    /**
     * Build or return cached JwtDecoder for the given issuer or jwksUri key.
     * Key should be unique per provider (use provider.id or issuer).
     */
    public JwtDecoder getOrCreateDecoder(String key, String issuer, String jwksUri) {
        return cache.computeIfAbsent(key, k -> buildDecoder(issuer, jwksUri));
    }

    private JwtDecoder buildDecoder(String issuer, String jwksUri) {
        NimbusJwtDecoder jwtDecoder;
        if (jwksUri != null && !jwksUri.isBlank()) {
            jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        } else {
            // fallback to OIDC discovery (may fetch jwks_uri automatically)
            jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromOidcIssuerLocation(issuer);
        }

        // validators: default issuer validator + optional audience or custom validators
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        // combine with other validators if needed
        OAuth2TokenValidator<Jwt> combined = new DelegatingOAuth2TokenValidator<>(issuerValidator);
        jwtDecoder.setJwtValidator(combined);
        return jwtDecoder;
    }

    // call to evict when provider changes
    public void evict(String key) {
        cache.remove(key);
    }
}

