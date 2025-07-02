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
        
        // Intentar extraer roles de extension_Roles (Azure AD B2C)
        Object extensionRoles = jwt.getClaim("extension_Roles");
        if (extensionRoles != null) {
            String roleValue = extensionRoles.toString();
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
        
        // Si no hay roles específicos, agregar un rol por defecto
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        
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
