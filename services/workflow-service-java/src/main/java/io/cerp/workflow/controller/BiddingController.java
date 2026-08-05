package io.cerp.workflow.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cerp.workflow.config.TenantContext;
import io.cerp.workflow.domain.bidding.BidTransaction;
import io.cerp.workflow.domain.bidding.BidWallet;
import io.cerp.workflow.domain.bidding.VendorBidSubmission;
import io.cerp.workflow.repository.SagaRepository;
import io.cerp.workflow.service.bidding.BidWalletService;
import io.cerp.workflow.service.bidding.BidWalletService.InsufficientBidsException;
import io.cerp.workflow.service.bidding.ChannelSubscriptionService;
import io.cerp.workflow.service.bidding.VendorBidSubmissionService;

/**
 * REST API for bidding workflow operations.
 * 
 * <p>
 * Endpoints for vendor bid submission and bid wallet management:
 * <ul>
 * <li>POST /api/v1/bidding/submissions - Submit a vendor bid</li>
 * <li>GET /api/v1/bidding/submissions - Get bids for a bidding saga</li>
 * <li>GET /api/v1/bidding/wallet/balance - Get bid wallet balance</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/bidding")
public class BiddingController {

    private static final Logger log = LoggerFactory.getLogger(BiddingController.class);

    private final VendorBidSubmissionService bidSubmissionService;
    private final BidWalletService bidWalletService;
    private final ChannelSubscriptionService channelSubscriptionService;
    private final SagaRepository sagaRepository;
    private final ObjectMapper objectMapper;

    public BiddingController(VendorBidSubmissionService bidSubmissionService,
                              BidWalletService bidWalletService,
                              ChannelSubscriptionService channelSubscriptionService,
                              SagaRepository sagaRepository,
                              ObjectMapper objectMapper) {
        this.bidSubmissionService = bidSubmissionService;
        this.bidWalletService = bidWalletService;
        this.channelSubscriptionService = channelSubscriptionService;
        this.sagaRepository = sagaRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Submit a new vendor bid for a tender/bidding saga.
     *
     * For OPEN_TENDER: vendor pays 5 bids from their wallet.
     * For INVITED: vendor submits for free — only the project owner pays.
     */
    @PostMapping("/submissions")
    public ResponseEntity<Map<String, Object>> submitBid(@RequestBody SubmitBidRequest request) {
        try {
            UUID tenantId = getCurrentTenantId();
            UUID vendorUserId = getCurrentUserId();

            // Check if vendor already submitted a bid for this saga
            if (bidSubmissionService.vendorHasSubmittedBid(request.biddingSagaId(), vendorUserId)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "DUPLICATE_BID");
                error.put("message", "Vendor has already submitted a bid for this bidding saga");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            boolean isOpenTender = !"INVITED".equalsIgnoreCase(request.biddingType());

            if (isOpenTender) {
                // Check if vendor has sufficient bids in wallet
                if (!bidWalletService.hasSufficientBids(vendorUserId, BidWallet.EntityType.VENDOR,
                        BidWalletService.OPEN_TENDER_PARTICIPATION_COST)) {
                    Long available = bidWalletService.getAvailableBids(vendorUserId, BidWallet.EntityType.VENDOR);
                    InsufficientBidsException ex = new InsufficientBidsException(available,
                            BidWalletService.OPEN_TENDER_PARTICIPATION_COST, "submit bid");
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "INSUFFICIENT_BIDS");
                    error.put("message", ex.getMessage());
                    error.put("availableBids", available);
                    error.put("requiredBids", BidWalletService.OPEN_TENDER_PARTICIPATION_COST);
                    return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error);
                }
            }

            // Calculate unit price if not provided
            BigDecimal quantity = request.quantity() != null ? request.quantity() : BigDecimal.ONE;
            BigDecimal unitPrice = request.unitPrice() != null ? request.unitPrice()
                    : request.price().divide(quantity, 2, BigDecimal.ROUND_HALF_UP);
            BigDecimal totalPrice = request.totalPrice() != null ? request.totalPrice()
                    : request.price();

            if (isOpenTender) {
                // Spend bids from vendor wallet
                bidWalletService.spendForOpenTenderParticipation(
                        vendorUserId,
                        BidWallet.EntityType.VENDOR,
                        vendorUserId,
                        tenantId,
                        request.biddingSagaId(),
                        request.tenderId(),
                        request.vendorId());
            }

            // Submit the bid
            var submission = bidSubmissionService.submitBid(
                    request.tenderId(),
                    request.biddingSagaId(),
                    request.vendorId(),
                    vendorUserId,
                    tenantId,
                    request.price(),
                    quantity,
                    unitPrice,
                    totalPrice,
                    request.timeline(),
                    request.notes(),
                    request.paymentMethod());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            
            Map<String, Object> bidData = new HashMap<>();
            bidData.put("id", submission.getId().toString());
            bidData.put("tenderId", submission.getTenderId());
            bidData.put("biddingSagaId", submission.getBiddingSagaId().toString());
            bidData.put("vendorId", submission.getVendorId().toString());
            bidData.put("vendorUserId", submission.getVendorUserId() != null
                    ? submission.getVendorUserId().toString()
                    : null);
            bidData.put("price", submission.getPrice().toString());
            bidData.put("quantity", submission.getQuantity().toString());
            bidData.put("unitPrice", submission.getUnitPrice().toString());
            bidData.put("totalPrice", submission.getTotalPrice().toString());
            bidData.put("timeline", submission.getTimeline());
            bidData.put("notes", submission.getNotes());
            bidData.put("paymentMethod", submission.getPaymentMethod());
            bidData.put("status", submission.getStatus().name());
            bidData.put("submittedAt", submission.getSubmittedAt().toString());
            
            response.put("bid", bidData);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "VALIDATION_ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (InsufficientBidsException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "INSUFFICIENT_BIDS");
            error.put("message", e.getMessage());
            error.put("availableBids", e.getAvailable());
            error.put("requiredBids", e.getRequired());
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "SUBMISSION_FAILED");
            error.put("message", "Failed to submit bid: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get all bids for a specific bidding saga (tender).
     * Only visible to the project owner/organizer.
     */
    @GetMapping("/submissions")
    public ResponseEntity<Map<String, Object>> getBids(
            @RequestParam UUID biddingSagaId,
            @RequestParam(required = false) String tenderId) {
        try {
            List<VendorBidSubmission> submissions;

            if (biddingSagaId != null) {
                submissions = bidSubmissionService.getBidsBySagaId(biddingSagaId);
            } else if (tenderId != null) {
                submissions = bidSubmissionService.getBidsByTenderId(tenderId);
            } else {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "VALIDATION_ERROR");
                error.put("message", "Either biddingSagaId or tenderId must be provided");
                return ResponseEntity.badRequest().body(error);
            }

            List<Map<String, Object>> bids = submissions.stream().map(s -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", s.getId().toString());
                map.put("tenderId", s.getTenderId());
                map.put("biddingSagaId", s.getBiddingSagaId().toString());
                map.put("vendorId", s.getVendorId().toString());
                map.put("vendorUserId", s.getVendorUserId() != null ? s.getVendorUserId().toString() : null);
                map.put("price", s.getPrice().toString());
                map.put("quantity", s.getQuantity().toString());
                map.put("unitPrice", s.getUnitPrice().toString());
                map.put("totalPrice", s.getTotalPrice().toString());
                map.put("timeline", s.getTimeline());
                map.put("notes", s.getNotes());
                map.put("paymentMethod", s.getPaymentMethod());
                map.put("status", s.getStatus().name());
                map.put("awardedAt", s.getAwardedAt());
                map.put("purchaseOrderId", s.getPurchaseOrderId());
                map.put("submittedAt", s.getSubmittedAt().toString());
                return map;
            }).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("bids", bids);
            response.put("totalBids", bids.size());
            response.put("submittedCount", bidSubmissionService.countBidsBySagaIdAndStatus(
                    biddingSagaId, VendorBidSubmission.BidStatus.SUBMITTED));
            response.put("awardedCount", bidSubmissionService.countBidsBySagaIdAndStatus(
                    biddingSagaId, VendorBidSubmission.BidStatus.AWARDED));
            response.put("rejectedCount", bidSubmissionService.countBidsBySagaIdAndStatus(
                    biddingSagaId, VendorBidSubmission.BidStatus.REJECTED));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "FETCH_FAILED");
            error.put("message", "Failed to fetch bids: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get bid wallet balance for the current user.
     * Returns full wallet details including totalBids, spentBids, and availableBids.
     */
    @GetMapping("/wallet/balance")
    public ResponseEntity<Map<String, Object>> getWalletBalance(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String userId) {
        try {
            UUID resolvedUserId = (userId != null && !userId.isBlank())
                    ? UUID.fromString(userId)
                    : getCurrentUserId();
            BidWallet.EntityType type = entityType != null
                    ? BidWallet.EntityType.valueOf(entityType.toUpperCase())
                    : BidWallet.EntityType.VENDOR;

            var walletOpt = bidWalletService.getWallet(resolvedUserId, type);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", resolvedUserId.toString());
            response.put("entityType", type.name());

            if (walletOpt.isPresent()) {
                var wallet = walletOpt.get();
                response.put("entityId", wallet.getEntityId() != null ? wallet.getEntityId().toString() : null);
                response.put("totalBids", wallet.getTotalBids());
                response.put("spentBids", wallet.getSpentBids());
                response.put("availableBids", wallet.getAvailableBids());
            } else {
                response.put("entityId", null);
                response.put("totalBids", 0);
                response.put("spentBids", 0);
                response.put("availableBids", 0);
            }

            response.put("bidUnitPriceEur", BidWalletService.BID_UNIT_PRICE_EUR.toString());
            response.put("bidPriceDescription",
                "Each bid costs " + BidWalletService.BID_UNIT_PRICE_EUR + " EUR");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "FETCH_FAILED");
            error.put("message", "Failed to fetch wallet balance: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get bid transaction history for the current user.
     * Returns paginated transactions from the bid wallet.
     */
    @GetMapping("/wallet/transactions")
    public ResponseEntity<Map<String, Object>> getWalletTransactions(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String transactionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            UUID userId = getCurrentUserId();
            BidWallet.EntityType type = entityType != null
                    ? BidWallet.EntityType.valueOf(entityType.toUpperCase())
                    : BidWallet.EntityType.VENDOR;

            List<BidTransaction> transactions = bidWalletService.getTransactions(
                    userId, type, transactionType, page, size);

            List<Map<String, Object>> transactionList = transactions.stream().map(t -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", t.getId().toString());
                map.put("transactionType", t.getTransactionType().name());
                map.put("amount", t.getAmount());
                map.put("unitPriceEur", t.getUnitPrice() != null ? t.getUnitPrice().toString() : null);
                map.put("totalPriceEur", t.getTotalPrice() != null ? t.getTotalPrice().toString() : null);
                map.put("balanceAfter", t.getBalanceAfter());
                map.put("referenceType", t.getReferenceType());
                map.put("referenceId", t.getReferenceId());
                map.put("description", t.getDescription());
                map.put("createdAt", t.getCreatedAt().toString());
                return map;
            }).toList();

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId.toString());
            response.put("entityType", type.name());
            response.put("transactions", transactionList);
            response.put("page", page);
            response.put("size", size);
            response.put("totalTransactions", transactionList.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "FETCH_FAILED");
            error.put("message", "Failed to fetch transactions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Allocate (purchase) bids to a user's wallet.
     * This is an admin-only operation.
     * 
     * @param request Body containing userId, entityType, and amount
     * @return Updated wallet details
     */
    @PostMapping("/wallet/allocate")
    public ResponseEntity<Map<String, Object>> allocateBids(@RequestBody Map<String, Object> request) {
        try {
            // Parse userId
            String userIdStr = (String) request.get("userId");
            if (userIdStr == null || userIdStr.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "VALIDATION_ERROR");
                error.put("message", "userId is required");
                return ResponseEntity.badRequest().body(error);
            }
            UUID userId = UUID.fromString(userIdStr);

            // Parse entityType
            String entityTypeStr = (String) request.get("entityType");
            if (entityTypeStr == null || entityTypeStr.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "VALIDATION_ERROR");
                error.put("message", "entityType is required (PROJECT_OWNER or VENDOR)");
                return ResponseEntity.badRequest().body(error);
            }
            BidWallet.EntityType type = BidWallet.EntityType.valueOf(entityTypeStr.toUpperCase());

            // Parse amount
            Long amount = ((Number) request.get("amount")).longValue();
            if (amount <= 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "VALIDATION_ERROR");
                error.put("message", "Amount must be positive");
                return ResponseEntity.badRequest().body(error);
            }

            UUID tenantId = getCurrentTenantId();

            // For allocation, we use userId as entityId initially
            // In production, you'd resolve the proper entity ID
            var wallet = bidWalletService.allocateBids(
                    userId, type, userId, tenantId, amount);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            
            Map<String, Object> walletData = new HashMap<>();
            walletData.put("id", wallet.getId().toString());
            walletData.put("userId", wallet.getUserId().toString());
            walletData.put("entityType", wallet.getEntityType().name());
            walletData.put("totalBids", wallet.getTotalBids());
            walletData.put("spentBids", wallet.getSpentBids());
            walletData.put("availableBids", wallet.getAvailableBids());
            
            response.put("wallet", walletData);
            response.put("allocatedAmount", amount);
            response.put("costEur", BidWalletService.BID_UNIT_PRICE_EUR.multiply(BigDecimal.valueOf(amount)));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "VALIDATION_ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "ALLOCATION_FAILED");
            error.put("message", "Failed to allocate bids: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get wallet details by user ID (for admin to view any user's wallet).
     */
    @GetMapping("/wallet/user")
    public ResponseEntity<Map<String, Object>> getUserWallet(
            @RequestParam UUID userId,
            @RequestParam(required = false) String entityType) {
        try {
            BidWallet.EntityType type = entityType != null
                    ? BidWallet.EntityType.valueOf(entityType.toUpperCase())
                    : BidWallet.EntityType.VENDOR;

            var walletOpt = bidWalletService.getWallet(userId, type);

            if (walletOpt.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "WALLET_NOT_FOUND");
                error.put("message", "No wallet found for user: " + userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            var wallet = walletOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("wallet", Map.of(
                "id", wallet.getId().toString(),
                "userId", wallet.getUserId().toString(),
                "entityType", wallet.getEntityType().name(),
                "totalBids", wallet.getTotalBids(),
                "spentBids", wallet.getSpentBids(),
                "availableBids", wallet.getAvailableBids(),
                "createdAt", wallet.getCreatedAt(),
                "updatedAt", wallet.getUpdatedAt()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "FETCH_FAILED");
            error.put("message", "Failed to fetch wallet: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ── Channel Subscription Endpoints ──

    /**
     * Subscribe the current user to a bidding channel.
     * Subscribed users receive push notifications when new tenders are published.
     */
    @PostMapping("/channels/{channelCode}/subscription")
    public ResponseEntity<Map<String, Object>> subscribeToChannel(@PathVariable String channelCode) {
        try {
            UUID tenantId = getCurrentTenantId();
            UUID userId = getCurrentUserId();
            var sub = channelSubscriptionService.subscribe(tenantId, userId, channelCode);
            Map<String, Object> response = new HashMap<>();
            response.put("subscribed", true);
            response.put("channelCode", channelCode);
            response.put("userId", userId.toString());
            response.put("subscribedAt", sub.getSubscribedAt().toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "SUBSCRIPTION_FAILED");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Unsubscribe the current user from a bidding channel.
     */
    @DeleteMapping("/channels/{channelCode}/subscription")
    public ResponseEntity<Map<String, Object>> unsubscribeFromChannel(@PathVariable String channelCode) {
        try {
            UUID tenantId = getCurrentTenantId();
            UUID userId = getCurrentUserId();
            channelSubscriptionService.unsubscribe(tenantId, userId, channelCode);
            Map<String, Object> response = new HashMap<>();
            response.put("subscribed", false);
            response.put("channelCode", channelCode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "UNSUBSCRIBE_FAILED");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Check whether the current user is subscribed to a channel.
     */
    @GetMapping("/channels/{channelCode}/subscription")
    public ResponseEntity<Map<String, Object>> getChannelSubscription(@PathVariable String channelCode) {
        try {
            UUID tenantId = getCurrentTenantId();
            UUID userId = getCurrentUserId();
            boolean subscribed = channelSubscriptionService.isSubscribed(tenantId, userId, channelCode);
            Map<String, Object> response = new HashMap<>();
            response.put("subscribed", subscribed);
            response.put("channelCode", channelCode);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "FETCH_FAILED");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ── Open Tender Channel Listing ──

    /**
     * List all active open tenders visible in the channel.
     * Visible to all vendors regardless of subscription status.
     */
    @GetMapping("/open-tenders")
    public ResponseEntity<Map<String, Object>> getOpenTenders() {
        try {
            UUID tenantId = getCurrentTenantId();
            List<Map<String, Object>> tenders = new ArrayList<>();

            List<io.cerp.workflow.repository.SagaEntity> sagas =
                    sagaRepository.findByTenantIdAndSagaTypeAndStatus(
                            tenantId.toString(),
                            "BIDDING_WORKFLOW",
                            List.of("RUNNING", "PAUSED"));

            for (io.cerp.workflow.repository.SagaEntity saga : sagas) {
                try {
                    if (saga.getDataJson() == null) continue;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = objectMapper.readValue(saga.getDataJson(), Map.class);
                    if (!"OPEN_TENDER".equals(data.get("biddingMode"))) continue;

                    String status = (String) data.get("status");
                    if (!"OPEN_FOR_BIDS".equals(status) && !"COLLECTING".equals(status)) continue;

                    Map<String, Object> tender = new HashMap<>();
                    tender.put("sagaId", saga.getId());
                    tender.put("tenderId", data.get("tenderId"));
                    tender.put("projectId", data.get("projectId"));
                    tender.put("bidDeadline", data.get("bidDeadline"));
                    tender.put("status", status);
                    tender.put("description", data.get("description"));
                    tender.put("sagaName", data.get("sagaName"));
                    tender.put("createdAt", saga.getCreatedAt() != null ? saga.getCreatedAt().toString() : null);
                    tenders.add(tender);
                } catch (Exception e) {
                    log.debug("Could not parse saga data for sagaId={}: {}", saga.getId(), e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("tenders", tenders);
            response.put("total", tenders.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "FETCH_FAILED");
            error.put("message", "Failed to fetch open tenders: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ── DTOs ──

    /**
     * Request body for submitting a vendor bid.
     */
    public record SubmitBidRequest(
            String tenderId,
            UUID biddingSagaId,
            UUID vendorId,
            BigDecimal price,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            String timeline,
            String notes,
            String paymentMethod,
            String biddingType) {
    }

    /**
     * Request body for allocating bids to a wallet.
     */
    public record AllocateBidsRequest(
            UUID userId,
            String entityType,
            Long amount) {
    }

    // ── Helper Methods ──

    private UUID getCurrentTenantId() {
        return TenantContext.getCurrentTenantId();
    }

    private UUID getCurrentUserId() {
        // In a real implementation, this would extract userId from the security context
        UUID userId = TenantContext.getCurrentTenantId();
        return userId != null ? userId : UUID.randomUUID();
    }
}