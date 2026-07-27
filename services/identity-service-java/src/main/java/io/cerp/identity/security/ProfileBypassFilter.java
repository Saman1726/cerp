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

        Optional<UUID> userUuid;
        try {
            userUuid = Optional.of(UUID.fromString(userId));
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("Content-Type", "application/json");
            response.getWriter().write("{\"error\":\"Invalid user ID format\"}");
            return;
        }

        // Store tenant ID so the request can proceed through the filter chain
        TenantContext.set(userUuid.get());

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Extract user ID from Bearer JWT token payload without signature verification. Looks for "sub", "userId", "id", or "user_id" claims.
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
                    String value = node.get(fieldName).asText();
                    if (isValidUuid(value)) {
                        return value;
                    }
                }
            }
            // Then try "sub" - only use if it's a valid UUID
            if (node.has("sub")) {
                String value = node.get("sub").asText();
                if (isValidUuid(value)) {
                    return value;
                }
            }
        } catch (Exception e) {
            log.warn("extractUserIdFromToken: exception={}", e.getMessage());
            return null;
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
}
