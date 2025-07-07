package com.sanchezdev.rabbitmqservice.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        // Try to extract roles from various claims
        Object rolesClaim = jwt.getClaim("roles");
        if (rolesClaim == null) {
            rolesClaim = jwt.getClaim("Role");
        }
        if (rolesClaim == null) {
            rolesClaim = jwt.getClaim("role");
        }
        if (rolesClaim == null) {
            rolesClaim = jwt.getClaim("extension_Role");
        }

        if (rolesClaim instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) rolesClaim;
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .map(GrantedAuthority.class::cast)
                    .toList();
        } else if (rolesClaim instanceof String) {
            String role = (String) rolesClaim;
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        }

        // Default role if no roles found in token
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
