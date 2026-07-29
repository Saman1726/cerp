package io.cerp.workflow.state;

import io.cerp.workflow.state.StateMachineDefinition.Transition;
import java.util.Map;
import java.util.function.Function;

/**
 * Predefined state machine definitions for workflow aggregates.
 *
 * <p>
 * Contains the state machine definitions for all core workflow aggregates used in the Procurement → Delivery → Financials workflow and related business processes.
 *
 * <p>
 * Each state machine defines:
 * <ul>
 * <li>Valid states for the aggregate</li>
 * <li>Allowed transitions between states</li>
 * <li>Optional guard conditions for conditional transitions</li>
 * </ul>
 */
public final class PredefinedStateMachines {

    // Private constructor to prevent instantiation
    private PredefinedStateMachines() {
    }

    // --- Aggregate type constants ---
    public static final String AGGREGATE_PROCUREMENT_REQUEST = "ProcurementRequest";
    public static final String AGGREGATE_PURCHASE_ORDER = "PurchaseOrder";
    public static final String AGGREGATE_DELIVERY = "Delivery";
    public static final String AGGREGATE_TENDER = "Tender";
    public static final String AGGREGATE_PROGRESS_CLAIM = "ProgressClaim";

    // --- State constants ---
    public static final String STATE_DRAFT = "Draft";
    public static final String STATE_ISSUED = "Issued";
    public static final String STATE_PENDING_APPROVAL = "PendingApproval";
    public static final String STATE_APPROVED = "Approved";
    public static final String STATE_REJECTED = "Rejected";
    public static final String STATE_TENDERED = "Tendered";
    public static final String STATE_PO_ISSUED = "PO_Issued";
    public static final String STATE_PARTIALLY_DELIVERED = "Partially_Delivered";
    public static final String STATE_FULLY_DELIVERED = "Fully_Delivered";
    public static final String STATE_CLOSED = "Closed";
    public static final String STATE_CANCELLED = "Cancelled";
    public static final String STATE_IN_FULFILLMENT = "In_Fulfillment";
    public static final String STATE_PARTIALLY_RECEIVED = "Partially_Received";
    public static final String STATE_FULLY_RECEIVED = "Fully_Received";
    public static final String STATE_SCHEDULED = "Scheduled";
    public static final String STATE_IN_TRANSIT = "In_Transit";
    public static final String STATE_ARRIVED = "Arrived";
    public static final String STATE_BEING_UNLOADED = "Being_Unloaded";
    public static final String STATE_DELIVERY_CONFIRMED = "Confirmed";
    public static final String STATE_DISCREPANCY_RAISED = "Discrepancy_Raised";
    public static final String STATE_DISCREPANCY_RESOLVED = "Resolved";
    public static final String STATE_OPEN = "Open";
    public static final String STATE_UNDER_EVALUATION = "Under_Evaluation";
    public static final String STATE_AWARDED = "Awarded";
    public static final String STATE_DRAFT_PROGRESS = "Draft";
    public static final String STATE_SUBMITTED = "Submitted";
    public static final String STATE_UNDER_REVIEW = "Under_Review";
    public static final String STATE_VERIFIED = "Verified";
    public static final String STATE_INVOICE_ISSUED = "Invoice_Issued";
    public static final String STATE_PAID = "Paid";

    // --- Transition constants ---
    public static final String TRANSITION_APPROVE_DRAFT = "ApproveDraft";
    public static final String TRANSITION_UNDER_EVALUATION = "UnderEvaluation";
    public static final String TRANSITION_UNDER_REVIEW = "UnderReview";
    public static final String TRANSITION_REJECT_APPROVAL = "ApprovalRejected";
    public static final String TRANSITION_START_TENDER = "StartTender";
    public static final String TRANSITION_AWARD_AND_ISSUE_PO = "AwardAndIssuePO";
    public static final String TRANSITION_RECEIVE_PARTIAL_DELIVERY = "ReceivePartialDelivery";
    public static final String TRANSITION_RECEIVE_REMAINING = "ReceiveRemaining";
    public static final String TRANSITION_CLOSE = "Close";
    public static final String TRANSITION_CANCEL = "Cancel";
    public static final String TRANSITION_RECEIVE_PARTIAL = "ReceivePartial";
    public static final String TRANSITION_RECEIVE_FULL = "ReceiveFull";
    public static final String TRANSITION_SCHEDULE_DELIVERY = "ScheduleDelivery";
    public static final String TRANSITION_START_TRANSIT = "StartTransit";
    public static final String TRANSITION_DELIVERY_ARRIVED = "DeliveryArrived";
    public static final String TRANSITION_START_UNLOADING = "StartUnloading";
    public static final String TRANSITION_CONFIRM_DELIVERY = "ConfirmDelivery";
    public static final String TRANSITION_RAISE_DISCREPANCY = "RaiseDiscrepancy";
    public static final String TRANSITION_RESOLVE_DISCREPANCY = "ResolveDiscrepancy";
    public static final String TRANSITION_SUBMIT_FOR_REVIEW = "SubmitForReview";
    public static final String TRANSITION_VERIFY_PROGRESS = "VerifyProgress";
    public static final String TRANSITION_ISSUE_INVOICE = "IssueInvoice";
    public static final String TRANSITION_APPROVE_PAYMENT = "ApprovePayment";
    public static final String TRANSITION_REJECT_CLAIM = "RejectClaim";

    /**
     * Get the state machine definition for ProcurementRequest.
     *
     * <p>
     * States: Draft → PendingApproval → Approved → Tendered → PO_Issued → Partially_Delivered → Fully_Delivered → Closed ↑ ↓ ↓ Rejected Cancelled Cancelled
     */
    public static StateMachineDefinition<String, String> procurementRequestMachine() {
        return StateMachineDefinition.<String, String> builder()
            .withInitialState(STATE_DRAFT)
            .withState(STATE_DRAFT)
            .withState(STATE_PENDING_APPROVAL)
            .withState(STATE_APPROVED)
            .withState(STATE_REJECTED)
            .withState(STATE_TENDERED)
            .withState(STATE_PO_ISSUED)
            .withState(STATE_PARTIALLY_DELIVERED)
            .withState(STATE_FULLY_DELIVERED)
            .withState(STATE_CLOSED)
            .withState(STATE_CANCELLED)
            .withTransition(STATE_DRAFT, TRANSITION_APPROVE_DRAFT, STATE_PENDING_APPROVAL)
            .withTransition(STATE_PENDING_APPROVAL, TRANSITION_APPROVE_DRAFT, STATE_APPROVED)
            .withTransition(STATE_PENDING_APPROVAL, TRANSITION_REJECT_APPROVAL, STATE_DRAFT)
            .withTransition(STATE_APPROVED, TRANSITION_START_TENDER, STATE_TENDERED)
            .withTransition(STATE_TENDERED, TRANSITION_AWARD_AND_ISSUE_PO, STATE_PO_ISSUED)
            .withTransition(STATE_PO_ISSUED, TRANSITION_RECEIVE_PARTIAL_DELIVERY, STATE_PARTIALLY_DELIVERED)
            .withTransition(STATE_PARTIALLY_DELIVERED, TRANSITION_RECEIVE_REMAINING, STATE_FULLY_DELIVERED)
            .withTransition(STATE_FULLY_DELIVERED, TRANSITION_CLOSE, STATE_CLOSED)
            .withTransition(STATE_PO_ISSUED, TRANSITION_CANCEL, STATE_CANCELLED)
            .withTransition(STATE_TENDERED, TRANSITION_CANCEL, STATE_CANCELLED)
            .withTransition(STATE_PARTIALLY_DELIVERED, TRANSITION_CANCEL, STATE_CANCELLED)
            .build();
    }

    /**
     * Get the state machine definition for PurchaseOrder.
     *
     * <p>
     * States: Draft → Approved → Issued → In_Fulfillment → Partially_Received → Fully_Received → Closed ↓ Cancelled
     */
    public static StateMachineDefinition<String, String> purchaseOrderMachine() {
        return StateMachineDefinition.<String, String> builder()
            .withInitialState(STATE_DRAFT)
            .withState(STATE_DRAFT)
            .withState(STATE_APPROVED)
            .withState(STATE_ISSUED)
            .withState(STATE_IN_FULFILLMENT)
            .withState(STATE_PARTIALLY_RECEIVED)
            .withState(STATE_FULLY_RECEIVED)
            .withState(STATE_CLOSED)
            .withState(STATE_CANCELLED)
            .withTransition(STATE_DRAFT, TRANSITION_APPROVE_DRAFT, STATE_APPROVED)
            .withTransition(STATE_APPROVED, TRANSITION_AWARD_AND_ISSUE_PO, STATE_ISSUED)
            .withTransition(STATE_ISSUED, TRANSITION_RECEIVE_PARTIAL, STATE_IN_FULFILLMENT)
            .withTransition(STATE_IN_FULFILLMENT, TRANSITION_RECEIVE_PARTIAL, STATE_PARTIALLY_RECEIVED)
            .withTransition(STATE_PARTIALLY_RECEIVED, TRANSITION_RECEIVE_FULL, STATE_FULLY_RECEIVED)
            .withTransition(STATE_FULLY_RECEIVED, TRANSITION_CLOSE, STATE_CLOSED)
            .withTransition(STATE_ISSUED, TRANSITION_CANCEL, STATE_CANCELLED)
            .withTransition(STATE_IN_FULFILLMENT, TRANSITION_CANCEL, STATE_CANCELLED)
            .build();
    }

    /**
     * Get the state machine definition for Delivery.
     *
     * <p>
     * States: Scheduled → In_Transit → Arrived → Being_Unloaded → Confirmed → Closed ↓ Discrepancy_Raised → Resolved → Closed
     */
    public static StateMachineDefinition<String, String> deliveryMachine() {
        Function<Map<String, Object>, Boolean> discrepancyGuard = ctx -> {
            Object hasDiscrepancy = ctx.get("hasDiscrepancy");
            return hasDiscrepancy instanceof Boolean b && b;
        };

        return StateMachineDefinition.<String, String> builder()
            .withInitialState(STATE_SCHEDULED)
            .withState(STATE_SCHEDULED)
            .withState(STATE_IN_TRANSIT)
            .withState(STATE_ARRIVED)
            .withState(STATE_BEING_UNLOADED)
            .withState(STATE_DELIVERY_CONFIRMED)
            .withState(STATE_DISCREPANCY_RAISED)
            .withState(STATE_DISCREPANCY_RESOLVED)
            .withState(STATE_CLOSED)
            .withTransition(STATE_SCHEDULED, TRANSITION_START_TRANSIT, STATE_IN_TRANSIT)
            .withTransition(STATE_IN_TRANSIT, TRANSITION_DELIVERY_ARRIVED, STATE_ARRIVED)
            .withTransition(STATE_ARRIVED, TRANSITION_START_UNLOADING, STATE_BEING_UNLOADED)
            .withTransition(STATE_BEING_UNLOADED, TRANSITION_CONFIRM_DELIVERY, STATE_DELIVERY_CONFIRMED)
            .withTransition(STATE_BEING_UNLOADED, TRANSITION_RAISE_DISCREPANCY, STATE_DISCREPANCY_RAISED,
                discrepancyGuard)
            .withTransition(STATE_DISCREPANCY_RAISED, TRANSITION_RESOLVE_DISCREPANCY, STATE_DISCREPANCY_RESOLVED)
            .withTransition(STATE_DISCREPANCY_RESOLVED, TRANSITION_CONFIRM_DELIVERY, STATE_DELIVERY_CONFIRMED)
            .withTransition(STATE_DELIVERY_CONFIRMED, TRANSITION_CLOSE, STATE_CLOSED)
            .build();
    }

    /**
     * Get the state machine definition for Tender.
     *
     * <p>
     * States: Draft → Open → Under_Evaluation → Awarded → Closed ↓ Cancelled
     */
    public static StateMachineDefinition<String, String> tenderMachine() {
        return StateMachineDefinition.<String, String> builder()
            .withInitialState(STATE_DRAFT)
            .withState(STATE_DRAFT)
            .withState(STATE_OPEN)
            .withState(STATE_UNDER_EVALUATION)
            .withState(STATE_AWARDED)
            .withState(STATE_CLOSED)
            .withState(STATE_CANCELLED)
            .withTransition(STATE_DRAFT, TRANSITION_START_TENDER, STATE_OPEN)
            .withTransition(STATE_OPEN, TRANSITION_UNDER_EVALUATION, STATE_UNDER_EVALUATION)
            .withTransition(STATE_UNDER_EVALUATION, TRANSITION_AWARD_AND_ISSUE_PO, STATE_AWARDED)
            .withTransition(STATE_AWARDED, TRANSITION_CLOSE, STATE_CLOSED)
            .withTransition(STATE_OPEN, TRANSITION_CANCEL, STATE_CANCELLED)
            .withTransition(STATE_UNDER_EVALUATION, TRANSITION_CANCEL, STATE_CANCELLED)
            .build();
    }

    /**
     * Get the state machine definition for ProgressClaim.
     *
     * <p>
     * States: Draft → Submitted → Under_Review → Verified → Invoice_Issued → Approved → Paid ↓ Rejected
     */
    public static StateMachineDefinition<String, String> progressClaimMachine() {
        return StateMachineDefinition.<String, String> builder()
            .withInitialState(STATE_DRAFT_PROGRESS)
            .withState(STATE_DRAFT_PROGRESS)
            .withState(STATE_SUBMITTED)
            .withState(STATE_UNDER_REVIEW)
            .withState(STATE_VERIFIED)
            .withState(STATE_INVOICE_ISSUED)
            .withState(STATE_APPROVED)
            .withState(STATE_PAID)
            .withState(STATE_REJECTED)
            .withTransition(STATE_DRAFT_PROGRESS, TRANSITION_SUBMIT_FOR_REVIEW, STATE_SUBMITTED)
            .withTransition(STATE_SUBMITTED, TRANSITION_UNDER_REVIEW, STATE_UNDER_REVIEW)
            .withTransition(STATE_UNDER_REVIEW, TRANSITION_VERIFY_PROGRESS, STATE_VERIFIED)
            .withTransition(STATE_UNDER_REVIEW, TRANSITION_REJECT_CLAIM, STATE_REJECTED)
            .withTransition(STATE_VERIFIED, TRANSITION_ISSUE_INVOICE, STATE_INVOICE_ISSUED)
            .withTransition(STATE_INVOICE_ISSUED, TRANSITION_APPROVE_PAYMENT, STATE_APPROVED)
            .withTransition(STATE_APPROVED, TRANSITION_CLOSE, STATE_PAID)
            .build();
    }

    /**
     * Get all predefined state machines.
     */
    public static Map<String, StateMachineDefinition<String, String>> allMachines() {
        return Map.of(
            AGGREGATE_PROCUREMENT_REQUEST, procurementRequestMachine(),
            AGGREGATE_PURCHASE_ORDER, purchaseOrderMachine(),
            AGGREGATE_DELIVERY, deliveryMachine(),
            AGGREGATE_TENDER, tenderMachine(),
            AGGREGATE_PROGRESS_CLAIM, progressClaimMachine());
    }

    /**
     * Get a state machine by aggregate type.
     *
     * @return null if no state machine exists for the given aggregate type
     */
    public static StateMachineDefinition<String, String> getMachine(String aggregateType) {
        return allMachines().get(aggregateType);
    }
}
