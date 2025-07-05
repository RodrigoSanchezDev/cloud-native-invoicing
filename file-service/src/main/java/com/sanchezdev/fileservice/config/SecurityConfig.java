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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

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
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
    JwtDecoder jwtDecoder(@Value("${azure.b2c.jwk-set-uri}") String jwkSetUri) {
        // Use JWK Set URI from environment variables for flexibility
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
            .build();
    }

    /* ---------------------- configuración CORS ---------------------- */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow all origins (or specify your frontend domain)
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // Allow common headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With", 
            "Accept", 
            "Origin", 
            "Access-Control-Request-Method", 
            "Access-Control-Request-Headers"
        ));
        
        // Allow common HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Cache preflight for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
