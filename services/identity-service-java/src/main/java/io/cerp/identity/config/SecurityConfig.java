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
            .securityMatcher("/tenants", "/users", "/memberships", "/subscriptions", "/auth/login", "/auth/*", "/api/v1/users/*/subscription",
                "/api/v1/users/*/can-write-project", "/api/v1/users/*/can-read-project", "/api/v1/users/*/can-delete-project",
                "/api/v1/users/*/accessible-projects", "/api/v1/users/*/is-tenant-admin-or-manager")
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
                // Read-only permission-check endpoints called by workflow-service (SagaController,
                // via IdentityPermissionClient) to gate saga creation, and by project-service's
                // saga-listing access control - internal service-to-service calls with no end-user
                // token to forward. These were previously uncovered by any securityMatcher here,
                // so they fell through to defaultFilterChain's anyRequest().authenticated() and
                // always 401'd - IdentityPermissionClient's catch-all then silently turned that into
                // "deny" for the specific-project checks (canWriteOnProject et al), so a real
                // resource membership could never actually grant write access.
                .requestMatchers(HttpMethod.GET, "/api/v1/users/*/can-write-project", "/api/v1/users/*/can-read-project",
                    "/api/v1/users/*/can-delete-project", "/api/v1/users/*/accessible-projects",
                    "/api/v1/users/*/is-tenant-admin-or-manager").permitAll()
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
                // Tomcat's internal error-page redispatch lands here as a fresh request to
                // "/error" (it doesn't match publicApiFilterChain's securityMatcher, so it falls
                // through to this chain) - without this, any @ResponseStatus exception thrown
                // from an otherwise-permitAll GET (e.g. UserController's 404 for "no such email")
                // hits anyRequest().authenticated() on the redispatch and comes back as a bare 401
                // instead of the intended status, masking the real error entirely.
                .requestMatchers("/error").permitAll()
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
