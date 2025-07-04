package com.sanchezdev.invoiceservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtRoleConverter jwtRoleConverter;

    /* audiencia que validarás (Client ID) */
    @Value("${jwt.audience}")
    private String audience;

    /* ---------------------- reglas HTTP ---------------------- */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtRoleConverter);

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health", "/h2-console/**").permitAll()
                    .requestMatchers("/api/invoices/**").authenticated()
                    .anyRequest().authenticated())
            .oauth2ResourceServer(o2 -> o2
                    .jwt(jwt -> jwt
                        .decoder(jwtDecoder())          // usa el bean de abajo
                        .jwtAuthenticationConverter(converter)))
            .headers(headers -> headers.frameOptions(Customizer.withDefaults())); // soporta H2

        return http.build();
    }

    /* ---------------------- decodificador JWT ---------------------- */
    @Bean
    JwtDecoder jwtDecoder() {
        // Use policy-specific path for Azure AD B2C JWKs
        return NimbusJwtDecoder.withJwkSetUri(
            "https://duoccloudnatives6.b2clogin.com/duoccloudnatives6.onmicrosoft.com/B2C_1_AppS3/discovery/v2.0/keys")
            .build();
    }
}
