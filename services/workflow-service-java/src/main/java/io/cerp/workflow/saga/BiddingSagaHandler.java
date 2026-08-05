package io.cerp.workflow.saga;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.cerp.workflow.domain.bidding.BidWallet;
import io.cerp.workflow.event.WorkflowEvent;
import io.cerp.workflow.config.KafkaTopicConfig;
import io.cerp.workflow.security.IdentityPermissionClient;
import io.cerp.workflow.service.EventPublisher;
import io.cerp.workflow.service.NotificationServiceClient;
import io.cerp.workflow.service.VendorServiceClient;
import io.cerp.workflow.service.bidding.BidWalletService;
import io.cerp.workflow.service.bidding.VendorBidSubmissionService;

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

    /** Resume a paused/stalled bidding saga. */
    public static final String RESUME = "Resume";

    private final EventPublisher eventPublisher;
    private final VendorBidSubmissionService bidSubmissionService;
    private final BidWalletService bidWalletService;
    private final NotificationServiceClient notificationServiceClient;
    private final VendorServiceClient vendorServiceClient;
    private final IdentityPermissionClient identityPermissionClient;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    public BiddingSagaHandler(final EventPublisher eventPublisher,
            VendorBidSubmissionService bidSubmissionService,
            BidWalletService bidWalletService,
            NotificationServiceClient notificationServiceClient,
            VendorServiceClient vendorServiceClient,
            IdentityPermissionClient identityPermissionClient,
            org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate) {
        this.eventPublisher = eventPublisher;
        this.bidSubmissionService = bidSubmissionService;
        this.bidWalletService = bidWalletService;
        this.notificationServiceClient = notificationServiceClient;
        this.vendorServiceClient = vendorServiceClient;
        this.identityPermissionClient = identityPermissionClient;
        this.kafkaTemplate = kafkaTemplate;
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
            case RESUME:
            case "RESUME":
                return executeResume(context, payload);
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

        // Extract projectId and set as aggregate ID
        Object projectIdObj = payload.get("projectId");
        UUID projectId = null;
        if (projectIdObj instanceof String s) {
            try {
                projectId = UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                // ignore
            }
        } else if (projectIdObj instanceof UUID u) {
            projectId = u;
        }

        final String tenderId = UUID.randomUUID().toString();
        final Map<String, Object> tenderData = new HashMap<>();
        tenderData.put("tenderId", tenderId);
        tenderData.put("projectId", projectIdObj);
        tenderData.put("status", "OPEN");
        tenderData.put("createdAt", Instant.now().toString());

        context.getData().putAll(tenderData);
        context.getData().put("tenderId", tenderId);

        // Set the aggregate ID to the project ID
        if (projectId != null) {
            context.setAggregateId(projectId);
            context.setProjectId(projectId);
        }

        // Emit TenderCreated event
        publishEvent(sagaId, tenantId, "TenderCreated", tenderData);

        log.info("Tender created and opened: tenderId={}, projectId={}", tenderId, payload.get("projectId"));
        return context;
    }

    private SagaContext executeInviteVendors(final SagaContext context, final Map<String, Object> payload)
            throws SagaException {
        final UUID sagaId = context.getSagaId();
        final String tenantId = context.getTenantId();
        String tenderId = (String) context.getData().get("tenderId");
        if (tenderId == null) {
            tenderId = UUID.randomUUID().toString();
            context.getData().put("tenderId", tenderId);
            log.info("Auto-generated tenderId for saga {}: {}", sagaId, tenderId);
        }

        @SuppressWarnings("unchecked")
        final Map<String, Object> inviteConfig = (payload != null && payload.containsKey("inviteConfig"))
                ? (Map<String, Object>) payload.get("inviteConfig")
                : null;

        // Try to get vendorIds from payload first, then fall back to saga context (persisted data)
        final List<String> invitedVendorIds;
        if (payload != null && payload.containsKey("vendorIds")) {
            final Object v = payload.get("vendorIds");
            invitedVendorIds = (v instanceof List<?> list) ? new ArrayList<>((List<String>) list) : List.of();
        } else {
            // Fall back to persisted vendor IDs stored in saga context
            Object vendorIdsFromContext = context.getData().get("invitedVendorIds");
            if (vendorIdsFromContext == null) {
                vendorIdsFromContext = context.getData().get("vendorIds");
            }
            invitedVendorIds = (vendorIdsFromContext instanceof List<?> list)
                    ? new ArrayList<>((List<String>) list)
                    : List.of();
        }

        final int invitedCount = invitedVendorIds.size();

        // Validate that at least one vendor is provided
        if (invitedVendorIds.isEmpty()) {
            throw new SagaException("NO_VENDERS_TO_INVITE",
                "At least one vendor must be invited. No vendors provided in this request.",
                Map.of("vendorIds", List.of()));
        }

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

        // Emit VendorsInvited event (for outbox/event sourcing)
        publishEvent(sagaId, tenantId, "VendorsInvited", inviteData);

        // Publish Kafka event for vendor invitation notifications
        publishVendorInvitedNotificationEvent(sagaId, tenantId, tenderId, invitedVendorIds, inviteConfig, context.getData());

        log.info("Vendors invited for tender: tenderId={}, invitedCount={}, expectedBids={}",
            tenderId, invitedCount, expectedBidsVal);
        return context;
    }

    /**
     * Publish a Kafka event to trigger vendor invitation notifications.
     * This event is consumed by VendorsInvitedEventConsumer which sends notifications
     * to each invited vendor and to the project owner.
     */
    private void publishVendorInvitedNotificationEvent(
            final UUID sagaId,
            final String tenantId,
            final String tenderId,
            final List<String> vendorIds,
            final Map<String, Object> inviteConfig,
            final Map<String, Object> contextData) {

        try {
            final Map<String, Object> eventData = new HashMap<>();
            eventData.put("tenderId", tenderId);
            eventData.put("sagaId", sagaId.toString());
            eventData.put("tenantId", tenantId);
            eventData.put("bidderUserId", resolveToUuid(contextData.get("userId")));
            eventData.put("projectId", contextData.get("projectId"));
            eventData.put("vendorIds", vendorIds);
            eventData.put("bidDeadline", inviteConfig != null ? inviteConfig.get("bidDeadline") : null);
            eventData.put("inviteConfig", inviteConfig != null ? inviteConfig : new HashMap<>());

            kafkaTemplate.send(
                    KafkaTopicConfig.TOPIC_VENDORS_INVITED,
                    tenderId,
                    eventData);

            log.info("Published vendor invitation notification event: tenderId={}, vendorCount={}",
                    tenderId, vendorIds.size());
        } catch (Exception e) {
            log.error("Failed to publish vendor invitation notification event: tenderId={}", tenderId, e);
            // Don't fail the saga if notification event publishing fails
        }
    }

    private SagaContext executeCollectBids(final SagaContext context, final Map<String, Object> payload)
            throws SagaException {
        final UUID sagaId = context.getSagaId();
        final String tenantId = context.getTenantId();
        final String tenderId = (String) context.getData().get("tenderId");

        // Deduct 1 bid from project owner's wallet when bidding starts
        final String projectOwnerId = context.getUserId();
        if (projectOwnerId != null) {
            try {
                UUID projectOwnerUUID = UUID.fromString(projectOwnerId);
                bidWalletService.spendForBiddingCreation(
                        projectOwnerUUID,
                        BidWallet.EntityType.PROJECT_OWNER,
                        projectOwnerUUID,
                        UUID.fromString(tenantId),
                        sagaId,
                        tenderId);
                log.info("Deducted 1 bid from project owner: userId={}, tenderId={}", projectOwnerId, tenderId);
            } catch (BidWalletService.InsufficientBidsException e) {
                final Map<String, Object> errorDetails = new HashMap<>();
                errorDetails.put("tenderId", tenderId);
                errorDetails.put("projectOwnerId", projectOwnerId);
                errorDetails.put("availableBids", e.getAvailable());
                errorDetails.put("requiredBids", e.getRequired());
                throw new SagaException("INSUFFICIENT_BIDS", e.getMessage(), errorDetails);
            } catch (IllegalArgumentException e) {
                log.warn("Could not deduct bids: {}", e.getMessage());
                // Continue anyway if user ID parsing fails
            }
        }

        // Extract bidding saga ID from context
        Object sagaIdObj = context.getData().get("biddingSagaId");
        UUID biddingSagaId = null;
        if (sagaIdObj instanceof String s) {
            try {
                biddingSagaId = UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                biddingSagaId = sagaId;
            }
        } else if (sagaIdObj instanceof UUID u) {
            biddingSagaId = u;
        }

        // Use the context sagaId if biddingSagaId is not set
        if (biddingSagaId == null) {
            biddingSagaId = sagaId;
        }

        // Query database for stored bids instead of expecting them in payload
        final List<Map<String, Object>> bids = bidSubmissionService
                .getBidsBySagaId(biddingSagaId)
                .stream()
                .map(VendorBidSubmissionService::toMap)
                .toList();

        if (bids.isEmpty()) {
            final Object vendorIdsObj = context.getData().get("invitedVendorIds");
            int invitedCountVal = 0;
            if (vendorIdsObj instanceof List) {
                invitedCountVal = ((List<?>) vendorIdsObj).size();
            }
            final Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("tenderId", tenderId);
            errorDetails.put("invitedCount", invitedCountVal);
            errorDetails.put("biddingSagaId", biddingSagaId.toString());
            throw new SagaException("NO_BIDS_RECEIVED", "No bids received for tender: " + tenderId
                    + " (sagaId: " + biddingSagaId + "). Vendors have not submitted bids yet.", errorDetails);
        }

        final Map<String, Object> collectData = new HashMap<>();
        collectData.put("tenderId", tenderId);
        collectData.put("biddingSagaId", biddingSagaId.toString());
        collectData.put("bidsReceived", bids.size());
        collectData.put("bids", bids);
        collectData.put("status", "UNDER_EVALUATION");

        context.getData().putAll(collectData);

        // Emit BidsCollected event
        publishEvent(sagaId, tenantId, "BidsCollected", collectData);

        log.info("Bids collected for tender: tenderId={}, biddingSagaId={}, bidsReceived={}", 
                tenderId, biddingSagaId, bids.size());
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
            case RESUME -> null; // Resume is handled by executeResume, not getNextStepCode
            default -> null;
        };
    }

    /**
     * Determine the next step based on the current step stored in saga state.
     */
    private String getNextStepFromCurrentState(final SagaContext context) {
        final Object currentStepObj = context.getData().get("currentStep");
        if (currentStepObj instanceof String currentStep) {
            return switch (currentStep) {
                case CREATE_TENDER -> INVITE_VENDORS;
                case INVITE_VENDORS -> null; // paused; caller must supply stepCode in Resume payload
                case COLLECT_BIDS -> EVALUATE_BIDS;
                case EVALUATE_BIDS -> AWARD_TENDER;
                default -> null;
            };
        }
        // Default: if no current step, start with inviting vendors
        // (CreateAndOpen is assumed to have been executed during saga creation)
        return INVITE_VENDORS;
    }

    /**
     * Resume a paused/stalled bidding saga by advancing to the next step.
     */
    private SagaContext executeResume(final SagaContext context, final Map<String, Object> payload)
            throws SagaException {
        final UUID sagaId = context.getSagaId();
        final String tenantId = context.getTenantId();
        final String tenderId = (String) context.getData().get("tenderId");

        // Extract optional target step from payload
        String targetStep = null;
        if (payload != null) {
            final Object stepCodeObj = payload.get("stepCode");
            if (stepCodeObj instanceof String s) {
                targetStep = s;
            }
        }

        // Get current step from context
        final Object currentStepObj = context.getData().get("currentStep");
        final String currentStep = currentStepObj instanceof String s ? s : null;

        // Determine next step
        final String nextStep;
        if (targetStep != null) {
            nextStep = targetStep;
        } else {
            nextStep = getNextStepFromCurrentState(context);
        }

        log.info("Resuming bidding saga: sagaId={}, tenderId={}, currentStep={}, nextStep={}",
            sagaId, tenderId, currentStep, nextStep);

        final Map<String, Object> resumeData = new HashMap<>();
        resumeData.put("tenderId", tenderId);
        resumeData.put("previousStep", currentStep != null ? currentStep : "unknown");
        resumeData.put("nextStep", nextStep != null ? nextStep : "completed");
        resumeData.put("status", "RESUMED");

        // Publish Resumed event
        publishEvent(sagaId, tenantId, "BiddingSagaResumed", resumeData);

        if (nextStep != null) {
            context.getData().put("currentStep", nextStep);
        } else {
            // Saga is complete
            log.info("Bidding saga completed after resume: tenderId={}", tenderId);
        }

        return context;
    }

    // ── Helpers ──

    /**
     * Convert a raw userId value to UUID. If the value is already a UUID string it is parsed
     * directly. If it looks like an email address (legacy JWT sub claim) the identity service
     * is queried to resolve the corresponding account UUID. Returns null when resolution fails.
     */
    private UUID resolveToUuid(Object rawUserId) {
        if (rawUserId == null) return null;
        final String s = rawUserId.toString();
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            if (s.contains("@")) {
                final UUID resolved = identityPermissionClient.resolveUserIdByEmail(s);
                if (resolved != null) {
                    log.debug("Resolved email {} to userId {}", s, resolved);
                    return resolved;
                }
                log.warn("Could not resolve email {} to a userId — bidderUserId will be omitted", s);
            }
            return null;
        }
    }

    // ── Event Publishing ──

    private void publishEvent(final UUID sagaId, final String tenantId, final String eventType,
            final Map<String, Object> data) {
        publishEvent(sagaId, tenantId, null, eventType, data);
    }

    private void publishEvent(final UUID sagaId, final String tenantId, final UUID aggregateId,
            final String eventType, final Map<String, Object> data) {
        try {
            eventPublisher.publish(WorkflowEvent.builder()
                .eventType(eventType)
                .sagaType("marketplace_bidding")
                .sagaId(sagaId.toString())
                .aggregateId(aggregateId)
                .aggregateType("Tender")
                .tenantId(tenantId)
                .data(data)
                .build());
        } catch (final Exception e) {
            log.warn("Failed to publish event: {}", eventType, e);
        }
    }
}
