package io.cerp.workflow.config;

import java.util.Optional;
import java.util.UUID;

/**
 * Thread-local holder for the current request's tenant id.
 * 
 * <p>
 * Populated by {@link io.cerp.workflow.security.TenantRequestFilter} from the X-Tenant-Id header. The header value is a tenant code (e.g., 'default') that is resolved to
 * a UUID by TenantService, then stored here for request-scoped access by all service layers.
 * </p>
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
        // Utility class
    }

    /**
     * Set the current tenant ID (UUID).
     * 
     * @param tenantId
     *            the UUID of the tenant
     */
    public static void set(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    /**
     * Get the current tenant ID as Optional.
     * 
     * @return Optional containing the tenant UUID if set, empty otherwise
     */
    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    /**
     * Get the current tenant ID directly (may be null).
     * 
     * @return the tenant UUID or null if not set
     * @deprecated Use {@link #get()} instead for null-safe access
     */
    @Deprecated
    public static UUID getCurrentTenantId() {
        return CURRENT.get();
    }

    /**
     * Clear the current tenant ID from the thread-local. Should be called at the end of each request.
     */
    public static void clear() {
        CURRENT.remove();
    }
}
