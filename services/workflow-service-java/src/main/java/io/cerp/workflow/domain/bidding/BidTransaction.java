package io.cerp.workflow.domain.bidding;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Bid Transaction: Audit trail for all bid movements.
 * 
 * Transaction types:
 * - ALLOCATION: Admin allocates bids to wallet
 * - SPEND: User spends bids (bidding creation, vendor participation)
 * - REFUND: Bids are refunded back to wallet
 */
@Entity
@Table(name = "bid_transactions", schema = "bidding", indexes = {
        @Index(name = "idx_bid_transactions_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_bid_transactions_saga_id", columnList = "bidding_saga_id"),
        @Index(name = "idx_bid_transactions_user_id", columnList = "user_id"),
        @Index(name = "idx_bid_transactions_tender_id", columnList = "tender_id"),
        @Index(name = "idx_bid_transactions_type", columnList = "transaction_type")
})
public class BidTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bid_transactions_wallet"))
    private BidWallet wallet;

    @Column(name = "bidding_saga_id")
    private UUID biddingSagaId;

    @Column(name = "tender_id")
    private String tenderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 16)
    private TransactionType transactionType;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(name = "reference_type", length = 64)
    private String referenceType;

    @Column(name = "reference_id", length = 128)
    private String referenceId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Enums ──

    public enum TransactionType {
        ALLOCATION, SPEND, REFUND
    }

    // ── Constructors ──

    protected BidTransaction() {
    }

    public BidTransaction(UUID tenantId, UUID userId, BidWallet wallet, UUID biddingSagaId, String tenderId,
            TransactionType transactionType, Long amount, BigDecimal unitPrice, BigDecimal totalPrice,
            Long balanceAfter, String referenceType, String referenceId, String description) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.wallet = wallet;
        this.biddingSagaId = biddingSagaId;
        this.tenderId = tenderId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.balanceAfter = balanceAfter;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.description = description;
    }

    // ── Pre-persist/update hooks ──

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
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

    public BidWallet getWallet() {
        return wallet;
    }

    public void setWallet(BidWallet wallet) {
        this.wallet = wallet;
    }

    public UUID getBiddingSagaId() {
        return biddingSagaId;
    }

    public void setBiddingSagaId(UUID biddingSagaId) {
        this.biddingSagaId = biddingSagaId;
    }

    public String getTenderId() {
        return tenderId;
    }

    public void setTenderId(String tenderId) {
        this.tenderId = tenderId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Long getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}