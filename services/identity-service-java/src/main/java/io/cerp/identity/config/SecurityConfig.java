package io.cerp.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import io.cerp.identity.security.TenantRequestFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain publicApiFilterChain(
            HttpSecurity http,
            TenantRequestFilter tenantRequestFilter) throws Exception {
        // This chain handles public endpoints (registration, login)
        http
            .securityMatcher("/tenants", "/users", "/memberships", "/auth/login", "/auth/*")
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Registration endpoints - must be public
                .requestMatchers("/tenants").permitAll()
                .requestMatchers("/users").permitAll()
                .requestMatchers("/memberships").permitAll()
                // DELETE endpoints for cleanup
                .requestMatchers(HttpMethod.DELETE, "/tenants/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/users/**").permitAll()
                // Auth endpoints
                .requestMatchers("/auth/login").permitAll()
                // everything else on this chain requires auth
                .anyRequest().authenticated())
            // Validate JWTs issued by Keycloak
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        http.addFilterAfter(tenantRequestFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        // Default chain for all other endpoints
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // actuator health/info public for dev/k8s probes
                .requestMatchers("/actuator/**").permitAll()
                // OpenAPI/Swagger docs
                .requestMatchers("/api-docs/**", "/swagger/**", "/swagger-ui/**").permitAll()
                // allow simple GETs for initial smoke tests (optional)
                .requestMatchers(HttpMethod.GET, "/tenants/**", "/users/**", "/memberships/**").authenticated()
                // everything else requires auth
                .anyRequest().authenticated())
            // Validate JWTs issued by Keycloak
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-Id", "X-Correlation-Id"));
        config.setExposedHeaders(List.of("X-Correlation-Id", "X-Tenant-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
