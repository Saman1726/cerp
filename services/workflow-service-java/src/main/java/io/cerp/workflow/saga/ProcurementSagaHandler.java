package io.cerp.workflow.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cerp.workflow.event.WorkflowEvent;
import io.cerp.workflow.repository.SagaEntity;
import io.cerp.workflow.repository.SagaRepository;
import io.cerp.workflow.service.EventPublisher;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Saga handler for the Procurement workflow.
 *
 * <p>
 * This handler manages the Procurement → Delivery → Financials saga, which includes:
 * <ol>
 * <li>Create Procurement Request</li>
 * <li>Submit for Approval</li>
 * <li>Approve/Reject</li>
 * <li>Issue Purchase Order</li>
 * <li>Receive Delivery</li>
 * <li>Confirm Delivery</li>
 * <li>Close</li>
 * </ol>
 *
 * <p>
 * Compensation flows:
 * <ul>
 * <li>If PO is cancelled after issuance → Release commitment, refund credits</li>
 * <li>If delivery fails → Cancel PO</li>
 * <li>If approval is rejected → Return to requestor</li>
 * </ul>
 */
@Component
public class ProcurementSagaHandler implements SagaHandler {

    private static final Logger log = LoggerFactory.getLogger(ProcurementSagaHandler.class);

    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final SagaRepository sagaRepository;

    @Autowired
    public ProcurementSagaHandler(EventPublisher eventPublisher, ObjectMapper objectMapper, SagaRepository sagaRepository) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.sagaRepository = sagaRepository;
    }

    // --- Step codes ---
    public static final String STEP_CREATE_REQUEST = "CreateProcurementRequest";
    public static final String STEP_SUBMIT_FOR_APPROVAL = "SubmitForApproval";
    public static final String STEP_APPROVE_REQUEST = "ApproveRequest";
    public static final String STEP_ISSUE_PO = "IssuePurchaseOrder";
    public static final String STEP_RECEIVE_DELIVERY = "ReceiveDelivery";
    public static final String STEP_CONFIRM_DELIVERY = "ConfirmDelivery";
    public static final String STEP_CLOSE = "CloseProcurement";
    public static final String STEP_RESUME = "Resume";

    @Override
    public String getStepCode() {
        return "ProcurementSaga";
    }

    @Override
    public SagaContext execute(SagaContext context, String stepCode, Map<String, Object> payload) throws Exception {
        log.info("Executing procurement saga step: {} for sagaId={}", stepCode, context.getSagaId());

        switch (stepCode) {
            case STEP_CREATE_REQUEST:
                return executeCreateRequest(context, payload);
            case STEP_SUBMIT_FOR_APPROVAL:
                return executeSubmitForApproval(context, payload);
            case STEP_APPROVE_REQUEST:
                return executeApproveRequest(context, payload);
            case STEP_ISSUE_PO:
                return executeIssuePO(context, payload);
            case STEP_RECEIVE_DELIVERY:
                return executeReceiveDelivery(context, payload);
            case STEP_CONFIRM_DELIVERY:
                return executeConfirmDelivery(context, payload);
            case STEP_CLOSE:
                return executeClose(context, payload);
            case STEP_RESUME:
            case "RESUME":
                return executeResume(context, payload);
            default:
                throw new IllegalArgumentException("Unknown step: " + stepCode);
        }
    }

    @Override
    public SagaContext compensate(SagaContext context, String stepCode) throws Exception {
        log.info("Compensating procurement saga step: {} for sagaId={}", stepCode, context.getSagaId());

        switch (stepCode) {
            case STEP_CREATE_REQUEST:
                return compensateCreateRequest(context);
            case STEP_SUBMIT_FOR_APPROVAL:
                return compensateSubmitForApproval(context);
            case STEP_APPROVE_REQUEST:
                return compensateApproveRequest(context);
            case STEP_ISSUE_PO:
                return compensateIssuePO(context);
            case STEP_RECEIVE_DELIVERY:
                return compensateReceiveDelivery(context);
            case STEP_CONFIRM_DELIVERY:
                return compensateConfirmDelivery(context);
            default:
                log.warn("No compensation defined for step: {}", stepCode);
                return context;
        }
    }

    @Override
    public String getNextStep(SagaContext context, String stepCode) {
        return switch (stepCode) {
            case STEP_CREATE_REQUEST -> STEP_SUBMIT_FOR_APPROVAL;
            case STEP_SUBMIT_FOR_APPROVAL -> STEP_APPROVE_REQUEST;
            case STEP_APPROVE_REQUEST -> STEP_ISSUE_PO;
            case STEP_ISSUE_PO -> STEP_RECEIVE_DELIVERY;
            case STEP_RECEIVE_DELIVERY -> STEP_CONFIRM_DELIVERY;
            case STEP_CONFIRM_DELIVERY -> STEP_CLOSE;
            case STEP_CLOSE -> null;
            case STEP_RESUME -> getNextStepFromCurrentState(context);
            default -> null;
        };
    }

    /**
     * Determine the next step based on the current step stored in saga state.
     * <p>
     * NOTE: If currentStep is "Resume" (a corrupted value from the old resume endpoint bug),
     * treat it as if no currentStep exists and return the first step.
     */
    private String getNextStepFromCurrentState(SagaContext context) {
        Object currentStepObj = context.getData().get("currentStep");
        if (currentStepObj instanceof String currentStep) {
            // Handle corrupted currentStep="Resume" from old resume endpoint
            if ("Resume".equals(currentStep) || "RESUME".equals(currentStep)) {
                log.warn("Detected corrupted currentStep='Resume' for saga {}, resetting to first step", context.getSagaId());
                return STEP_CREATE_REQUEST;
            }
            return switch (currentStep) {
                case STEP_CREATE_REQUEST -> STEP_SUBMIT_FOR_APPROVAL;
                case STEP_SUBMIT_FOR_APPROVAL -> STEP_APPROVE_REQUEST;
                case STEP_APPROVE_REQUEST -> STEP_ISSUE_PO;
                case STEP_ISSUE_PO -> STEP_RECEIVE_DELIVERY;
                case STEP_RECEIVE_DELIVERY -> STEP_CONFIRM_DELIVERY;
                case STEP_CONFIRM_DELIVERY -> STEP_CLOSE;
                default -> null;
            };
        }
        // No currentStep - return first step
        return STEP_CREATE_REQUEST;
    }

    // --- Execute methods ---

    private SagaContext executeCreateRequest(SagaContext context, Map<String, Object> payload) throws Exception {
        // Extract projectId from context data
        Object projectIdObj = context.getData().get("projectId");
        UUID projectId = null;
        if (projectIdObj instanceof String s) {
            try {
                projectId = UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                // ignore - projectId may not be a valid UUID
            }
        } else if (projectIdObj instanceof UUID u) {
            projectId = u;
        }

        // Set the aggregate ID to the project ID (the business entity)
        if (projectId != null) {
            context.setAggregateId(projectId);
            context.setProjectId(projectId);
        }

        Map<String, Object> data = Map.of(
            "requestId", context.getSagaId().toString(),
            "projectId", context.getData().get("projectId"),
            "materialId", context.getData().get("materialId"),
            "quantity", context.getData().get("quantity"),
            "status", "Draft");

        WorkflowEvent event = WorkflowEvent.builder()
            .eventType("ProcurementRequestCreated")
            .sagaType("ProcurementSaga")
            .sagaId(context.getSagaId().toString())
            .aggregateId(projectId)
            .aggregateType("ProcurementRequest")
            .tenantId(context.getTenantId())
            .userId(context.getUserId())
            .data(data)
            .build();

        eventPublisher.publish(event);
        context.setCurrentStep(STEP_SUBMIT_FOR_APPROVAL);
        return context;
    }

    private SagaContext executeSubmitForApproval(SagaContext context, Map<String, Object> payload) throws Exception {
        WorkflowEvent event = WorkflowEvent.builder()
            .eventType("ProcurementRequestSubmittedForApproval")
            .sagaType("ProcurementSaga")
            .sagaId(context.getSagaId().toString())
            .aggregateId(context.getAggregateId())
            .aggregateType("ProcurementRequest")
            .tenantId(context.getTenantId())
            .userId(context.getUserId())
            .data(Map.of("status", "PendingApproval"))
            .build();

        eventPublisher.publish(event);
        context.setCurrentStep(STEP_APPROVE_REQUEST);
        return context;
    }

    private SagaContext executeApproveRequest(SagaContext context, Map<String, Object> payload) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> approvalData = (Map<String, Object>) context.getData().get("approvalData");

        if (approvalData != null && Boolean.FALSE.equals(approvalData.get("approved"))) {
            WorkflowEvent event = WorkflowEvent.builder()
                .eventType("ProcurementRequestRejected")
                .sagaType("ProcurementSaga")
                .sagaId(context.getSagaId().toString())
                .aggregateId(context.getAggregateId())
                .aggregateType("ProcurementRequest")
                .tenantId(context.getTenantId())
                .userId(context.getUserId())
                .data(Map.of("status", "Rejected", "reason", approvalData.get("rejectionReason")))
                .build();

            eventPublisher.publish(event);
            context.complete();
            return context;
        }

        WorkflowEvent event = WorkflowEvent.builder()
            .eventType("RequestApproved")
            .sagaType("ProcurementSaga")
            .sagaId(context.getSagaId().toString())
            .aggregateId(context.getAggregateId())
            .aggregateType("ProcurementRequest")
            .tenantId(context.getTenantId())
            .userId(context.getUserId())
            .data(Map.of("status", "Approved"))
            .build();

        eventPublisher.publish(event);
        context.setCurrentStep(STEP_ISSUE_PO);
        return context;
    }

    private SagaContext executeIssuePO(SagaContext context, Map<String, Object> payload) throws Exception {
        // Validate that a vendor is assigned before allowing PO issuance
        Object vendorIdObj = context.getData().get("vendorId");
        // Fallback: accept vendorId from the step payload if not yet in saga context
        if (vendorIdObj == null && payload != null) {
            vendorIdObj = payload.get("vendorId");
        }
        if (vendorIdObj == null || (vendorIdObj instanceof String s && s.isBlank())) {
            throw new SagaException("VENDOR_REQUIRED", "Vendor must be assigned before issuing a purchase order. Please assign a vendor via the vendor assignment modal or complete/abort the related bidding.", Map.of("sagaId", context.getSagaId().toString()));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> poData = (Map<String, Object>) context.getData().get("poData");

        WorkflowEvent event = WorkflowEvent.builder()
            .eventType("POIssued")
            .sagaType("ProcurementSaga")
            .sagaId(context.getSagaId().toString())
            .aggregateId(context.getAggregateId())
            .aggregateType("PurchaseOrder")
            .tenantId(context.getTenantId())
            .userId(context.getUserId())
            .data(poData != null ? poData : Map.of("status", "Issued"))
            .build();

        eventPublisher.publish(event);
        context.setCurrentStep(STEP_RECEIVE_DELIVERY);
        return context;
    }

    private SagaContext executeReceiveDelivery(SagaContext context, Map<String, Object> payload) throws Exception {
        String procurementSagaId = context.getSagaId().toString();

        // Retrieve all linked delivery sagas from the database
        String sagaIdPattern = "%" + procurementSagaId + "%";
        List<SagaEntity> deliverySagas = sagaRepository.findDeliverySagasByProcurementSagaId(sagaIdPattern);

        // Filter to only DeliverySaga type and exclude already completed ones
        List<SagaEntity> pendingDeliveries = deliverySagas.stream()
            .filter(s -> "DeliverySaga".equals(s.getSagaType()) && !"COMPLETED".equals(s.getStatus()))
            .toList();

        if (!pendingDeliveries.isEmpty()) {
            String pendingIds = pendingDeliveries.stream()
                .map(SagaEntity::getId)
                .collect(Collectors.joining(", "));
            throw new SagaException(
                "PENDING_DELIVERIES",
                "Cannot advance to ConfirmDelivery: " + pendingDeliveries.size() +
                " linked delivery schedule(s) are still in progress: " + pendingIds,
                Map.of(
                    "pendingDeliverySagas", pendingDeliveries.stream().map(SagaEntity::getId).collect(Collectors.toList()),
                    "pendingCount", pendingDeliveries.size()));
        }

        // All linked deliveries are complete — calculate total delivered quantity
        int totalDelivered = deliverySagas.stream()
            .mapToInt(s -> {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = objectMapper.readValue(s.getDataJson(), Map.class);
                    Object qty = data.get("quantity");
                    if (qty instanceof Number n) return n.intValue();
                } catch (Exception ignored) { /* ignore parse errors */ }
                return 0;
            })
            .sum();

        context.put("totalDeliveredQuantity", totalDelivered);

        WorkflowEvent event = WorkflowEvent.builder()
            .eventType("DeliveryReceived")
            .sagaType("ProcurementSaga")
            .sagaId(context.getSagaId().toString())
            .aggregateId(context.getAggregateId())
            .aggregateType("Delivery")
            .tenantId(context.getTenantId())
            .userId(context.getUserId())
            .data(Map.of(
                "status", "Arrived",
                "totalDeliveredQuantity", totalDelivered,
                "deliverySagasCompleted", deliverySagas.stream().map(SagaEntity::getId).collect(Collectors.toList())))
            .build();

        eventPublisher.publish(event);
        context.setCurrentStep(STEP_CONFIRM_DELIVERY);
        return context;
    }

    private SagaContext executeConfirmDelivery(SagaContext context, Map<String, Object> payload) throws Exception {
        WorkflowEvent event = WorkflowEvent.builder()
            .eventType("DeliveryConfirmed")
            .sagaType("ProcurementSaga")
            .sagaId(context.getSagaId().toString())
            .aggregateId(context.getAggregateId())
            .aggregateType("Delivery")
            .tenantId(context.getTenantId())
            .userId(context.getUserId())
            .data(Map.of("status", "Confirmed"))
            .build();

        eventPublisher.publish(event);
        context.setCurrentStep(STEP_CLOSE);
        return context;
    }

    private SagaContext executeClose(SagaContext context, Map<String, Object> payload) throws Exception {
        WorkflowEvent event = WorkflowEvent.builder()
            .eventType("ProcurementClosed")
            .sagaType("ProcurementSaga")
            .sagaId(context.getSagaId().toString())
            .aggregateId(context.getAggregateId())
            .aggregateType("ProcurementRequest")
            .tenantId(context.getTenantId())
            .userId(context.getUserId())
            .data(Map.of("status", "Closed"))
            .build();

        eventPublisher.publish(event);
        context.complete();
        return context;
    }

    /**
     * Resume a paused/stalled saga by advancing to the next step based on current state.
     * This is used when a saga needs manual intervention or was paused waiting for external action.
     */
    private SagaContext executeResume(SagaContext context, Map<String, Object> payload) throws Exception {
        // Extract optional stepCode from payload (passed by SagaController)
        @SuppressWarnings("unchecked")
        Map<String, Object> resumePayload = payload != null ? payload : Map.of();
        Object stepCodeObj = resumePayload.get("stepCode");
        String targetStep = stepCodeObj instanceof String s ? s : null;

        // Get current step from context data
        Object currentStepObj = context.getData().get("currentStep");
        String currentStep = currentStepObj instanceof String s ? s : null;

        log.info("Resuming procurement saga: sagaId={}, currentStep={}, targetStep={}",
            context.getSagaId(), currentStep, targetStep);

        // If a specific target step is provided, use it; otherwise advance from current
        String nextStep;
        if (targetStep != null) {
            nextStep = targetStep;
        } else if (currentStep != null) {
            // Advance to the next step in the sequence
            nextStep = switch (currentStep) {
                case STEP_CREATE_REQUEST -> STEP_SUBMIT_FOR_APPROVAL;
                case STEP_SUBMIT_FOR_APPROVAL -> STEP_APPROVE_REQUEST;
                case STEP_APPROVE_REQUEST -> STEP_ISSUE_PO;
                case STEP_ISSUE_PO -> STEP_RECEIVE_DELIVERY;
                case STEP_RECEIVE_DELIVERY -> STEP_CONFIRM_DELIVERY;
                case STEP_CONFIRM_DELIVERY -> STEP_CLOSE;
                default -> null;
            };
        } else {
            // No current step info, try to determine from saga status
            nextStep = STEP_SUBMIT_FOR_APPROVAL; // Default to first actionable step
        }

        // Publish resume event
        WorkflowEvent event = WorkflowEvent.builder()
            .eventType("ProcurementSagaResumed")
            .sagaType("ProcurementSaga")
            .sagaId(context.getSagaId().toString())
            .aggregateId(context.getAggregateId())
            .aggregateType("ProcurementRequest")
            .tenantId(context.getTenantId())
            .userId(context.getUserId())
            .data(Map.of(
                "status", "Resumed",
                "previousStep", currentStep,
                "nextStep", nextStep))
            .build();

        eventPublisher.publish(event);

        // Update current step to the next step
        if (nextStep != null) {
            context.setCurrentStep(nextStep);
        } else {
            // Saga is complete
            context.complete();
        }

        return context;
    }

    // --- Compensation methods ---

    private SagaContext compensateCreateRequest(SagaContext context) {
        log.info("Compensating create request for sagaId={}", context.getSagaId());
        return context;
    }

    private SagaContext compensateSubmitForApproval(SagaContext context) {
        log.info("Compensating submit for approval for sagaId={}", context.getSagaId());
        return context;
    }

    private SagaContext compensateApproveRequest(SagaContext context) {
        try {
            WorkflowEvent event = WorkflowEvent.builder()
                .eventType("RequestApprovalReverted")
                .sagaType("ProcurementSaga")
                .sagaId(context.getSagaId().toString())
                .aggregateId(context.getAggregateId())
                .aggregateType("ProcurementRequest")
                .tenantId(context.getTenantId())
                .userId(context.getUserId())
                .data(Map.of("status", "Draft", "reverted", true))
                .build();

            eventPublisher.publish(event);
        } catch (Exception e) {
            log.error("Failed to compensate approval for sagaId={}: {}", context.getSagaId(), e.getMessage(), e);
        }
        return context;
    }

    private SagaContext compensateIssuePO(SagaContext context) {
        try {
            WorkflowEvent event = WorkflowEvent.builder()
                .eventType("POCommitmentReleased")
                .sagaType("ProcurementSaga")
                .sagaId(context.getSagaId().toString())
                .aggregateId(context.getAggregateId())
                .aggregateType("PurchaseOrder")
                .tenantId(context.getTenantId())
                .userId(context.getUserId())
                .data(Map.of("status", "CommitmentReleased"))
                .build();

            eventPublisher.publish(event);
        } catch (Exception e) {
            log.error("Failed to compensate PO for sagaId={}: {}", context.getSagaId(), e.getMessage(), e);
        }
        return context;
    }

    private SagaContext compensateReceiveDelivery(SagaContext context) {
        try {
            WorkflowEvent event = WorkflowEvent.builder()
                .eventType("POCancelled")
                .sagaType("ProcurementSaga")
                .sagaId(context.getSagaId().toString())
                .aggregateId(context.getAggregateId())
                .aggregateType("PurchaseOrder")
                .tenantId(context.getTenantId())
                .userId(context.getUserId())
                .data(Map.of("status", "Cancelled", "reason", "Delivery failed"))
                .build();

            eventPublisher.publish(event);
        } catch (Exception e) {
            log.error("Failed to compensate delivery for sagaId={}: {}", context.getSagaId(), e.getMessage(), e);
        }
        return context;
    }

    private SagaContext compensateConfirmDelivery(SagaContext context) {
        log.info("Compensating confirm delivery for sagaId={}", context.getSagaId());
        return context;
    }
}
