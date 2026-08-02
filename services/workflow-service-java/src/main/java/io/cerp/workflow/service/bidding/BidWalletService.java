package io.cerp.workflow.service.bidding;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.cerp.workflow.domain.bidding.BidTransaction;
import io.cerp.workflow.domain.bidding.BidTransaction.TransactionType;
import io.cerp.workflow.domain.bidding.BidWallet;
import io.cerp.workflow.repository.bidding.BidTransactionRepository;
import io.cerp.workflow.repository.bidding.BidWalletRepository;

/**
 * Service for managing bid wallets and bid transactions.
 * 
 * Business rules:
 * - Project Owners pay 1 bid to create a bidding
 * - Vendors pay 5 bids to participate in OPEN_TENDER biddings
 * - Vendors pay 0 bids for INVITED biddings
 * - Bids can only be purchased or allocated by app admin
 */
@Service
public class BidWalletService {

    private static final Logger log = LoggerFactory.getLogger(BidWalletService.class);

    public static final long BIDDING_CREATION_COST = 1L;
    public static final long OPEN_TENDER_PARTICIPATION_COST = 5L;
    public static final BigDecimal BID_UNIT_PRICE_EUR = new BigDecimal("10.00");

    private final BidWalletRepository walletRepository;
    private final BidTransactionRepository transactionRepository;

    public BidWalletService(BidWalletRepository walletRepository, BidTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Get or create a wallet for a user.
     */
    @Transactional(readOnly = true)
    public Optional<BidWallet> getWallet(UUID userId, BidWallet.EntityType entityType) {
        return walletRepository.findByUserIdAndEntityType(userId, entityType);
    }

    /**
     * Check if a wallet exists for a user.
     */
    @Transactional(readOnly = true)
    public boolean hasWallet(UUID userId) {
        return walletRepository.existsByUserId(userId);
    }

    /**
     * Allocate bids to a user's wallet (admin only operation).
     * @throws IllegalArgumentException if amount is not positive
     */
    @Transactional
    public BidWallet allocateBids(UUID userId, BidWallet.EntityType entityType, UUID entityId, String tenantId,
            Long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Allocation amount must be positive");
        }

        BidWallet wallet = ensureWalletExists(userId, entityType, entityId, tenantId);
        wallet.allocateBids(amount);
        walletRepository.save(wallet);

        // Record allocation transaction
        BidTransaction transaction = new BidTransaction(
                tenantId,
                userId,
                wallet,
                null,
                null,
                TransactionType.ALLOCATION,
                amount,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                wallet.getAvailableBids(),
                "ADMIN_ALLOCATION",
                null,
                "Admin allocated " + amount + " bids");
        transactionRepository.save(transaction);

        log.info("Bids allocated: userId={}, entityType={}, entityId={}, amount={}, newBalance={}",
                userId, entityType, entityId, amount, wallet.getAvailableBids());
        return wallet;
    }

    /**
     * Spend bids for bidding creation (Project Owner).
     * @throws InsufficientBidsException if the user doesn't have enough bids
     */
    @Transactional
    public BidTransaction spendForBiddingCreation(UUID userId, BidWallet.EntityType entityType, UUID entityId,
            String tenantId, UUID biddingSagaId, String tenderId) {
        return spendBids(userId, entityType, entityId, tenantId, BIDDING_CREATION_COST,
                biddingSagaId, tenderId, "BIDDING_CREATION",
                "Bid creation cost for bidding saga " + biddingSagaId);
    }

    /**
     * Spend bids for vendor participation in OPEN_TENDER.
     * @throws InsufficientBidsException if the vendor doesn't have enough bids
     */
    @Transactional
    public BidTransaction spendForOpenTenderParticipation(UUID userId, BidWallet.EntityType entityType, UUID entityId,
            String tenantId, UUID biddingSagaId, String tenderId, UUID vendorId) {
        return spendBids(userId, entityType, entityId, tenantId, OPEN_TENDER_PARTICIPATION_COST,
                biddingSagaId, tenderId, "VENDOR_PARTICIPATION",
                "Open tender participation cost for tender " + tenderId + " (vendor: " + vendorId + ")");
    }

    /**
     * Refund bids back to wallet (e.g., when bidding is cancelled).
     */
    @Transactional
    public BidTransaction refundBids(UUID userId, BidWallet.EntityType entityType, UUID entityId, String tenantId,
            Long amount, UUID biddingSagaId, String tenderId, String referenceId, String description) {
        BidWallet wallet = walletRepository.findByUserIdAndEntityType(userId, entityType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Wallet not found for user: " + userId + ", entityType: " + entityType));

        wallet.creditBids(amount);
        walletRepository.save(wallet);

        BidTransaction transaction = new BidTransaction(
                tenantId,
                userId,
                wallet,
                biddingSagaId,
                tenderId,
                TransactionType.REFUND,
                amount,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                wallet.getAvailableBids(),
                referenceId,
                null,
                description);
        return transactionRepository.save(transaction);
    }

    /**
     * Get the available bids for a user.
     */
    @Transactional(readOnly = true)
    public Long getAvailableBids(UUID userId, BidWallet.EntityType entityType) {
        return walletRepository.findByUserIdAndEntityType(userId, entityType)
                .map(BidWallet::getAvailableBids)
                .orElse(0L);
    }

    /**
     * Check if user has sufficient bids for a specific operation.
     */
    public boolean hasSufficientBids(UUID userId, BidWallet.EntityType entityType, long requiredBids) {
        return walletRepository.findByUserIdAndEntityType(userId, entityType)
                .map(w -> w.getAvailableBids() != null && w.getAvailableBids() >= requiredBids)
                .orElse(false);
    }

    // ── Custom Exception ──

    public static class InsufficientBidsException extends RuntimeException {
        private final Long available;
        private final long required;

        public InsufficientBidsException(Long available, long required, String operation) {
            super(String.format(
                    "Not enough credits to %s. This %s costs %d bid(s) (%.2f€). Please purchase bids to continue.",
                    operation,
                    required == 1 ? "operation" : "participation",
                    required,
                    required * BID_UNIT_PRICE_EUR.doubleValue()));
            this.available = available;
            this.required = required;
        }

        public Long getAvailable() {
            return available;
        }

        public long getRequired() {
            return required;
        }
    }

    // ── Private Methods ──

    private BidTransaction spendBids(UUID userId, BidWallet.EntityType entityType, UUID entityId, String tenantId,
            long amount, UUID biddingSagaId, String tenderId, String referenceType, String description) {
        BidWallet wallet = ensureWalletExists(userId, entityType, entityId, tenantId);

        try {
            wallet.debitBids(amount);
            walletRepository.save(wallet);
        } catch (IllegalStateException e) {
            throw new InsufficientBidsException(
                    wallet.getAvailableBids() != null ? wallet.getAvailableBids() : 0L,
                    amount,
                    referenceType.toLowerCase());
        }

        BidTransaction transaction = new BidTransaction(
                tenantId,
                userId,
                wallet,
                biddingSagaId,
                tenderId,
                TransactionType.SPEND,
                amount,
                BID_UNIT_PRICE_EUR,
                BigDecimal.valueOf(amount * BID_UNIT_PRICE_EUR.longValue()),
                wallet.getAvailableBids(),
                referenceType,
                biddingSagaId != null ? biddingSagaId.toString() : null,
                description);
        return transactionRepository.save(transaction);
    }

    private BidWallet ensureWalletExists(UUID userId, BidWallet.EntityType entityType, UUID entityId, String tenantId) {
        return walletRepository.findByUserIdAndEntityType(userId, entityType)
                .orElseGet(() -> {
                    BidWallet newWallet = new BidWallet(userId, entityType, entityId, tenantId);
                    return walletRepository.save(newWallet);
                });
    }
}