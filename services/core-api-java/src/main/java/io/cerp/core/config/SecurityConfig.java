package io.cerp.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // For simple dev usage; no browser form login, use HTTP Basic for protected routes
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // actuator health/info should be public in docker dev
                .requestMatchers("/actuator/**").permitAll()
                // public endpoints for core-api used by web/other services
                .requestMatchers(HttpMethod.GET, "/partners/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/partner-login").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/properties/**", "/work-orders/**", "/purchase-orders/**", "/construction-sites/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/properties/**", "/construction-sites/**").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/properties/**", "/construction-sites/**").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/properties/**", "/construction-sites/**").permitAll()
                // everything else requires auth (keeps a sane default)
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
