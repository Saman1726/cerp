package io.cerp.workflow.saga;

import java.util.Map;
import java.util.UUID;

/**
 * Handler for a specific step in a saga workflow.
 *
 * <p>
 * Each saga step handler implements the {@link #execute} method to perform the business logic and the {@link #compensate} method to define the compensating transaction
 * if the saga needs to be rolled back.
 *
 * <p>
 * The handler is registered with a step code that identifies which command it handles. The orchestrator calls {@link #getNextStep} to determine the subsequent step in
 * the workflow.
 */
public interface SagaHandler {

    /**
     * Execute the saga step.
     *
     * @param context
     *            the saga context containing state and data
     * @param stepCode
     *            the code identifying which step to execute
     * @param payload
     *            additional payload data for the step
     * @return updated saga context after step execution
     * @throws Exception
     *             if the step fails
     */
    SagaContext execute(SagaContext context, String stepCode, Map<String, Object> payload) throws Exception;

    /**
     * Compensate (roll back) the saga step.
     *
     * @param context
     *            the saga context containing state and data
     * @param stepCode
     *            the code identifying which step to compensate
     * @return updated saga context after compensation
     * @throws Exception
     *             if compensation fails
     */
    default SagaContext compensate(SagaContext context, String stepCode) throws Exception {
        // No compensation by default
        return context;
    }

    /**
     * Get the next step code after this step completes successfully.
     *
     * @param context
     *            the saga context
     * @param stepCode
     *            the current step code
     * @return the next step code, or null if this is the final step
     */
    default String getNextStep(SagaContext context, String stepCode) {
        return null;
    }

    /**
     * Get the step code that this handler manages.
     *
     * @return the step code
     */
    String getStepCode();

    /**
     * Get the step name for logging and monitoring.
     *
     * @return the step name
     */
    default String getStepName() {
        return this.getClass().getSimpleName();
    }
}
