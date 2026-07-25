package io.cerp.gateway.filter;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Global filter that extracts and propagates tenant context.
 *
 * <p>
 * Tenant resolution order:
 * <ol>
 * <li>X-Tenant-Id header from the incoming request</li>
 * <li>tenantId claim from the JWT access token</li>
 * <li>403 Forbidden if neither is available (for protected routes)</li>
 * </ol>
 *
 * <p>
 * The tenant ID is injected as X-Tenant-Id header on all downstream requests, enabling RLS (Row-Level Security) enforcement in backend services.
 *
 * <p>
 * Per Section 21: "tenant scoping and header propagation"
 */
@Component
public class TenantContextFilter implements GlobalFilter, Ordered {

    public static final String TENANT_ID_HEADER = "X-Tenant-Id";

    /**
     * Default tenant ID used when no tenant context is available in development environments. This matches the default tenant seeded by TenantSeedInitializer in
     * workflow-service.
     */
    public static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000001";

    @Value("${cerp.gateway.tenant.jwt-claim:resource_access}")
    private String jwtTenantClaimPath;

    @Value("${cerp.gateway.tenant.fallback-to-default:true}")
    private boolean fallbackToDefault;

    @Override
    @Order(-100)
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();

        // First, try to extract from existing header
        String tenantId = headers.getFirst(TENANT_ID_HEADER);

        if (tenantId == null || tenantId.isBlank()) {
            // Extract tenant from JWT token
            tenantId = extractTenantFromJwt(exchange);
        }

        if (tenantId != null && !tenantId.isBlank()) {
            ServerHttpRequest mutatedRequest = request.mutate()
                .header(TENANT_ID_HEADER, tenantId)
                .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        // Fallback to default tenant ID for development environments
        if (fallbackToDefault) {
            tenantId = DEFAULT_TENANT_ID;
            ServerHttpRequest mutatedRequest = request.mutate()
                .header(TENANT_ID_HEADER, tenantId)
                .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        // For public paths (health, etc.), allow without tenant context
        String path = request.getURI().getPath();
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // No tenant context available for a protected route — fail
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
        return exchange.getResponse().writeWith(
            Mono.just(exchange.getResponse().bufferFactory()
                .wrap("{\"error\":\"unauthorized\",\"message\":\"Tenant context required\"}".getBytes())));
    }

    /**
     * Attempt to extract tenant/organization ID from JWT claims. In Keycloak, the tenant mapping is typically stored in either: - resource_access.{client}.roles - A
     * custom "tenant_id" or "organization_id" claim
     */
    private String extractTenantFromJwt(ServerWebExchange exchange) {
        var authToken = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authToken == null || !authToken.startsWith("Bearer ")) {
            return null;
        }

        String token = authToken.substring(7);
        try {
            // Parse JWT payload (base64url decoded) to extract tenant claim
            // This is a lightweight parse; full validation is done by the resource server
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            // Simple JSON parsing for the tenant claim
            // Look for common Keycloak claim names
            String[] claimNames = { "tenant_id", "organization_id", "tenantId", "orgId" };
            for (String claim : claimNames) {
                String value = extractJsonValue(payload, claim);
                if (value != null) {
                    return value;
                }
            }

            // Try to get from resource_access map (Keycloak convention)
            String resourceAccess = extractJsonValue(payload, "resource_access");
            if (resourceAccess != null) {
                // Extract the first available client/roles context
                // In production, match against known gateway client ID
            }

            return null;
        } catch (Exception e) {
            // JWT parsing failed; let security handle validation errors
            return null;
        }
    }

    /**
     * Simple JSON value extraction without a full JSON library. Handles string values only.
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }

        int valueStart = json.indexOf(':', keyIndex + searchKey.length());
        if (valueStart == -1) {
            return null;
        }

        valueStart++;
        // Skip whitespace
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length()) {
            return null;
        }

        char c = json.charAt(valueStart);
        if (c == '"') {
            // String value
            int end = json.indexOf('"', valueStart + 1);
            if (end == -1) {
                return null;
            }
            return json.substring(valueStart + 1, end);
        }
        // For non-string values (numbers, booleans), return raw text until , or }
        int end = valueStart;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
            end++;
        }
        return json.substring(valueStart, end).trim();
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/actuator/")
                || path.equals("/api-docs")
                || path.startsWith("/api-docs/")
                || path.startsWith("/swagger-ui")
                || path.equals("/logout");
    }

    @Override
    public int getOrder() {
        // Runs after correlation ID filter (-200) but before security (-100 would be before,
        // but we set -100 explicitly for clarity). Actually, lower = earlier, so:
        // CorrelationIdFilter = -200, TenantContextFilter = -100
        return -100;
    }
}
