package io.cerp.workflow.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cerp.workflow.event.WorkflowEvent;
import io.cerp.workflow.service.EventPublisher;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Saga handler for the Material Delivery and Discrepancy Resolution workflow.
 *
 * <p>
 * This handler manages the delivery lifecycle including:
 * <ol>
 * <li>Schedule Delivery</li>
 * <li>Start Transit</li>
 * <li>Delivery Arrived</li>
 * <li>Start Unloading</li>
 * <li>Confirm Delivery (or Raise Discrepancy)</li>
 * <li>Resolve Discrepancy (if applicable)</li>
 * <li>Close</li>
 * </ol>
 *
 * <p>
 * Key features:
 * <ul>
 * <li>SLA: 24h triage for discrepancies</li>
 * <li>SLA: 5 days resolution for discrepancies</li>
 * <li>Branch: Discrepancy raised during unloading</li>
 * </ul>
 */
@Component
public class DeliverySagaHandler implements SagaHandler {

    private static final Logger log = LoggerFactory.getLogger(DeliverySagaHandler.class);

    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public DeliverySagaHandler(EventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    // --- Step code for this handler ---
    public static final String STEP_CODE = "DeliverySaga";

    // --- Step names ---
    public static final String STEP_SCHEDULE_DELIVERY = "ScheduleDelivery";
    public static final String STEP_START_TRANSIT = "StartTransit";
    public static final String STEP_DELIVERY_ARRIVED = "DeliveryArrived";
    public static final String STEP_START_UNLOADING = "StartUnloading";
    public static final String STEP_CONFIRM_DELIVERY = "ConfirmDelivery";
    public static final String STEP_RAISE_DISCREPANCY = "RaiseDiscrepancy";
    public static final String STEP_RESOLVE_DISCREPANCY = "ResolveDiscrepancy";
    public static final String STEP_CLOSE = "CloseDelivery";

    @Override
    public String getStepCode() {
        return STEP_CODE;
    }

    @Override
    public SagaContext execute(SagaContext context, String stepCode, Map<String, Object> payload) throws Exception {
        log.info("Executing delivery saga step: {} for sagaId={}", stepCode, context.getSagaId());

        switch (stepCode) {
            case STEP_SCHEDULE_DELIVERY:
                executeScheduleDelivery(context);
                return context;
            case STEP_START_TRANSIT:
                executeStartTransit(context);
                return context;
            case STEP_DELIVERY_ARRIVED:
                executeDeliveryArrived(context);
                return context;
            case STEP_START_UNLOADING:
                executeStartUnloading(context);
                return context;
            case STEP_CONFIRM_DELIVERY:
                executeConfirmDelivery(context);
                return context;
            case STEP_RAISE_DISCREPANCY:
                executeRaiseDiscrepancy(context);
                return context;
            case STEP_RESOLVE_DISCREPANCY:
                executeResolveDiscrepancy(context);
                return context;
            case STEP_CLOSE:
                executeClose(context);
                return context;
            default:
                throw new IllegalArgumentException("Unknown step: " + stepCode);
        }
    }

    @Override
    public SagaContext compensate(SagaContext context, String stepCode) throws Exception {
        log.info("Compensating delivery saga step: {} for sagaId={}", stepCode, context.getSagaId());

        switch (stepCode) {
            case STEP_SCHEDULE_DELIVERY:
                compensateScheduleDelivery(context);
                break;
            case STEP_START_TRANSIT:
                compensateStartTransit(context);
                break;
            case STEP_DELIVERY_ARRIVED:
                compensateDeliveryArrived(context);
                break;
            case STEP_START_UNLOADING:
                compensateStartUnloading(context);
                break;
            case STEP_CONFIRM_DELIVERY:
                compensateConfirmDelivery(context);
                break;
            case STEP_RAISE_DISCREPANCY:
                compensateRaiseDiscrepancy(context);
                break;
            case STEP_RESOLVE_DISCREPANCY:
                compensateResolveDiscrepancy(context);
                break;
            default:
                log.warn("No compensation defined for step: {}", stepCode);
                break;
        }
        return context;
    }

    @Override
    public String getNextStep(SagaContext context, String stepCode) {
        return switch (stepCode) {
            case STEP_SCHEDULE_DELIVERY -> STEP_START_TRANSIT;
            case STEP_START_TRANSIT -> STEP_DELIVERY_ARRIVED;
            case STEP_DELIVERY_ARRIVED -> STEP_START_UNLOADING;
            case STEP_START_UNLOADING -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> inspectionData = (Map<String, Object>) context.getData().get("inspectionData");
                Boolean hasDiscrepancy = inspectionData != null ? (Boolean) inspectionData.get("hasDiscrepancy") : false;
                if (Boolean.TRUE.equals(hasDiscrepancy)) {
                    yield STEP_RESOLVE_DISCREPANCY;
                } else {
                    yield STEP_CONFIRM_DELIVERY;
                }
            }
            case STEP_CONFIRM_DELIVERY -> STEP_CLOSE;
            case STEP_RAISE_DISCREPANCY -> STEP_RESOLVE_DISCREPANCY;
            case STEP_RESOLVE_DISCREPANCY -> STEP_CONFIRM_DELIVERY;
            case STEP_CLOSE -> null;
            default -> null;
        };
    }

    // --- Execute methods ---

    private void executeScheduleDelivery(SagaContext context) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> deliveryData = (Map<String, Object>) context.getData().get("deliveryData");

        WorkflowEvent event = WorkflowEvent.builder()
            .eventType("DeliveryScheduled")
            .sagaType("DeliverySaga")
            .sagaId(context.getSagaId().toString())
            .aggregateId(context.getAggregateId())
            .aggregateType("Delivery")
            .tenantId(context.getTenantId())
            .userId(context.getUserId())
            .data(deliveryData != null ? deliveryData : Map.of("status", "Scheduled"))
            .build();

        eventPublisher.publish(event);
        context.setCurrentStep(STEP_START_TRANSIT);
    }

    private void executeStartTransit(SagaContext context) throws Exception {
        WorkflowEvent event = WorkflowEvent.builder()
            .eventType("DeliveryInTransit")
            .sagaType("DeliverySaga")
            .sagaId(context.getSagaId().toString())
            .aggregateId(context.getAggregateId())
            .aggregateType("Delivery")
            .tenantId(context.getTenantId())
            .userId(context.getUserId())
            .data(Map.of("status", "In_Transit"))
            .build();

        eventPublisher.publish(event);
        context.setCurrentStep(STEP_DELIVERY_ARRIVED);
    }

    private void executeDeliveryArrived(SagaContext context) throws Exception {
        WorkflowEvent event = WorkflowEvent.builder()
            .eventType("DeliveryArrived")
            .sagaType("DeliverySaga")
            .sagaId(context.getSagaId().toString())
            .aggregateId(context.getAggregateId())
            .aggregateType("Delivery")
            .tenantId(context.getTenantId())
            .userId(context.getUserId())
            .data(Map.of("status", "Arrived", "etaMet", true))
            .build();

        eventPublisher.publish(event);
        context.setCurrentStep(STEP_START_UNLOADING);
    }

    private void executeStartUnloading(SagaContext context) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> inspectionData = (Map<String, Object>) context.getData().get("inspectionData");

        Boolean hasDiscrepancy = inspectionData != null ? (Boolean) inspectionData.get("hasDiscrepancy") : false;

        if (Boolean.TRUE.equals(hasDiscrepancy)) {
            WorkflowEvent event = WorkflowEvent.builder()
                .eventType("DiscrepancyRaised")
                .sagaType("DeliverySaga")
                .sagaId(context.getSagaId().toString())
                .aggregateId(context.getAggregateId())
                .aggregateType("Delivery")
                .tenantId(context.getTenantId())
                .userId(context.getUserId())
                .data(inspectionData != null ? inspectionData
                        : Map.of(
                            "status", "Discrepancy_Raised",
                            "type", "quantity_shortage",
                            "severity", "minor"))
                .build();

            eventPublisher.publish(event);
            context.setCurrentStep(STEP_RESOLVE_DISCREPANCY);
        } else {
            context.setCurrentStep(STEP_CONFIRM_DELIVERY);
        }
    }

    private void executeConfirmDelivery(SagaContext context) throws Exception {
        WorkflowEvent event = WorkflowEvent.builder()
            .eventType("DeliveryConfirmed")
            .sagaType("DeliverySaga")
            .sagaId(context.getSagaId().toString())
            .aggregateId(context.getAggregateId())
            .aggregateType("Delivery")
            .tenantId(context.getTenantId())
            .userId(context.getUserId())
            .data(Map.of("status", "Confirmed", "confirmBy", context.getUserId()))
            .build();

        eventPublisher.publish(event);
        context.setCurrentStep(STEP_CLOSE);
    }

    private void executeRaiseDiscrepancy(SagaContext context) throws Exception {
        WorkflowEvent event = WorkflowEvent.builder()
            .eventType("DiscrepancyReported")
            .sagaType("DeliverySaga")
            .sagaId(context.getSagaId().toString())
            .aggregateId(context.getAggregateId())
            .aggregateType("Delivery")
            .tenantId(context.getTenantId())
            .userId(context.getUserId())
            .data(Map.of(
                "status", "Discrepancy_Raised",
                "triageSLA", "24h",
                "resolutionSLA", "5days"))
            .build();

        eventPublisher.publish(event);
        context.setCurrentStep(STEP_RESOLVE_DISCREPANCY);
    }

    private void executeResolveDiscrepancy(SagaContext context) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> resolutionData = (Map<String, Object>) context.getData().get("resolutionData");

        WorkflowEvent event = WorkflowEvent.builder()
            .eventType("DiscrepancyResolved")
            .sagaType("DeliverySaga")
            .sagaId(context.getSagaId().toString())
            .aggregateId(context.getAggregateId())
            .aggregateType("Delivery")
            .tenantId(context.getTenantId())
            .userId(context.getUserId())
            .data(resolutionData != null ? resolutionData
                    : Map.of(
                        "status", "Resolved",
                        "resolution", "accepted_as_is"))
            .build();

        eventPublisher.publish(event);
        context.setCurrentStep(STEP_CONFIRM_DELIVERY);
    }

    private void executeClose(SagaContext context) throws Exception {
        WorkflowEvent event = WorkflowEvent.builder()
            .eventType("DeliveryClosed")
            .sagaType("DeliverySaga")
            .sagaId(context.getSagaId().toString())
            .aggregateId(context.getAggregateId())
            .aggregateType("Delivery")
            .tenantId(context.getTenantId())
            .userId(context.getUserId())
            .data(Map.of("status", "Closed"))
            .build();

        eventPublisher.publish(event);
        context.complete();
    }

    // --- Compensation methods ---

    private void compensateScheduleDelivery(SagaContext context) {
        log.info("Compensating schedule delivery for sagaId={}", context.getSagaId());
    }

    private void compensateStartTransit(SagaContext context) {
        try {
            WorkflowEvent event = WorkflowEvent.builder()
                .eventType("DeliveryCancelled")
                .sagaType("DeliverySaga")
                .sagaId(context.getSagaId().toString())
                .aggregateId(context.getAggregateId())
                .aggregateType("Delivery")
                .tenantId(context.getTenantId())
                .userId(context.getUserId())
                .data(Map.of("status", "Cancelled"))
                .build();

            eventPublisher.publish(event);
        } catch (Exception e) {
            log.error("Failed to compensate transit for sagaId={}: {}", context.getSagaId(), e.getMessage(), e);
        }
    }

    private void compensateDeliveryArrived(SagaContext context) {
        log.info("Compensating delivery arrived for sagaId={}", context.getSagaId());
    }

    private void compensateStartUnloading(SagaContext context) {
        log.info("Compensating start unloading for sagaId={}", context.getSagaId());
    }

    private void compensateConfirmDelivery(SagaContext context) {
        log.info("Compensating confirm delivery for sagaId={}", context.getSagaId());
    }

    private void compensateRaiseDiscrepancy(SagaContext context) {
        log.info("Compensating raise discrepancy for sagaId={}", context.getSagaId());
    }

    private void compensateResolveDiscrepancy(SagaContext context) {
        try {
            WorkflowEvent event = WorkflowEvent.builder()
                .eventType("DiscrepancyReopened")
                .sagaType("DeliverySaga")
                .sagaId(context.getSagaId().toString())
                .aggregateId(context.getAggregateId())
                .aggregateType("Delivery")
                .tenantId(context.getTenantId())
                .userId(context.getUserId())
                .data(Map.of("status", "Discrepancy_Raised", "reason", "compensation"))
                .build();

            eventPublisher.publish(event);
        } catch (Exception e) {
            log.error("Failed to compensate discrepancy for sagaId={}: {}", context.getSagaId(), e.getMessage(), e);
        }
    }
}
