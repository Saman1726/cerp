package io.cerp.workflow.repository.bidding;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.cerp.workflow.domain.bidding.BidTransaction;

@Repository
public interface BidTransactionRepository extends JpaRepository<BidTransaction, UUID> {

    List<BidTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId);

    List<BidTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<BidTransaction> findByBiddingSagaIdOrderByCreatedAtDesc(UUID biddingSagaId);

    long countByUserIdAndCreatedAtBetween(UUID userId, java.time.LocalDateTime start, java.time.LocalDateTime end);
}