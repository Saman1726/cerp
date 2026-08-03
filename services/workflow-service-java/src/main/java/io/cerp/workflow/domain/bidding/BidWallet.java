package io.cerp.workflow.domain.bidding;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Bid Wallet: One wallet per user to track bid credits.
 * Bids can only be purchased or allocated by app admin.
 * 
 * Entity types:
 * - PROJECT_OWNER: Pays bids to create biddings (1 bid per bidding)
 * - VENDOR: Pays bids to participate in OPEN_TENDER biddings (5 bids per participation)
 */
@Entity
@Table(name = "bid_wallets", schema = "bidding", indexes = {
        @Index(name = "idx_bid_wallets_user_id", columnList = "user_id"),
        @Index(name = "idx_bid_wallets_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_bid_wallets_tenant_id", columnList = "tenant_id")
})
public class BidWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 16)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "total_bids", nullable = false)
    private Long totalBids = 0L;

    @Column(name = "spent_bids", nullable = false)
    private Long spentBids = 0L;

    @Column(name = "available_bids", nullable = false)
    private Long availableBids = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Enums ──

    public enum EntityType {
        PROJECT_OWNER, VENDOR
    }

    // ── Constructors ──

    protected BidWallet() {
    }

    public BidWallet(UUID userId, EntityType entityType, UUID entityId, UUID tenantId) {
        this.userId = userId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.tenantId = tenantId;
        this.totalBids = 0L;
        this.spentBids = 0L;
        this.availableBids = 0L;
    }

    // ── Business methods ──

    /**
     * Allocate bids to this wallet (admin only).
     */
    public void allocateBids(Long amount) {
        this.totalBids = (this.totalBids != null ? this.totalBids : 0L) + amount;
        this.availableBids = (this.availableBids != null ? this.availableBids : 0L) + amount;
    }

    /**
     * Debit (spend) bids from this wallet.
     * @throws IllegalStateException if insufficient bids
     */
    public void debitBids(Long amount) {
        if (amount <= 0) return;
        if (this.availableBids == null || this.availableBids < amount) {
            throw new IllegalStateException(
                String.format("Insufficient bids: have %d, need %d", this.availableBids, amount));
        }
        this.spentBids = (this.spentBids != null ? this.spentBids : 0L) + amount;
        this.availableBids = this.availableBids - amount;
    }

    /**
     * Credit (refund) bids back to this wallet.
     */
    public void creditBids(Long amount) {
        if (amount <= 0) return;
        if (this.spentBids != null && this.spentBids >= amount) {
            this.spentBids = this.spentBids - amount;
        }
        this.availableBids = (this.availableBids != null ? this.availableBids : 0L) + amount;
    }

    // ── Pre-persist/update hooks ──

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Getters and Setters ──

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public Long getTotalBids() {
        return totalBids;
    }

    public void setTotalBids(Long totalBids) {
        this.totalBids = totalBids;
    }

    public Long getSpentBids() {
        return spentBids;
    }

    public void setSpentBids(Long spentBids) {
        this.spentBids = spentBids;
    }

    public Long getAvailableBids() {
        return availableBids;
    }

    public void setAvailableBids(Long availableBids) {
        this.availableBids = availableBids;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}