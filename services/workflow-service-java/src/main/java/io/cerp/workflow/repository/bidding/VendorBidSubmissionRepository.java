package io.cerp.workflow.repository.bidding;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.cerp.workflow.domain.bidding.VendorBidSubmission;

@Repository
public interface VendorBidSubmissionRepository extends JpaRepository<VendorBidSubmission, UUID> {

    List<VendorBidSubmission> findByBiddingSagaIdOrderBySubmittedAtDesc(UUID biddingSagaId);

    List<VendorBidSubmission> findByTenderIdOrderBySubmittedAtDesc(String tenderId);

    List<VendorBidSubmission> findByVendorIdOrderBySubmittedAtDesc(UUID vendorId);

    List<VendorBidSubmission> findByVendorUserIdOrderBySubmittedAtDesc(UUID vendorUserId);

    List<VendorBidSubmission> findByBiddingSagaIdAndStatusOrderBySubmittedAtDesc(UUID biddingSagaId,
            VendorBidSubmission.BidStatus status);

    long countByBiddingSagaIdAndStatus(UUID biddingSagaId, VendorBidSubmission.BidStatus status);

    boolean existsByBiddingSagaIdAndVendorId(UUID biddingSagaId, UUID vendorId);
}