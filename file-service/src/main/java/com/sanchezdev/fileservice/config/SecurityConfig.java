package com.sanchezdev.fileservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtRoleConverter jwtRoleConverter;
    
    @Value("${jwt.audience}")
    private String audience;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Health endpoints públicos
                .requestMatchers("/actuator/health").permitAll()
                // Todos los endpoints de files requieren autenticación (sin roles específicos)
                .requestMatchers("/files/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                .decoder(jwtDecoder())
                .jwtAuthenticationConverter(jwtRoleConverter)));

        return http.build();
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        // Usar la URL específica de JWK Set para Azure AD regular
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(
            "https://login.microsoftonline.com/28dbf599-4a0c-47c3-be6a-0790f3c7f43b/discovery/v2.0/keys"
        ).build();
        
        // Para Azure AD regular, solo validamos el issuer (la audiencia se valida automáticamente)
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(
            "https://login.microsoftonline.com/28dbf599-4a0c-47c3-be6a-0790f3c7f43b/v2.0"
        );
        
        jwtDecoder.setJwtValidator(withIssuer);
        return jwtDecoder;
    }
}
