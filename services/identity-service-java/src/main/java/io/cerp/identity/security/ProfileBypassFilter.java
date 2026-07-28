package io.cerp.identity.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * This filter intercepts /users/profile requests BEFORE Spring Security's OAuth2 JWT validation. It uses FilterRegistrationBean with a low order to run at the servlet
 * filter level before the Spring Security filter chain.
 *
 * The filter resolves the user ID from the token. If the token contains a UUID (e.g., from proper JWT tokens), it sets up TenantContext and passes through. If the token
 * contains a non-UUID value (e.g., an email address from locally generated tokens), the filter passes through without setting TenantContext, allowing the UserController
 * to handle the email-to-user resolution.
 */
@Component
public class ProfileBypassFilter extends OncePerRequestFilter {

    private static final String PROFILE_PATH = "/users/profile";
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ProfileBypassFilter.class);

    public ProfileBypassFilter() {
        // No-arg constructor for FilterRegistrationBean
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Only intercept GET /users/profile
        if (!"GET".equals(request.getMethod()) || !PROFILE_PATH.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        String userId = extractUserIdFromToken(authHeader);

        if (userId == null || userId.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("Content-Type", "application/json");
            response.getWriter().write("{\"error\":\"No valid authorization\"}");
            return;
        }

        // Try to resolve as UUID first
        Optional<UUID> userUuid = tryParseUuid(userId);

        if (userUuid.isPresent()) {
            // UUID format: set tenant context and proceed
            TenantContext.set(userUuid.get());
            try {
                filterChain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
        } else {
            // Non-UUID format (e.g., email): pass through without TenantContext
            // The UserController will handle email-to-user resolution
            log.debug("ProfileBypassFilter: non-UUID user identifier '{}', passing through to controller", userId);
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Extract user ID from Bearer JWT token payload without signature verification. Returns the raw value (can be UUID or email) for downstream resolution.
     */
    private String extractUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String payload = parts[1];
            String base64 = payload.replace('-', '+').replace('_', '/');
            while (base64.length() % 4 != 0) {
                base64 += "=";
            }
            byte[] decoded = Base64.getDecoder().decode(base64);
            String json = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);

            JsonNode node = mapper.readTree(json);

            // First try field names that should contain UUIDs
            for (String fieldName : new String[] { "userId", "user_id", "id" }) {
                if (node.has(fieldName)) {
                    return node.get(fieldName).asText();
                }
            }
            // Then try "sub" - return as-is (can be email or UUID)
            if (node.has("sub")) {
                return node.get("sub").asText();
            }
        } catch (Exception e) {
            log.warn("extractUserIdFromToken: exception={}", e.getMessage());
        }
        return null;
    }

    private boolean isValidUuid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Optional<UUID> tryParseUuid(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
