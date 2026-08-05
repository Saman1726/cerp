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
 * Vendor Bid Submission: The actual bid (price, quantity, timeline) submitted by a vendor.
 * 
 * Status flow: SUBMITTED → AWARDED or REJECTED
 */
@Entity
@Table(name = "vendor_bid_submissions", schema = "bidding", indexes = {
        @Index(name = "idx_vendor_submissions_saga_id", columnList = "bidding_saga_id"),
        @Index(name = "idx_vendor_submissions_tender_id", columnList = "tender_id"),
        @Index(name = "idx_vendor_submissions_vendor", columnList = "vendor_id"),
        @Index(name = "idx_vendor_submissions_status", columnList = "status")
})
public class VendorBidSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tender_id", nullable = false)
    private String tenderId;

    @Column(name = "bidding_saga_id", nullable = false)
    private UUID biddingSagaId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "vendor_user_id")
    private UUID vendorUserId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "timeline", length = 255)
    private String timeline;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "payment_method", length = 64)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private BidStatus status = BidStatus.SUBMITTED;

    @Column(name = "awarded_at")
    private LocalDateTime awardedAt;

    @Column(name = "purchase_order_id", length = 128)
    private String purchaseOrderId;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Enums ──

    public enum BidStatus {
        SUBMITTED, AWARDED, REJECTED
    }

    // ── Constructors ──

    protected VendorBidSubmission() {
    }

    public VendorBidSubmission(String tenderId, UUID biddingSagaId, UUID vendorId, UUID vendorUserId,
            UUID tenantId, BigDecimal price, BigDecimal quantity, BigDecimal unitPrice, BigDecimal totalPrice,
            String timeline, String notes, String paymentMethod) {
        this.tenderId = tenderId;
        this.biddingSagaId = biddingSagaId;
        this.vendorId = vendorId;
        this.vendorUserId = vendorUserId;
        this.tenantId = tenantId;
        this.price = price;
        this.quantity = quantity != null ? quantity : BigDecimal.ONE;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.timeline = timeline;
        this.notes = notes;
        this.paymentMethod = paymentMethod;
    }

    /**
     * Mark this bid as awarded.
     */
    public void award(String purchaseOrderId) {
        this.status = BidStatus.AWARDED;
        this.awardedAt = LocalDateTime.now();
        this.purchaseOrderId = purchaseOrderId;
    }

    /**
     * Reject this bid.
     */
    public void reject() {
        this.status = BidStatus.REJECTED;
    }

    // ── Pre-persist/update hooks ──

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.submittedAt = LocalDateTime.now();
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

    public String getTenderId() {
        return tenderId;
    }

    public void setTenderId(String tenderId) {
        this.tenderId = tenderId;
    }

    public UUID getBiddingSagaId() {
        return biddingSagaId;
    }

    public void setBiddingSagaId(UUID biddingSagaId) {
        this.biddingSagaId = biddingSagaId;
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public void setVendorId(UUID vendorId) {
        this.vendorId = vendorId;
    }

    public UUID getVendorUserId() {
        return vendorUserId;
    }

    public void setVendorUserId(UUID vendorUserId) {
        this.vendorUserId = vendorUserId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
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

    public String getTimeline() {
        return timeline;
    }

    public void setTimeline(String timeline) {
        this.timeline = timeline;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BidStatus getStatus() {
        return status;
    }

    public void setStatus(BidStatus status) {
        this.status = status;
    }

    public LocalDateTime getAwardedAt() {
        return awardedAt;
    }

    public void setAwardedAt(LocalDateTime awardedAt) {
        this.awardedAt = awardedAt;
    }

    public String getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(String purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
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