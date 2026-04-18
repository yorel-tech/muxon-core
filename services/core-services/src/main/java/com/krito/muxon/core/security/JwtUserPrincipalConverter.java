package com.krito.muxon.core.security;

import com.krito.muxon.core.auth.UserPrincipal;
import com.krito.muxon.db.repository.UserRoleBindingViewRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        List<String> roles = userRoleRepo.findByExternalId(userId)
                .stream()
                .map(binding -> binding.getRoleName())
                .distinct()
                .collect(Collectors.toList());

        // Create UserPrincipal
        UserPrincipal userPrincipal = new UserPrincipal(userId, username, roles);

        // Return custom authentication token with UserPrincipal as principal
        // Authorities list is empty since we handle authorization separately
        return new UserPrincipalAuthenticationToken(jwt, new ArrayList<GrantedAuthority>(), userPrincipal);
    }
}
