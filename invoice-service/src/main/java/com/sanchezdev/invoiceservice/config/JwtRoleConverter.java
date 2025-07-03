package com.sanchezdev.invoiceservice.config;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Convierte los claims de roles (extension_Roles o roles estándar)
 * en objetos GrantedAuthority entendibles por Spring Security.
 */
@Component
public class JwtRoleConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String EXT_ROLES = "extension_Roles";
    private static final String STD_ROLES = "roles";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {

        Set<GrantedAuthority> authorities = new HashSet<>();

        /* -------- 1) Roles en ‘extension_Roles’ --------------- */
        Object extClaim = jwt.getClaim(EXT_ROLES);
        if (extClaim != null) {
            authorities.addAll(mapAzureRole(extClaim.toString()));
        }

        /* -------- 2) Roles estándar ‘roles’ ------------------- */
        Collection<String> stdRoles = jwt.getClaimAsStringList(STD_ROLES);
        if (stdRoles != null) {
            authorities.addAll(stdRoles.stream()
                    .flatMap(r -> mapAzureRole(r).stream())
                    .collect(Collectors.toSet()));
        }

        /* -------- 3) Respaldo por emisor ---------------------- */
        if (authorities.isEmpty()) {
            String iss = jwt.getIssuer().toString();
            if (iss.contains("b2clogin.com")) {
                authorities.add(new SimpleGrantedAuthority("ROLE_InvoiceReader"));
            } else {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }
        }

        return authorities;
    }

    /* ------------ mapeo Azure ➜ Spring ----------------------- */
    private Collection<GrantedAuthority> mapAzureRole(String role) {
        switch (role.trim().toLowerCase()) {
            case "admin":
                return List.of(
                    new SimpleGrantedAuthority("ROLE_InvoiceManager"),
                    new SimpleGrantedAuthority("ROLE_InvoiceReader"));
            case "manager":
                return List.of(
                    new SimpleGrantedAuthority("ROLE_InvoiceManager"),
                    new SimpleGrantedAuthority("ROLE_InvoiceReader"));
            case "reader":
                return List.of(new SimpleGrantedAuthority("ROLE_InvoiceReader"));
            case "user":
                return List.of(new SimpleGrantedAuthority("ROLE_USER"));
            default:
                return List.of(new SimpleGrantedAuthority("ROLE_InvoiceReader"));
        }
    }
}
