package io.cerp.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A user account's Subscription. Every account - regardless of AccountType - has exactly one, keyed by user_id (not tenant_id): a Company's plan does not cover every user
 * under its tenant as a shared entitlement.
 */
@Entity
@Table(name = "subscriptions", schema = "identity")
public class Subscription {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    private Plan plan;

    @Column(name = "status", nullable = false)
    private String status = "active";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Subscription() {
    }

    public Subscription(UUID userId, Plan plan) {
        this.userId = userId;
        this.plan = plan;
        this.status = "active";
    }

    @PrePersist
    public void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = "active";
        }
        this.createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Subscription setId(UUID id) {
        this.id = id;
        return this;
    }

    public UUID getUserId() {
        return userId;
    }

    public Subscription setUserId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public Plan getPlan() {
        return plan;
    }

    public Subscription setPlan(Plan plan) {
        this.plan = plan;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public Subscription setStatus(String status) {
        this.status = status;
        return this;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
