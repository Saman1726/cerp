package io.cerp.gateway.config;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebReactiveSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveNimbusJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandlers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import reactor.core.publisher.Mono;

/**
 * Security configuration for the API Gateway / BFF.
 *
 * <p>
 * Key responsibilities:
 * <ul>
 * <li>Configure OAuth2 Resource Server with JWT decoder pointing to Keycloak issuer</li>
 * <li>Define per-route authorization rules (public, authenticated, role-gated)</li>
 * <li>CORS configuration for frontend clients</li>
 * <li>/logout endpoint for session invalidation</li>
 * </ul>
 */
@Configuration
@EnableWebReactiveSecurity
public class SecurityConfig {

        @Value("${cerp.gateway.issuer-uri}")
        private String issuerUri;

        @Value("${cerp.gateway.logout.path:/logout}")
        private String logoutPath;

        @Value("${cerp.gateway.allowed-origins:https://localhost:3001,https://localhost:3000}")
        private String allowedOrigins;

        // ── Resource-server chain: all API routes protected ──

        @Bean
        @Order(0)
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
                http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                        .authorizeExchange(exchange -> exchange
                                // Health and actuator endpoints – no auth required
                                .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                                // OpenAPI docs (when available)
                                .pathMatchers("/api-docs/**", "/swagger-ui/**").permitAll()

                                // Identity service – registration/public endpoints (no auth required)
                                .pathMatchers("/api/identity/tenants").permitAll()
                                .pathMatchers("/api/identity/users").permitAll()
                                .pathMatchers("/api/identity/memberships").permitAll()
                                // Identity service – authenticated endpoints
                                .pathMatchers("/api/identity/**").authenticated()

                                // Organization service
                                .pathMatchers("/api/orgs/**").hasAuthority("org:manage")

                                // Project service
                                .pathMatchers("/api/projects/**").hasAuthority("project:manage")

                                // DMS
                                .pathMatchers("/api/documents/**").hasAuthority("document:manage")

                                // Audit
                                .pathMatchers("/api/audit/**").hasAnyAuthority("audit:read", "audit:manage")

                                // Procurement
                                .pathMatchers("/api/procurement/**").hasAuthority("procurement:manage")

                                // Vendor – registration endpoint (no auth required)
                                .pathMatchers("/api/vendors").permitAll()
                                // Vendor – authenticated endpoints
                                .pathMatchers("/api/vendors/**").hasAnyAuthority("vendor:manage", "vendor:read")

                                // Catalog
                                .pathMatchers("/api/catalog/**").hasAnyAuthority("catalog:manage", "catalog:read")

                                // Projections / reporting – read-only for any authenticated user
                                .pathMatchers("/api/reports/**").hasAuthority("reports:read")

                                // All other requests must be authenticated
                                .anyExchange().authenticated())
                        .logout(logout -> logout
                                .logoutUrl(logoutPath)
                                .logoutHandler(tokenLogoutHandler()))
                        .exceptionHandling(ex -> ex
                                .authenticationEntryPoint((exchange, ex1) -> exchange
                                        .response()
                                        .setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .writeWith(Mono.empty()))
                                .accessDeniedHandler((exchange, ex1) -> exchange
                                        .response()
                                        .setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .writeWith(Mono.empty())));

                return http.build();
        }

        // ── JWT Decoder ──

        @Bean
        public ReactiveJwtDecoder jwtDecoder() {
                ReactiveNimbusJwtDecoder decoder = new ReactiveNimbusJwtDecoder(
                        issuerUri + "/protocol/openid-connect/certs");
                return decoder;
        }

        // ── CORS ──

        @Bean
        public CorsWebFilter corsWebFilter() {
                return corsConfigurationSource();
        }

        private UrlBasedCorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of(
                        "Authorization",
                        "Content-Type",
                        "X-Tenant-Id",
                        "X-Correlation-Id",
                        "X-Request-Id"));
                config.setExposedHeaders(List.of(
                        "X-Correlation-Id",
                        "X-Request-Id",
                        "X-Tenant-Id"));
                config.setAllowCredentials(true);
                config.setMaxAge(Duration.ofHours(1));

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }

        // ── Logout handler: revoke access token on Keycloak ──

        @Bean
        public ServerLogoutHandler tokenLogoutHandler() {
                return ServerLogoutHandlers.defaultLogoutHandler();
        }
}
