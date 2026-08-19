package io.cerp.vendor.config;

import io.cerp.vendor.security.TenantRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
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
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "DEV_MODE", havingValue = "true", matchIfMissing = false)
    @org.springframework.core.annotation.Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TenantRequestFilter tenantRequestFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // In dev mode, allow all requests (no JWT required)
                .requestMatchers("/**").permitAll())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        tenantRequestFilter.setExcludePatterns(java.util.List.of("/**"));

        http.addFilterAfter(tenantRequestFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
    @org.springframework.core.annotation.Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain productionSecurityFilterChain(HttpSecurity http, TenantRequestFilter tenantRequestFilter) throws Exception {
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
                // Internal service-to-service reads (e.g. workflow-service resolving vendor user IDs)
                // do not carry a user Bearer token; the tenant context is set via X-Tenant-Id header.
                .requestMatchers(HttpMethod.GET, "/vendors/**").permitAll()
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
