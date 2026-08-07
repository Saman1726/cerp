package io.cerp.identity.domain;

/**
 * Subscription plan tier. Top-level (not nested under Subscription/PlanLimits) since both entities reference it.
 */
public enum Plan {
    FREEMIUM, PROFESSIONAL, PREMIUM
}
