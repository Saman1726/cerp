package io.cerp.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "memberships", schema = "identity")
public class Membership {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Roles stored as PostgreSQL JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "roles", nullable = false, columnDefinition = "jsonb")
    private List<String> roles;

    // Scopes stored as JSONB (Hibernate 6)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scopes", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> scopes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Membership() {
    }

    public Membership(UUID id, UUID tenantId, UUID userId, List<String> roles, Map<String, Object> scopes) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.roles = roles;
        this.scopes = scopes;
    }

    @PrePersist
    public void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
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

    public Membership setId(UUID id) {
        this.id = id;
        return this;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public Membership setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public UUID getUserId() {
        return userId;
    }

    public Membership setUserId(UUID userId) {
        this.userId = userId;
        return this;
    }

    public List<String> getRoles() {
        return roles;
    }

    public Membership setRoles(List<String> roles) {
        this.roles = roles;
        return this;
    }

    public Membership setRoles(String... roleArray) {
        this.roles = roleArray != null ? List.of(roleArray) : List.of();
        return this;
    }

    public Map<String, Object> getScopes() {
        return scopes;
    }

    public Membership setScopes(Map<String, Object> scopes) {
        this.scopes = scopes;
        return this;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
