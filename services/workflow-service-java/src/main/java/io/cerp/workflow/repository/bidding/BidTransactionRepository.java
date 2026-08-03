package io.cerp.workflow.repository.bidding;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.cerp.workflow.domain.bidding.BidTransaction;
import io.cerp.workflow.domain.bidding.BidTransaction.TransactionType;

@Repository
public interface BidTransactionRepository extends JpaRepository<BidTransaction, UUID> {

    List<BidTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId);

    List<BidTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<BidTransaction> findByBiddingSagaIdOrderByCreatedAtDesc(UUID biddingSagaId);

    long countByUserIdAndCreatedAtBetween(UUID userId, java.time.LocalDateTime start, java.time.LocalDateTime end);

    // Pagination support

    List<BidTransaction> findByWalletId(UUID walletId, Pageable pageable);

    List<BidTransaction> findByWalletIdAndTransactionType(UUID walletId, TransactionType type, Pageable pageable);

    List<BidTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);

    List<BidTransaction> findByWalletIdAndTransactionTypeOrderByCreatedAtDesc(UUID walletId, TransactionType type,
            Pageable pageable);
}
