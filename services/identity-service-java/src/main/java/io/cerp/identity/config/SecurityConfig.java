package io.cerp.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
    @Order(2)
    public SecurityFilterChain publicApiFilterChain(
            HttpSecurity http,
            TenantRequestFilter tenantRequestFilter) throws Exception {
        // This chain handles public endpoints (registration, login).
        // /users/profile is intentionally NOT listed here - it falls through to
        // defaultFilterChain (Order 3), which requires a validated Keycloak JWT.
        http
            .securityMatcher("/tenants", "/users", "/memberships", "/subscriptions", "/auth/login", "/auth/*", "/api/v1/users/*/subscription")
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                // Registration endpoints - must be public
                .requestMatchers(HttpMethod.POST, "/tenants").permitAll()
                // GET /tenants (list-all) requires auth - no legitimate public caller
                .requestMatchers(HttpMethod.GET, "/tenants").authenticated()
                .requestMatchers("/users").permitAll()
                .requestMatchers(HttpMethod.POST, "/users").permitAll()
                // GET /users (without path segments) - list/get by email. UserController.get()
                // itself enforces that the bare (no ?email=) full-list form additionally
                // requires a validated JWT - see UserController for the actual gate.
                .requestMatchers(HttpMethod.GET, "/users").permitAll()
                // GET /users/{id} - authenticated
                .requestMatchers(HttpMethod.GET, "/users/**").authenticated()
                // Registration flows create a Subscription right after the Membership step
                .requestMatchers(HttpMethod.POST, "/subscriptions").permitAll()
                // Auth endpoints
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/auth/refresh").permitAll()
                // Registration endpoints - must be public
                .requestMatchers(HttpMethod.POST, "/memberships").permitAll()
                // GET /memberships?tenantId=... called by internal services (e.g. workflow-service)
                // to resolve vendor notification recipients without a user JWT
                .requestMatchers(HttpMethod.GET, "/memberships").permitAll()
                // called by project-service (and, later, workflow-service) to resolve a
                // user's plan limits for quota enforcement - internal service-to-service call
                .requestMatchers(HttpMethod.GET, "/api/v1/users/*/subscription").permitAll()
                // everything else on this chain requires auth
                .anyRequest().authenticated())
            // Validate JWTs issued by Keycloak
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        http.addFilterAfter(tenantRequestFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(3)
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
