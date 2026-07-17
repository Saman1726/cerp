package io.cerp.workflow.config;

/**
 * Thread-local holder for the current tenant identity.
 *
 * <p>
 * Populated by a servlet filter/interceptor from the JWT token issued by Keycloak. All repository queries are scoped by tenantId enforced at the service layer.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Utility class
    }

    public static String getCurrentTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void setCurrentTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
