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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

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
    SecurityFilterChain filterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtRoleConverter);

        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health", "/h2-console/**").permitAll()
                    .requestMatchers("/api/invoices/**").authenticated()
                    .anyRequest().authenticated())
            .oauth2ResourceServer(o2 -> o2
                    .jwt(jwt -> jwt
                        .decoder(jwtDecoder)          // usa el bean inyectado
                        .jwtAuthenticationConverter(converter)))
            .headers(headers -> headers.frameOptions(Customizer.withDefaults())); // soporta H2

        return http.build();
    }

    /* ---------------------- decodificador JWT ---------------------- */
    @Bean
    JwtDecoder jwtDecoder() {
        // Use the CORRECT Azure AD JWK Set URI (not B2C)
        return NimbusJwtDecoder.withJwkSetUri("https://login.microsoftonline.com/28dbf599-4a0c-47c3-be6a-0790f3c7f43b/discovery/v2.0/keys")
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
