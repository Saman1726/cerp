package io.cerp.workflow.saga;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.cerp.workflow.event.WorkflowEvent;
import io.cerp.workflow.service.EventPublisher;

/**
 * Saga handler for the Marketplace Bidding workflow (Section 7.4).
 *
 * <p>
 * This handler coordinates the bidding process:
 * <ol>
 * <li>Tender is created and opened</li>
 * <li>Vendors are invited to bid</li>
 * <li>Bids are collected and evaluated</li>
 * <li>Tender is awarded and PO is issued</li>
 * </ol>
 *
 * <p>
 * State machine:
 *
 * <pre>
 * Draft → Open → Under_Evaluation → Awarded → Closed
 *                         ↓
 *                    Cancelled
 * </pre>
 *
 * @see <a href="https://microservices.io/patterns/orchestration/Saga.html">Saga Pattern</a>
 */
@Component
public class BiddingSagaHandler implements SagaHandler {

    private static final Logger log = LoggerFactory.getLogger(BiddingSagaHandler.class);

    /** Create and open a tender for bidding. */
    public static final String CREATE_TENDER = "Tender.CreateAndOpen";

    /** Invite vendors to submit bids. */
    public static final String INVITE_VENDORS = "Tender.InviteVendors";

    /** Collect bids from invited vendors. */
    public static final String COLLECT_BIDS = "Bidding.CollectBids";

    /** Evaluate received bids and select winner. */
    public static final String EVALUATE_BIDS = "Bidding.Evaluate";

    /** Award tender and issue purchase order. */
    public static final String AWARD_TENDER = "Tender.Award";

    private final EventPublisher eventPublisher;

    public BiddingSagaHandler(final EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String getStepCode() {
        return CREATE_TENDER;
    }

    @Override
    public SagaContext execute(final SagaContext context, final String command, final Map<String, Object> payload)
            throws Exception {
        log.info("Executing bidding saga step: command={}, sagaId={}", command, context.getSagaId());

        switch (command) {
            case CREATE_TENDER:
                return executeCreateTender(context, payload);
            case INVITE_VENDORS:
                return executeInviteVendors(context, payload);
            case COLLECT_BIDS:
                return executeCollectBids(context, payload);
            case EVALUATE_BIDS:
                return executeEvaluateBids(context, payload);
            case AWARD_TENDER:
                return executeAwardTender(context, payload);
            default:
                throw new SagaException("UNKNOWN_COMMAND", "Unknown command: " + command, new HashMap<>());
        }
    }

    @Override
    public SagaContext compensate(final SagaContext context, final String stepCode) throws Exception {
        log.info("Compensating bidding saga step: stepCode={}, sagaId={}", stepCode, context.getSagaId());

        switch (stepCode) {
            case AWARD_TENDER:
                return compensateAwardTender(context);
            case EVALUATE_BIDS:
                return compensateEvaluateBids(context);
            case COLLECT_BIDS:
                return compensateCollectBids(context);
            case INVITE_VENDORS:
                return compensateInviteVendors(context);
            case CREATE_TENDER:
                return compensateCreateTender(context);
            default:
                log.warn("No compensation for step: {}", stepCode);
                return context;
        }
    }

    @Override
    public String getNextStep(final SagaContext context, final String executedCommand) {
        String next = getNextStepCode(executedCommand);
        if (next != null) {
            context.getData().put("currentStep", next);
        }
        return next;
    }

    // ── Step Implementations ──

    private SagaContext executeCreateTender(final SagaContext context, final Map<String, Object> payload)
            throws SagaException {
        final UUID sagaId = context.getSagaId();
        final String tenantId = context.getTenantId();

        // Validate required fields
        if (payload == null || !payload.containsKey("projectId")) {
            throw new SagaException("VALIDATION_ERROR", "projectId is required for Tender.CreateAndOpen",
                Map.of("field", "projectId"));
        }

        final String tenderId = UUID.randomUUID().toString();
        final Map<String, Object> tenderData = new HashMap<>();
        tenderData.put("tenderId", tenderId);
        tenderData.put("projectId", payload.get("projectId"));
        tenderData.put("status", "OPEN");
        tenderData.put("createdAt", Instant.now().toString());

        context.getData().putAll(tenderData);
        context.getData().put("tenderId", tenderId);

        // Emit TenderCreated event
        publishEvent(sagaId, tenantId, "TenderCreated", tenderData);

        log.info("Tender created and opened: tenderId={}, projectId={}", tenderId, payload.get("projectId"));
        return context;
    }

    private SagaContext executeInviteVendors(final SagaContext context, final Map<String, Object> payload)
            throws SagaException {
        final UUID sagaId = context.getSagaId();
        final String tenantId = context.getTenantId();
        final String tenderId = (String) context.getData().get("tenderId");

        @SuppressWarnings("unchecked")
        final Map<String, Object> inviteConfig = (payload != null && payload.containsKey("inviteConfig"))
                ? (Map<String, Object>) payload.get("inviteConfig")
                : null;

        final List<String> invitedVendorIds = (payload != null && payload.containsKey("vendorIds"))
                ? (List<String>) payload.get("vendorIds")
                : List.of();

        final int invitedCount = invitedVendorIds.size();

        // Safely extract expectedBids with null handling
        Integer expectedBidsVal = invitedCount;
        if (inviteConfig != null && inviteConfig.containsKey("expectedBids")) {
            final Object eb = inviteConfig.get("expectedBids");
            if (eb instanceof Number) {
                expectedBidsVal = ((Number) eb).intValue();
            } else if (eb != null) {
                expectedBidsVal = Integer.parseInt(eb.toString());
            }
        }

        // Safely extract bidDeadline with null handling
        Object bidDeadlineVal = "unknown";
        if (inviteConfig != null && inviteConfig.containsKey("bidDeadline")) {
            final Object bd = inviteConfig.get("bidDeadline");
            if (bd != null) {
                bidDeadlineVal = bd;
            }
        }

        final Map<String, Object> inviteData = new HashMap<>();
        inviteData.put("tenderId", tenderId);
        inviteData.put("invitedVendorIds", invitedVendorIds);
        inviteData.put("expectedBids", expectedBidsVal);
        inviteData.put("bidDeadline", bidDeadlineVal);
        inviteData.put("status", "UNDER_EVALUATION");

        context.getData().putAll(inviteData);

        // Emit VendorsInvited event
        publishEvent(sagaId, tenantId, "VendorsInvited", inviteData);

        log.info("Vendors invited for tender: tenderId={}, invitedCount={}, expectedBids={}",
            tenderId, invitedCount, expectedBidsVal);
        return context;
    }

    private SagaContext executeCollectBids(final SagaContext context, final Map<String, Object> payload)
            throws SagaException {
        final UUID sagaId = context.getSagaId();
        final String tenantId = context.getTenantId();
        final String tenderId = (String) context.getData().get("tenderId");

        final List<Map<String, Object>> bids = (payload != null && payload.containsKey("bids"))
                ? (List<Map<String, Object>>) payload.get("bids")
                : List.of();

        if (bids.isEmpty()) {
            final Object vendorIdsObj = context.getData().get("invitedVendorIds");
            int invitedCountVal = 0;
            if (vendorIdsObj instanceof List) {
                invitedCountVal = ((List<?>) vendorIdsObj).size();
            }
            final Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("tenderId", tenderId);
            errorDetails.put("invitedCount", invitedCountVal);
            throw new SagaException("NO_BIDS_RECEIVED", "No bids received for tender: " + tenderId, errorDetails);
        }

        final Map<String, Object> collectData = new HashMap<>();
        collectData.put("tenderId", tenderId);
        collectData.put("bidsReceived", bids.size());
        collectData.put("bids", bids);
        collectData.put("status", "UNDER_EVALUATION");

        context.getData().putAll(collectData);

        // Emit BidsCollected event
        publishEvent(sagaId, tenantId, "BidsCollected", collectData);

        log.info("Bids collected for tender: tenderId={}, bidsReceived={}", tenderId, bids.size());
        return context;
    }

    private SagaContext executeEvaluateBids(final SagaContext context, final Map<String, Object> payload)
            throws SagaException {
        final UUID sagaId = context.getSagaId();
        final String tenantId = context.getTenantId();
        final String tenderId = (String) context.getData().get("tenderId");

        final Map<String, Object> evaluation = (payload != null && payload.containsKey("evaluation"))
                ? (Map<String, Object>) payload.get("evaluation")
                : null;

        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> bids = (List<Map<String, Object>>) context.getData().get("bids");

        // Select winning bid (simple: lowest price, or use evaluation criteria)
        String winningVendorId = null;
        double winningPrice = Double.MAX_VALUE;

        if (bids != null) {
            for (final Map<String, Object> bid : bids) {
                final Object priceObj = bid.get("price");
                double price;
                if (priceObj instanceof Double) {
                    price = (Double) priceObj;
                } else if (priceObj instanceof Number) {
                    price = ((Number) priceObj).doubleValue();
                } else if (priceObj != null) {
                    price = Double.parseDouble(priceObj.toString());
                } else {
                    price = Double.MAX_VALUE;
                }
                if (price < winningPrice) {
                    winningPrice = price;
                    winningVendorId = (String) bid.get("vendorId");
                }
            }
        }

        if (winningVendorId == null) {
            throw new SagaException("EVALUATION_FAILED", "Could not determine winning bid",
                Map.of("tenderId", tenderId));
        }

        final Map<String, Object> awardData = new HashMap<>();
        awardData.put("tenderId", tenderId);
        awardData.put("winningVendorId", winningVendorId);
        awardData.put("winningPrice", winningPrice);
        awardData.put("evaluation", evaluation != null ? evaluation : new HashMap<>());
        awardData.put("status", "AWARDED");

        context.getData().putAll(awardData);

        // Emit TenderEvaluated event
        publishEvent(sagaId, tenantId, "TenderEvaluated", awardData);

        log.info("Tender evaluated: tenderId={}, winningVendor={}, winningPrice={}",
            tenderId, winningVendorId, winningPrice);
        return context;
    }

    private SagaContext executeAwardTender(final SagaContext context, final Map<String, Object> payload)
            throws SagaException {
        final UUID sagaId = context.getSagaId();
        final String tenantId = context.getTenantId();
        final String tenderId = (String) context.getData().get("tenderId");
        final String winningVendorId = (String) context.getData().get("winningVendorId");
        final Double winningPrice = (Double) context.getData().get("winningPrice");

        final Map<String, Object> awardCompleteData = new HashMap<>();
        awardCompleteData.put("tenderId", tenderId);
        awardCompleteData.put("winningVendorId", winningVendorId);
        awardCompleteData.put("winningPrice", winningPrice);
        awardCompleteData.put("poIssued", true);
        awardCompleteData.put("status", "AWARDED");

        context.getData().putAll(awardCompleteData);

        // Emit TenderAwarded and POIssued events
        publishEvent(sagaId, tenantId, "TenderAwarded", awardCompleteData);

        final Map<String, Object> poData = new HashMap<>();
        poData.put("tenderId", tenderId);
        poData.put("vendorId", winningVendorId);
        poData.put("amount", winningPrice);
        publishEvent(sagaId, tenantId, "POIssued", poData);

        log.info("Tender awarded and PO issued: tenderId={}, vendorId={}, amount={}",
            tenderId, winningVendorId, winningPrice);
        return context;
    }

    // ── Compensation Methods ──

    private SagaContext compensateAwardTender(final SagaContext context) throws SagaException {
        final String tenderId = (String) context.getData().get("tenderId");
        final Map<String, Object> cancelData = new HashMap<>();
        cancelData.put("tenderId", tenderId);
        cancelData.put("reason", "saga_compensation");
        publishEvent(context.getSagaId(), context.getTenantId(), "POCancelled", cancelData);
        log.info("PO cancellation compensated: tenderId={}", tenderId);
        return context;
    }

    private SagaContext compensateEvaluateBids(final SagaContext context) throws SagaException {
        final String tenderId = (String) context.getData().get("tenderId");
        final Map<String, Object> reopenData = new HashMap<>();
        reopenData.put("tenderId", tenderId);
        reopenData.put("reason", "saga_compensation");
        publishEvent(context.getSagaId(), context.getTenantId(), "TenderReopened", reopenData);
        log.info("Tender reopened after evaluation compensation: tenderId={}", tenderId);
        return context;
    }

    private SagaContext compensateCollectBids(final SagaContext context) throws SagaException {
        final String tenderId = (String) context.getData().get("tenderId");
        final Map<String, Object> cancelData = new HashMap<>();
        cancelData.put("tenderId", tenderId);
        cancelData.put("reason", "saga_compensation");
        publishEvent(context.getSagaId(), context.getTenantId(), "BidsCollectionCancelled", cancelData);
        log.info("Bids collection cancelled: tenderId={}", tenderId);
        return context;
    }

    private SagaContext compensateInviteVendors(final SagaContext context) throws SagaException {
        final String tenderId = (String) context.getData().get("tenderId");
        @SuppressWarnings("unchecked")
        final List<String> vendorIds = (List<String>) context.getData().get("invitedVendorIds");
        final Map<String, Object> revokeData = new HashMap<>();
        revokeData.put("tenderId", tenderId);
        revokeData.put("vendorIds", vendorIds != null ? vendorIds : List.of());
        revokeData.put("reason", "saga_compensation");
        publishEvent(context.getSagaId(), context.getTenantId(), "InvitationsRevoked", revokeData);
        log.info("Invitations revoked: tenderId={}, vendorCount={}", tenderId,
            vendorIds != null ? vendorIds.size() : 0);
        return context;
    }

    private SagaContext compensateCreateTender(final SagaContext context) throws SagaException {
        final String tenderId = (String) context.getData().get("tenderId");
        final Map<String, Object> cancelData = new HashMap<>();
        cancelData.put("tenderId", tenderId);
        cancelData.put("reason", "saga_compensation");
        publishEvent(context.getSagaId(), context.getTenantId(), "TenderCancelled", cancelData);
        log.info("Tender cancelled: tenderId={}", tenderId);
        return context;
    }

    // ── Step Transition Map ──

    private String getNextStepCode(final String executedStep) {
        return switch (executedStep) {
            case CREATE_TENDER -> INVITE_VENDORS;
            case INVITE_VENDORS -> COLLECT_BIDS;
            case COLLECT_BIDS -> EVALUATE_BIDS;
            case EVALUATE_BIDS -> AWARD_TENDER;
            case AWARD_TENDER -> null; // Saga complete
            default -> null;
        };
    }

    // ── Event Publishing ──

    private void publishEvent(final UUID sagaId, final String tenantId, final String eventType,
            final Map<String, Object> data) {
        try {
            eventPublisher.publish(WorkflowEvent.builder()
                .eventType(eventType)
                .sagaType("marketplace_bidding")
                .sagaId(sagaId.toString())
                .tenantId(tenantId)
                .data(data)
                .build());
        } catch (final Exception e) {
            log.warn("Failed to publish event: {}", eventType, e);
        }
    }
}
