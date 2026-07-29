package com.yorel.muxon.security;

import com.yorel.muxon.auth.UserPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;

public class UserPrincipalAuthenticationToken extends JwtAuthenticationToken {

    private final UserPrincipal userPrincipal;

    public UserPrincipalAuthenticationToken(Jwt jwt, Collection<? extends org.springframework.security.core.GrantedAuthority> authorities, UserPrincipal userPrincipal) {
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
