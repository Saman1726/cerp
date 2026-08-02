package io.cerp.workflow.repository.bidding;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import io.cerp.workflow.domain.bidding.BidWallet;

@Repository
public interface BidWalletRepository extends JpaRepository<BidWallet, UUID> {

    @Query("SELECT w FROM BidWallet w WHERE w.userId = :userId AND w.entityType = :entityType")
    Optional<BidWallet> findByUserIdAndEntityType(UUID userId, BidWallet.EntityType entityType);

    @Query("SELECT w FROM BidWallet w WHERE w.entityId = :entityId AND w.entityType = :entityType")
    Optional<BidWallet> findByEntityIdAndEntityType(UUID entityId, BidWallet.EntityType entityType);

    Optional<BidWallet> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}