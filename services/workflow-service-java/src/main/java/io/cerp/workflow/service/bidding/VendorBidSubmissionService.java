package io.cerp.workflow.service.bidding;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.cerp.workflow.domain.bidding.VendorBidSubmission;
import io.cerp.workflow.repository.bidding.VendorBidSubmissionRepository;

/**
 * Service for managing vendor bid submissions in the bidding workflow.
 * Vendors submit actual bids (price, quantity, timeline) for tenders.
 */
@Service
public class VendorBidSubmissionService {

    private final VendorBidSubmissionRepository submissionRepository;

    public VendorBidSubmissionService(VendorBidSubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    /**
     * Submit a new bid for a tender.
     */
    @Transactional
    public VendorBidSubmission submitBid(String tenderId, UUID biddingSagaId, UUID vendorId, UUID vendorUserId,
            UUID tenantId, java.math.BigDecimal price, java.math.BigDecimal quantity,
            java.math.BigDecimal unitPrice, java.math.BigDecimal totalPrice,
            String timeline, String notes, String paymentMethod) {
        VendorBidSubmission submission = new VendorBidSubmission(
                tenderId, biddingSagaId, vendorId, vendorUserId, tenantId,
                price, quantity, unitPrice, totalPrice, timeline, notes, paymentMethod);
        return submissionRepository.save(submission);
    }

    /**
     * Get all bids for a specific bidding saga (tender).
     */
    @Transactional(readOnly = true)
    public List<VendorBidSubmission> getBidsBySagaId(UUID biddingSagaId) {
        return submissionRepository.findByBiddingSagaIdOrderBySubmittedAtDesc(biddingSagaId);
    }

    /**
     * Get all bids for a specific tender ID.
     */
    @Transactional(readOnly = true)
    public List<VendorBidSubmission> getBidsByTenderId(String tenderId) {
        return submissionRepository.findByTenderIdOrderBySubmittedAtDesc(tenderId);
    }

    /**
     * Get all bids submitted by a specific vendor.
     */
    @Transactional(readOnly = true)
    public List<VendorBidSubmission> getBidsByVendorId(UUID vendorId) {
        return submissionRepository.findByVendorIdOrderBySubmittedAtDesc(vendorId);
    }

    /**
     * Get all bids submitted by a specific vendor user.
     */
    @Transactional(readOnly = true)
    public List<VendorBidSubmission> getBidsByVendorUserId(UUID vendorUserId) {
        return submissionRepository.findByVendorUserIdOrderBySubmittedAtDesc(vendorUserId);
    }

    /**
     * Get bids for a saga with specific status.
     */
    @Transactional(readOnly = true)
    public List<VendorBidSubmission> getBidsBySagaIdAndStatus(UUID biddingSagaId, VendorBidSubmission.BidStatus status) {
        return submissionRepository.findByBiddingSagaIdAndStatusOrderBySubmittedAtDesc(biddingSagaId, status);
    }

    /**
     * Get count of bids with specific status for a saga.
     */
    @Transactional(readOnly = true)
    public long countBidsBySagaIdAndStatus(UUID biddingSagaId, VendorBidSubmission.BidStatus status) {
        return submissionRepository.countByBiddingSagaIdAndStatus(biddingSagaId, status);
    }

    /**
     * Get total number of SUBMITTED bids for a bidding saga (used by CollectBids step).
     */
    @Transactional(readOnly = true)
    public long countSubmittedBidsForSaga(UUID biddingSagaId) {
        return submissionRepository.countByBiddingSagaIdAndStatus(biddingSagaId, VendorBidSubmission.BidStatus.SUBMITTED);
    }

    /**
     * Check if a vendor has already submitted a bid for this bidding saga.
     */
    @Transactional(readOnly = true)
    public boolean vendorHasSubmittedBid(UUID biddingSagaId, UUID vendorId) {
        return submissionRepository.existsByBiddingSagaIdAndVendorId(biddingSagaId, vendorId);
    }

    /**
     * Award a specific bid (mark as AWARDED).
     */
    @Transactional
    public VendorBidSubmission awardBid(UUID bidId, String purchaseOrderId) {
        VendorBidSubmission submission = submissionRepository.findById(bidId)
                .orElseThrow(() -> new IllegalArgumentException("Bid not found: " + bidId));
        submission.award(purchaseOrderId);
        return submissionRepository.save(submission);
    }

    /**
     * Reject a specific bid (mark as REJECTED).
     */
    @Transactional
    public VendorBidSubmission rejectBid(UUID bidId) {
        VendorBidSubmission submission = submissionRepository.findById(bidId)
                .orElseThrow(() -> new IllegalArgumentException("Bid not found: " + bidId));
        submission.reject();
        return submissionRepository.save(submission);
    }

    /**
     * Get a single bid by ID.
     */
    @Transactional(readOnly = true)
    public Optional<VendorBidSubmission> getBidById(UUID bidId) {
        return submissionRepository.findById(bidId);
    }

    /**
     * Convert VendorBidSubmission to Map for saga context data (legacy compatibility).
     */
    public static java.util.Map<String, Object> toMap(VendorBidSubmission submission) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("bidId", submission.getId().toString());
        map.put("vendorId", submission.getVendorId().toString());
        map.put("price", submission.getPrice().doubleValue());
        map.put("quantity", submission.getQuantity().doubleValue());
        map.put("unitPrice", submission.getUnitPrice().doubleValue());
        map.put("totalPrice", submission.getTotalPrice().doubleValue());
        map.put("timeline", submission.getTimeline());
        map.put("notes", submission.getNotes());
        map.put("status", submission.getStatus().name());
        map.put("submittedAt", submission.getSubmittedAt().toString());
        return map;
    }

    /**
     * Convert list of VendorBidSubmission to list of Maps for saga context data.
     */
    public static List<java.util.Map<String, Object>> toMapList(List<VendorBidSubmission> submissions) {
        return submissions.stream()
                .map(VendorBidSubmissionService::toMap)
                .collect(java.util.stream.Collectors.toList());
    }
}