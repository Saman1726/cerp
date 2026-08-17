package io.cerp.workflow.saga;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Saga orchestrator for coordinating cross-service business workflows.
 *
 * <p>
 * Implements the orchestrator-based saga pattern where a central coordinator manages the sequence of commands and handles compensations.
 *
 * @see <a href="https://microservices.io/patterns/data/saga.html">Saga Pattern</a>
 */
public interface SagaOrchestrator {

    /**
     * Start a new saga instance with the given context.
     *
     * @param sagaType
     *            the saga type (e.g., "PROCUREMENT_DELIVERY_FINANCIALS")
     * @param context
     *            the saga context containing aggregate IDs and workflow state
     * @return the saga instance ID
     */
    UUID startSaga(String sagaType, SagaContext context);

    /**
     * Execute the next step in a saga instance.
     *
     * @param sagaId
     *            the saga instance ID
     * @param command
     *            the command to execute
     * @param payload
     *            additional payload for the command
     * @return the updated saga context
     */
    SagaContext executeStep(UUID sagaId, String command, Map<String, Object> payload);

    /**
     * Compensate (rollback) a saga step.
     *
     * @param sagaId
     *            the saga instance ID
     * @param stepCode
     *            the step code to compensate
     * @return the updated saga context after compensation
     */
    SagaContext compensateStep(UUID sagaId, String stepCode);

    /**
     * Pause a running saga instance.
     *
     * @param sagaId
     *            the saga instance ID
     */
    void pauseSaga(UUID sagaId);

    /**
     * Resume a paused saga instance (transition from PAUSED to RUNNING).
     *
     * @param sagaId
     *            the saga instance ID
     */
    void resumeSaga(UUID sagaId);

    /**
     * Complete a saga instance successfully.
     *
     * @param sagaId
     *            the saga instance ID
     * @param result
     *            the final result of the saga
     */
    void completeSaga(UUID sagaId, Map<String, Object> result);

    /**
     * Abort a saga instance due to error.
     *
     * @param sagaId
     *            the saga instance ID
     * @param error
     *            the error that caused the abort
     */
    void abortSaga(UUID sagaId, SagaError error);

    /**
     * Get the current state of a saga instance.
     *
     * @param sagaId
     *            the saga instance ID
     * @return the saga state
     */
    SagaState getSagaState(UUID sagaId);

    /**
     * Get the saga state by ID with optional data deserialization.
     *
     * @param sagaId
     *            the saga instance ID
     * @return the saga state, or empty if not found
     */
    java.util.Optional<SagaState> getSagaStateOptional(UUID sagaId);

    /**
     * Register a saga handler for a specific saga type.
     *
     * @param sagaType
     *            the saga type
     * @param handler
     *            the saga handler
     */
    void registerHandler(String sagaType, SagaHandler handler);

    /**
     * Get all saga instances for a given aggregate.
     *
     * @param aggregateType
     *            the aggregate type
     * @param aggregateId
     *            the aggregate ID
     * @return list of saga instances
     */
    List<SagaState> getSagaInstancesByAggregate(String aggregateType, UUID aggregateId);

    /**
     * Update the data payload of an existing saga without changing its state.
     *
     * <p>
     * This method merges the provided data fields into the saga's existing data map,
     * preserving the saga's current status, steps, and history.
     * </p>
     *
     * @param sagaId
     *            the saga instance ID
     * @param newData
     *            the new data fields to merge into the saga
     * @return the updated saga context
     */
    SagaContext updateSagaData(UUID sagaId, Map<String, Object> newData);

    /**
     * List saga states by access mode, using direct repository queries instead of in-memory filtering.
     *
     * @param userId
     *            the user ID for access control filtering
     * @param accessMode
     *            the access mode ("created", "assigned", "member", "project", or null for combined)
     * @param projectIds
     *            project IDs to filter by (used when accessMode is "project" or default for non-admin users)
     * @return list of saga states matching the access criteria
     */
    List<SagaState> listSagasByAccess(String userId, String accessMode, List<String> projectIds);
}
