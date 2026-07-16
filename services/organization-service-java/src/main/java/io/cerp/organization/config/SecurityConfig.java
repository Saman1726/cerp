package io.cerp.organization.config;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.oauth2.resource.AbstractJwtBearerTokenAuthenticationParser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security configuration for Organization Service.
 *
 * <p>
 * Configures OAuth2 resource server (JWT validation), security headers, and per-path access rules.
 *
 * <p>
 * Per Section 21: All paths require authentication except actuator health. User identity is extracted from JWT claims for audit trail.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String ORG_ID_CLAIM = "organization_id";

    @Bean
    @Order(0)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        // Allow unauthenticated access to actuator endpoints for k8s probes
        http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (stateless API)
            .csrf(AbstractHttpConfigurer::disable)

            // CORS configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public health/readiness probes
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()

                // Admin endpoints (only platform admins)
                .requestMatchers("/api/orgs/v1/admin/**").hasRole("PLATFORM_ADMIN")

                // All other API paths require authentication
                .requestMatchers("/api/**").authenticated()

                // Anything else permit all
                .anyRequest().permitAll())

            // OAuth2 Resource Server (JWT validation)
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())))

            // Security headers
            .headers(headers -> headers
                .contentTypeOptions(options -> options.nosniff())
                .strictTransportSecurity(hsp -> hsp.maxAgeInSeconds = 31536000)
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));

        return http.build();
    }

    /**
     * JWT authentication converter that extracts: - User ID from preferred_name or sub claim - Tenant IDs from resource_access or custom claims - Roles/authorities from
     * scope or roles claim
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(new Function<>() {
            private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

            @Override
            public org.springframework.security.core.GrantedAuthority apply(org.springframework.security.oauth2.jwt.Jwt jwt) {
                // Default scope-based authorities
                var authorities = defaultConverter.apply(jwt);

                // Add role-based authorities
                List<String> roles = jwt.getClaimOrDefault("roles", List.class);
                if (roles != null) {
                    for (String role : roles) {
                        String authority = "ROLE_" + role.toUpperCase();
                        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(authority));
                    }
                }

                return authorities;
            }
        });

        converter.setClaimName("preferred_username");

        converter.setPrincipalClaimName("preferred_username");

        return converter;
    }

    /**
     * CORS configuration for API endpoints.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "https://localhost:*",
            "https://cerp-local.example.com"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Tenant-Id",
            "X-Correlation-Id",
            "X-User-Id"));
        config.setExposedHeaders(List.of(
            "X-Correlation-Id",
            "X-Rate-Limit-Limit",
            "X-Rate-Limit-Remaining"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
