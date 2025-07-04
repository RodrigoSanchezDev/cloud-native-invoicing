package com.sanchezdev.fileservice.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;


@Component
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {

		String roles = jwt.getClaimAsString("extension_Roles");

		if (roles == null || roles.isBlank()) {
			return java.util.Collections.emptyList();
		}

		return Arrays.stream(roles.split(",")).map(String::trim).map(role -> "ROLE_" + role.toUpperCase())
				.map(SimpleGrantedAuthority::new).collect(Collectors.toList());
	}
}
