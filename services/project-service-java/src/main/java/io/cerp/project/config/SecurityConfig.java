package io.cerp.project.config;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The single security chain for project-service-java. Previously two separate SecurityFilterChain beans existed here (this class and the now-deleted
 * ProjectServiceSecurityConfig), neither with an explicit securityMatcher/@Order - harmless while both merely permitted everything under /api/**, but unsafe once real
 * authentication enforcement mattered (which one would apply was implementation-defined). Consolidated into one.
 *
 * Creation/assignment endpoints now require a valid Keycloak-issued JWT (see IdentityContext for how controllers/services read the caller's identity out of it) - this is
 * a correctness fix, not a tightening for its own sake: without a real caller identity, a Project can't have a real owner (see ProjectService.create()).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/actuator/info", "/actuator/metrics", "/actuator/prometheus").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .addFilterBefore(new TenantIdValidationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("https://localhost:*", "https://cerp-local.example.com", "http://localhost:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-Id", "X-Correlation-Id", "X-User-Id"));
        config.setExposedHeaders(List.of("X-Correlation-Id", "X-Rate-Limit-Limit", "X-Rate-Limit-Remaining"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /**
     * Validates the X-Tenant-Id header is present for project service requests, enforcing tenant isolation at the API level. Unchanged from before: tenant resolution
     * stays header-based (already reliable - the frontend always sets it) rather than moving to a JWT claim, since a user's tenant_id Keycloak attribute is only known
     * once their Membership exists, not at account-creation time (see KeycloakAdminClient.upsertUser).
     */
    private static class TenantIdValidationFilter extends OncePerRequestFilter {

        private static final String TENANT_ID_HEADER = "X-Tenant-Id";
        private static final List<String> PUBLIC_PATHS = List.of(
            "/actuator/health",
            "/actuator/info",
            "/actuator/metrics",
            "/actuator/prometheus");

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return PUBLIC_PATHS.stream().anyMatch(request.getServletPath()::startsWith);
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String tenantId = request.getHeader(TENANT_ID_HEADER);
            if (tenantId == null || tenantId.isBlank()) {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"status\":401,\"message\":\"Missing X-Tenant-Id header\"}");
                return;
            }
            filterChain.doFilter(request, response);
        }
    }
}
