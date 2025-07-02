package com.sanchezdev.invoiceservice.config;

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
                // Health endpoints y H2 Console públicos
                .requestMatchers("/actuator/health", "/h2-console/**").permitAll()
                // Todos los endpoints de API requieren autenticación (sin roles específicos)
                .requestMatchers("/api/invoices/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                .decoder(jwtDecoder())
                .jwtAuthenticationConverter(jwtRoleConverter)))
            // Para H2 Console
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        // Usar la URL específica de JWK Set para Azure AD regular
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(
            "https://login.microsoftonline.com/28dbf599-4a0c-47c3-be6a-0790f3c7f43b/discovery/v2.0/keys"
        ).build();
        
        // Validar audience (que el token sea para nuestra aplicación)
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtAudienceValidator(audience);
        
        // Validar issuer (que el token venga de nuestro tenant de Azure AD) 
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(
            "https://login.microsoftonline.com/28dbf599-4a0c-47c3-be6a-0790f3c7f43b/v2.0"
        );
        
        // Combinar ambos validadores
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);
        
        jwtDecoder.setJwtValidator(withAudience);
        return jwtDecoder;
    }
}
