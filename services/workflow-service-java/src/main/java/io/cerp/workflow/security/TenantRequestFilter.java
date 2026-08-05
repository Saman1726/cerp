package io.cerp.workflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.cerp.workflow.config.TenantContext;
import io.cerp.workflow.service.TenantService;

/**
 * Extracts tenant id from X-Tenant-Id header and resolves it to a UUID using TenantService, then stores it in TenantContext for the duration of the request.
 * 
 * <p>
 * The X-Tenant-Id header contains a tenant code (e.g., 'default', 'acme-corp'). This filter looks up the corresponding UUID via TenantService and stores it in
 * TenantContext for request-scoped access by all service layers.
 * </p>
 */
@Component
public class TenantRequestFilter extends OncePerRequestFilter {

    private static final String HEADER_TENANT_ID = "X-Tenant-Id";

    private final TenantService tenantService;

    public TenantRequestFilter(TenantService tenantService) {
        this.tenantService = tenantService;
    }

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
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }
        String value = header.trim();
        // Accept either a raw UUID or a tenant code string
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ignored) {
            return tenantService.getTenantIdByCode(value);
        }
    }
}
