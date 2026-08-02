package io.cerp.workflow.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.cerp.workflow.saga.BiddingSagaHandler;
import io.cerp.workflow.saga.DeliverySagaHandler;
import io.cerp.workflow.saga.ProcurementSagaHandler;
import io.cerp.workflow.saga.SagaHandler;
import io.cerp.workflow.saga.SagaOrchestrator;

/**
 * Registers all saga handlers with the saga orchestrator at application startup.
 *
 * <p>
 * Maps frontend saga type strings (e.g. "PROCUREMENT_WORKFLOW") to the corresponding
 * Spring-managed SagaHandler beans so that {@link io.cerp.workflow.saga.DefaultSagaOrchestrator#findHandlerForStep}
 * can locate them by saga type.
 */
@Configuration
public class SagaHandlerRegistration {

    private static final Logger log = LoggerFactory.getLogger(SagaHandlerRegistration.class);

    /**
     * Register all available saga handlers with the orchestrator.
     *
     * <p>
     * Saga type keys are chosen to match the sagas created by the frontend:
     * <ul>
     *   <li>PROCUREMENT_WORKFLOW → ProcurementSagaHandler</li>
     *   <li>BIDDING_WORKFLOW → BiddingSagaHandler</li>
     *   <li>DELIVERY_WORKFLOW → DeliverySagaHandler</li>
     * </ul>
     */
    @Bean
    public CommandLineRunner registerSagaHandlers(SagaOrchestrator orchestrator,
            List<SagaHandler> sagaHandlers) {
        return args -> {
            // Register handlers with saga types that match frontend sagaType values
            for (SagaHandler handler : sagaHandlers) {
                String stepCode = handler.getStepCode();
                log.info("Discovered SagaHandler with getStepCode={}: {}", stepCode, handler.getClass().getSimpleName());
            }

            // Explicitly register each handler with the saga type key the frontend uses
            orchestrator.registerHandler("PROCUREMENT_WORKFLOW", sagaHandlerByType(ProcurementSagaHandler.class, sagaHandlers));
            orchestrator.registerHandler("BIDDING_WORKFLOW", sagaHandlerByType(BiddingSagaHandler.class, sagaHandlers));
            orchestrator.registerHandler("DELIVERY_WORKFLOW", sagaHandlerByType(DeliverySagaHandler.class, sagaHandlers));

            log.info("Saga handler registration complete. Registered types: PROCUREMENT_WORKFLOW, BIDDING_WORKFLOW, DELIVERY_WORKFLOW");
        };
    }

    @SuppressWarnings("unchecked")
    private <T extends SagaHandler> T sagaHandlerByType(Class<T> type, List<SagaHandler> handlers) {
        for (SagaHandler h : handlers) {
            if (type.isInstance(h)) {
                return (T) h;
            }
        }
        throw new IllegalStateException("SagaHandler of type " + type.getSimpleName() + " not found in Spring context");
    }
}