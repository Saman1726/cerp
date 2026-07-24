package io.cerp.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Workflow Service.
 *
 * <p>
 * For development and internal service-to-service communication, workflow endpoints are accessible without authentication. The X-Tenant-Id header is used for
 * multi-tenancy routing. OAuth2 resource server is explicitly disabled for internal service calls — the dependency is present for future compatibility but not enforced.
 * </p>
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    /**
     * Disables OAuth2 resource server to allow unauthenticated internal service-to-service calls. The oauth2-resource-server dependency is present for future
     * compatibility but JWT validation is not enforced on internal endpoints.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .oauth2ResourceServer(oauth2 -> oauth2.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().permitAll())
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));
        return http.build();
    }
}
