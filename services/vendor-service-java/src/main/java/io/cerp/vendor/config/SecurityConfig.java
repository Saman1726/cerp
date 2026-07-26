package io.cerp.vendor.config;

import io.cerp.vendor.security.TenantRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoderProduction(
            @Value("${OAUTH_ISSUER_URI:http://keycloak:8081/realms/master}") String issuerUri) {
        // Production decoder for Keycloak-signed RS256 JWTs
        // Only used when DevJwtDecoder is not active (i.e., DEV_MODE is not true)
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(issuerUri + "/protocol/openid-connect/certs").build();
        return decoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TenantRequestFilter tenantRequestFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // actuator health/info public for dev/k8s probes
                .requestMatchers("/actuator/**").permitAll()
                // OpenAPI/Swagger docs
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Registration endpoint (used by register-vendor API route) - must be public
                .requestMatchers(HttpMethod.POST, "/vendors").permitAll()
                .requestMatchers("/vendors/health").permitAll()
                // allow simple GETs for initial smoke tests (optional)
                .requestMatchers(HttpMethod.GET, "/vendors/**").authenticated()
                // everything else requires auth
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        // Skip tenant context extraction for public endpoints
        tenantRequestFilter.setExcludePatterns(java.util.List.of("/vendors", "/vendors/health", "/actuator/**", "/v3/api-docs/**", "/swagger-ui/**"));

        // Bind tenant context after JWT resolution (tenant filter now handles exclusions internally)
        http.addFilterAfter(tenantRequestFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}
