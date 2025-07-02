package com.sanchezdev.fileservice.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // Log JWT claims for debugging
        System.out.println("JWT Claims: " + jwt.getClaims());
        
        // Intentar extraer roles de extension_Roles (Azure AD B2C)
        Object extensionRoles = jwt.getClaim("extension_Roles");
        if (extensionRoles != null) {
            String roleValue = extensionRoles.toString();
            System.out.println("Found extension_Roles: " + roleValue);
            authorities.addAll(mapAzureRolesToSpringRoles(roleValue));
        }
        
        // También intentar extraer de roles estándar (por compatibilidad)
        Object roles = jwt.getClaim("roles");
        if (roles instanceof Collection<?>) {
            Collection<?> roleCollection = (Collection<?>) roles;
            authorities.addAll(
                roleCollection.stream()
                    .map(Object::toString)
                    .flatMap(role -> mapAzureRolesToSpringRoles(role).stream())
                    .collect(Collectors.toSet())
            );
        }
        
        // TEMPORAL: Para testing, si no hay roles específicos pero el token es válido, 
        // asignar roles por defecto basado en el issuer
        if (authorities.isEmpty()) {
            String issuer = jwt.getIssuer().toString();
            if (issuer.contains("b2clogin.com")) {
                // Token de Azure AD B2C sin extension_Roles - asignar permisos básicos
                System.out.println("Azure AD B2C token without extension_Roles - assigning default roles");
                authorities.add(new SimpleGrantedAuthority("ROLE_InvoiceManager"));
                authorities.add(new SimpleGrantedAuthority("ROLE_InvoiceReader"));
            } else if (issuer.contains("microsoftonline.com")) {
                // Token de Azure AD regular - asignar solo lectura por seguridad
                System.out.println("Azure AD token - assigning read-only role");
                authorities.add(new SimpleGrantedAuthority("ROLE_InvoiceReader"));
            } else {
                // Token de origen desconocido - rol básico
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }
        }
        
        System.out.println("Assigned authorities: " + authorities);
        return authorities;
    }
    
    private Set<GrantedAuthority> mapAzureRolesToSpringRoles(String azureRole) {
        Set<GrantedAuthority> springRoles = new HashSet<>();
        
        switch (azureRole.trim().toLowerCase()) {
            case "admin":
                // Admin tiene permisos completos
                springRoles.add(new SimpleGrantedAuthority("ROLE_InvoiceManager"));
                springRoles.add(new SimpleGrantedAuthority("ROLE_InvoiceReader"));
                break;
            case "manager":
                // Manager puede gestionar facturas
                springRoles.add(new SimpleGrantedAuthority("ROLE_InvoiceManager"));
                springRoles.add(new SimpleGrantedAuthority("ROLE_InvoiceReader"));
                break;
            case "reader":
                // Reader solo puede leer facturas
                springRoles.add(new SimpleGrantedAuthority("ROLE_InvoiceReader"));
                break;
            case "user":
                // Usuario básico
                springRoles.add(new SimpleGrantedAuthority("ROLE_USER"));
                break;
            default:
                // Por defecto, asignar rol de lectura si es un rol no reconocido
                springRoles.add(new SimpleGrantedAuthority("ROLE_InvoiceReader"));
        }
        
        return springRoles;
    }
}
