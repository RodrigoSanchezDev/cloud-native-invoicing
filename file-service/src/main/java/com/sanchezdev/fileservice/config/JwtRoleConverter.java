package com.sanchezdev.fileservice.config;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Convierte los roles que vienen en el JWT ( extension_Roles o roles )
 * a objetos GrantedAuthority que Spring Security entiende.
 *
 * ⚠️ Sólo devuelve la **colección de autoridades**, NO un AuthenticationToken
 * (es lo que exige JwtAuthenticationConverter#setJwtGrantedAuthoritiesConverter).
 */
@Component
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String EXT_ROLES = "extension_Roles";
    private static final String STD_ROLES = "roles";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {

        Set<GrantedAuthority> authorities = new HashSet<>();

        /* ------------ 1) roles personalizados B2C -------------- */
        Object extClaim = jwt.getClaim(EXT_ROLES);
        if (extClaim != null) {
            authorities.addAll(toSpringRoles(extClaim.toString()));
        }

        /* ------------ 2) roles estándar Azure AD --------------- */
        Collection<String> stdRoles = jwt.getClaimAsStringList(STD_ROLES);
        if (stdRoles != null) {
            authorities.addAll(stdRoles.stream()
                    .flatMap(r -> toSpringRoles(r).stream())
                    .collect(Collectors.toSet()));
        }

        /* ------------ 3) respaldo si no vino ningún role -------- */
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_FileReader"));
        }

        return authorities;
    }

    /* ---- mapping Azure-role ➜ Spring-role --------------------- */
    private Collection<GrantedAuthority> toSpringRoles(String azureRole) {
        switch (azureRole.trim().toLowerCase()) {
            case "admin":
                return List.of(
                    new SimpleGrantedAuthority("ROLE_FileManager"),
                    new SimpleGrantedAuthority("ROLE_FileReader"));
            case "manager":
                return List.of(
                    new SimpleGrantedAuthority("ROLE_FileManager"),
                    new SimpleGrantedAuthority("ROLE_FileReader"));
            case "reader":
                return List.of(new SimpleGrantedAuthority("ROLE_FileReader"));
            default:
                return List.of(new SimpleGrantedAuthority("ROLE_FileReader"));
        }
    }
}
