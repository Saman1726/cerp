package io.cerp.vendor.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Thread-local holder for the current request's user id, set by TenantRequestFilter from the
 * X-User-Id header or the JWT's "internal_user_id" claim.
 */
public final class UserContext {
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UUID userId) {
        CURRENT.set(userId);
    }

    public static Optional<UUID> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }
}
