package io.cerp.vendor.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * DevJwtDecoder accepts unsigned JWTs (alg:none) used in local development.
 *
 * JWT format issued by identity-service: Header: {"alg":"none","typ":"JWT"} Payload: {"sub":"email","roles":"role1,role2","exp":timestamp}
 *
 * This decoder is used when DEV_MODE=true (local docker-compose). It is conditional so that in production the standard NimbusJwtDecoder handles Keycloak-signed RS256
 * tokens.
 */
@Configuration
@ConditionalOnProperty(name = "DEV_MODE", havingValue = "true", matchIfMissing = false)
public class DevJwtDecoder {

    @Bean
    @Primary
    public JwtDecoder jwtDecoder(
            @Value("${JWT_SECRET:dev}") String secret) {
        return (rawToken) -> {
            try {
                String[] parts = rawToken.split("\\.");
                if (parts.length != 3) {
                    throw new JwtException("Invalid JWT format: expected 3 parts");
                }

                // Decode header
                String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
                JsonNode header = new ObjectMapper().readTree(headerJson);

                String alg = header.has("alg") ? header.get("alg").asText() : null;

                // Decode payload
                String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                JsonNode payload = new ObjectMapper().readTree(payloadJson);

                // Extract claims with defaults for missing fields
                String sub = payload.has("sub") ? payload.get("sub").asText() : "unknown";
                String roles = payload.has("roles") ? payload.get("roles").asText() : "";
                long exp = payload.has("exp") ? payload.get("exp").asLong() : Instant.now().getEpochSecond() + 3600;

                // Check expiration
                Instant expiresAt = Instant.ofEpochSecond(exp);
                if (expiresAt.isBefore(Instant.now())) {
                    throw new JwtException("JWT has expired");
                }

                Instant issuedAt = payload.has("iat") ? Instant.ofEpochSecond(payload.get("iat").asLong()) : Instant.now();

                // Build the JWT with authorities extracted from roles
                Map<String, Object> headers = Map.of(
                    "alg", alg != null ? alg : "none",
                    "typ", header.has("typ") ? header.get("typ").asText() : "JWT");

                Map<String, Object> claims = Map.of(
                    "sub", sub,
                    "roles", roles,
                    "exp", exp,
                    "iat", payload.has("iat") ? payload.get("iat").asLong() : issuedAt.getEpochSecond());

                return new Jwt(rawToken, issuedAt, expiresAt, headers, claims);
            } catch (JwtException e) {
                throw e;
            } catch (Exception e) {
                throw new JwtException("Failed to decode JWT: " + e.getMessage());
            }
        };
    }
}
