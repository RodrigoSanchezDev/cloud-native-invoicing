package com.sanchezdev.invoiceservice.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;


@Component
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final Logger logger = LoggerFactory.getLogger(JwtRoleConverter.class);

    @Override
	public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
		
		logger.info("JWT Claims: {}", jwt.getClaims());
		
		// Try different possible claim names
		String roles = jwt.getClaimAsString("extension_Roles");
		logger.info("extension_Roles claim: {}", roles);
		
		if (roles == null) {
			roles = jwt.getClaimAsString("roles");
			logger.info("roles claim: {}", roles);
		}
		
		if (roles == null) {
			Object extRolesObj = jwt.getClaim("extension_Roles");
			logger.info("extension_Roles as Object: {}", extRolesObj);
			if (extRolesObj != null) {
				roles = extRolesObj.toString();
			}
		}

		if (roles == null || roles.isBlank()) {
			logger.warn("No roles found in JWT, assigning default ADMIN role for testing");
			return java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
		}

		Collection<GrantedAuthority> authorities = Arrays.stream(roles.split(","))
				.map(String::trim)
				.map(role -> "ROLE_" + role.toUpperCase())
				.map(SimpleGrantedAuthority::new)
				.collect(Collectors.toList());
		
		logger.info("Converted authorities: {}", authorities);
		return authorities;
	}
}
