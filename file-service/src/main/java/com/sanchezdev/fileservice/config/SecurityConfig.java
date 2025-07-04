package com.sanchezdev.fileservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.core.converter.ClaimConversionService;

@Configuration
public class SecurityConfig {

    /* Issuer de la instancia B2C  ------------------------------------ */
    private static final String ISSUER =
        "https://duoccloudnatives6.b2clogin.com/28dbf599-4a0c-47c3-be6a-0790f3c7f43b/v2.0";

    @Value("${jwt.audience}")
    private String audience;

    /* ------------- 1.  JwtAuthenticationConverter ------------------ */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(JwtRoleConverter roleConverter) {
        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(roleConverter);   // <– usa tu converter
        return conv;
    }

    /* ------------- 2.  SecurityFilterChain ------------------------- */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    JwtDecoder jwtDecoder,
                                    JwtAuthenticationConverter authConverter) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/files/**").authenticated()
                .anyRequest().authenticated())
            .oauth2ResourceServer(o2 -> o2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder)
                    .jwtAuthenticationConverter(authConverter)));

        return http.build();
    }

    /* ------------- 3.  JwtDecoder con validadores ------------------ */
    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(
            "https://duoccloudnatives6.b2clogin.com/duoccloudnatives6.onmicrosoft.com/discovery/v2.0/keys?p=B2C_1_AppS3")
            .build();
    }
}
