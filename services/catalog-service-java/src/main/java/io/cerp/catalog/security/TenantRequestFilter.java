package io.cerp.catalog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Extracts tenant id from X-Tenant-Id header or JWT claims and stores it in TenantContext for the duration of the request.
 */
@Component
public class TenantRequestFilter extends OncePerRequestFilter {

    private static final String HEADER_TENANT_ID = "X-Tenant-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Optional<UUID> tenantId = resolveTenantId(request);
            tenantId.ifPresent(TenantContext::set);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
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
}
