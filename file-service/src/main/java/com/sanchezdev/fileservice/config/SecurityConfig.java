package com.sanchezdev.fileservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
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
        // Usar la URL específica de JWK Set con el user flow (como el profesor)
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(
            "https://duoccloudnatives6.b2clogin.com/DuoccloudnativeS6.onmicrosoft.com/discovery/v2.0/keys?p=B2C_1_AppS3"
        ).build();
        
        // Validar audience (que el token sea para nuestra aplicación)
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtAudienceValidator(audience);
        
        // Validar issuer (que el token venga de nuestro tenant de Azure AD B2C) 
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(
            "https://duoccloudnatives6.b2clogin.com/DuoccloudnativeS6.onmicrosoft.com/v2.0/?p=B2C_1_AppS3"
        );
        
        // Combinar ambos validadores
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);
        
        jwtDecoder.setJwtValidator(withAudience);
        return jwtDecoder;
    }
}
