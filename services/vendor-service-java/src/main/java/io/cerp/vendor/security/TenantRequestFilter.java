package io.cerp.vendor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Extracts tenant id and user id from X-Tenant-Id/X-User-Id headers or JWT claims and stores
 * them in TenantContext/UserContext for the duration of the request.
 */
@Component
public class TenantRequestFilter extends OncePerRequestFilter {

    private static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String HEADER_USER_ID = "X-User-Id";
    private List<String> excludePatterns = List.of();

    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        // Skip tenant/user context extraction for excluded paths
        boolean shouldExclude = excludePatterns.stream().anyMatch(pattern -> {
            if (pattern.endsWith("/**")) {
                String basepath = pattern.substring(0, pattern.length() - 3);
                return path.equals(basepath) || path.startsWith(basepath + "/");
            }
            return path.equals(pattern);
        });

        if (!shouldExclude) {
            try {
                Optional<UUID> tenantId = resolveTenantId(request);
                tenantId.ifPresent(TenantContext::set);
            } catch (Exception e) {
                // Ignore tenant resolution errors for non-authenticated requests
            }
            try {
                Optional<UUID> userId = resolveUserId(request);
                userId.ifPresent(UserContext::set);
            } catch (Exception e) {
                // Ignore user resolution errors for non-authenticated requests
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            UserContext.clear();
        }
    }

    private Optional<UUID> resolveTenantId(HttpServletRequest request) {
        String header = request.getHeader(HEADER_TENANT_ID);
        if (header != null && !header.isBlank()) {
            try {
                return Optional.of(UUID.fromString(header.trim()));
            } catch (IllegalArgumentException ignored) {
                // fall through to JWT claim parsing
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            Object val = null;
            if (jwt.hasClaim("tenantId")) {
                val = jwt.getClaim("tenantId");
            } else if (jwt.hasClaim("tenant_id")) {
                val = jwt.getClaim("tenant_id");
            } else if (jwt.hasClaim("tid")) {
                val = jwt.getClaim("tid");
            }
            if (val instanceof String s) {
                try {
                    return Optional.of(UUID.fromString(s));
                } catch (IllegalArgumentException ignored) {
                    // ignore invalid format
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Mirrors resolveTenantId: header first, then a JWT claim fallback. "internal_user_id" is
     * the only user-identity claim identity-service guarantees on real Keycloak tokens - the
     * token's own "sub" is Keycloak's internal id, not identity.user_accounts.id (see
     * KeycloakBootstrap in identity-service).
     */
    private Optional<UUID> resolveUserId(HttpServletRequest request) {
        String header = request.getHeader(HEADER_USER_ID);
        if (header != null && !header.isBlank()) {
            try {
                return Optional.of(UUID.fromString(header.trim()));
            } catch (IllegalArgumentException ignored) {
                // fall through to JWT claim parsing
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            if (jwt.hasClaim("internal_user_id") && jwt.getClaim("internal_user_id") instanceof String s) {
                try {
                    return Optional.of(UUID.fromString(s));
                } catch (IllegalArgumentException ignored) {
                    // ignore invalid format
                }
            }
        }
        return Optional.empty();
    }
}
