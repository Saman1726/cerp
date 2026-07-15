package io.cerp.identity.config;

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
            // Validate JWTs issued by Keycloak via issuer-uri configured in application.yml
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}
