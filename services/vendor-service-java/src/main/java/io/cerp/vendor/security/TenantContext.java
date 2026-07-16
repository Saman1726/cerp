package io.cerp.vendor.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Thread-local holder for the current request's tenant id. Used to bind PostgreSQL RLS context via SET LOCAL app.tenant_id in the DB session.
 */
public final class TenantContext {
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
