package io.cerp.workflow.state;

import java.util.Map;

/**
 * State machine definitions for the Workflow and WorkflowStep aggregates.
 *
 * <p>
 * These mirror the guarded-status transitions previously hand-rolled in
 * {@code Workflow#requireStatus} and {@code WorkflowStep#requireStatus} exactly — same states,
 * same transition codes, same allow-lists. No guard functions are needed: every existing guard is
 * a pure "is current status in this allow-list" check, which a state machine expresses natively
 * via one {@code withTransition} call per allowed source state.
 */
public final class WorkflowStateMachines {

    private WorkflowStateMachines() {
    }

    // --- Aggregate type constants ---
    public static final String AGGREGATE_WORKFLOW = "Workflow";
    public static final String AGGREGATE_WORKFLOW_STEP = "WorkflowStep";

    // --- State constants (verbatim Workflow.WorkflowStatus / WorkflowStep.StepStatus names) ---
    public static final String STATE_PLANNED = "PLANNED";
    public static final String STATE_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATE_BLOCKED = "BLOCKED";
    public static final String STATE_ON_HOLD = "ON_HOLD";
    public static final String STATE_COMPLETED = "COMPLETED";
    public static final String STATE_APPROVED = "APPROVED";
    public static final String STATE_REJECTED = "REJECTED";
    public static final String STATE_CANCELED = "CANCELED";
    public static final String STEP_STATE_NOT_STARTED = "NOT_STARTED";

    // --- Transition constants ---
    public static final String TRANSITION_START = "Start";
    public static final String TRANSITION_BLOCK = "Block";
    public static final String TRANSITION_UNBLOCK = "Unblock";
    public static final String TRANSITION_HOLD = "Hold";
    public static final String TRANSITION_UNHOLD = "Unhold";
    public static final String TRANSITION_COMPLETE = "Complete";
    public static final String TRANSITION_APPROVE = "Approve";
    public static final String TRANSITION_REJECT = "Reject";
    public static final String TRANSITION_CANCEL = "Cancel";

    /**
     * Get the state machine definition for Workflow.
     *
     * <p>
     * States: PLANNED -&gt; IN_PROGRESS -&gt; {BLOCKED, ON_HOLD, COMPLETED}; BLOCKED/IN_PROGRESS -&gt;
     * ON_HOLD; COMPLETED -&gt; {APPROVED, REJECTED}; PLANNED/IN_PROGRESS -&gt; CANCELED.
     */
    public static StateMachineDefinition<String, String> workflowMachine() {
        return StateMachineDefinition.<String, String> builder()
            .withInitialState(STATE_PLANNED)
            .withState(STATE_IN_PROGRESS)
            .withState(STATE_BLOCKED)
            .withState(STATE_ON_HOLD)
            .withState(STATE_COMPLETED)
            .withState(STATE_APPROVED)
            .withState(STATE_REJECTED)
            .withState(STATE_CANCELED)
            .withTransition(STATE_PLANNED, TRANSITION_START, STATE_IN_PROGRESS)
            .withTransition(STATE_IN_PROGRESS, TRANSITION_BLOCK, STATE_BLOCKED)
            .withTransition(STATE_BLOCKED, TRANSITION_UNBLOCK, STATE_IN_PROGRESS)
            .withTransition(STATE_IN_PROGRESS, TRANSITION_HOLD, STATE_ON_HOLD)
            .withTransition(STATE_BLOCKED, TRANSITION_HOLD, STATE_ON_HOLD)
            .withTransition(STATE_ON_HOLD, TRANSITION_UNHOLD, STATE_IN_PROGRESS)
            .withTransition(STATE_IN_PROGRESS, TRANSITION_COMPLETE, STATE_COMPLETED)
            .withTransition(STATE_COMPLETED, TRANSITION_APPROVE, STATE_APPROVED)
            .withTransition(STATE_COMPLETED, TRANSITION_REJECT, STATE_REJECTED)
            .withTransition(STATE_PLANNED, TRANSITION_CANCEL, STATE_CANCELED)
            .withTransition(STATE_IN_PROGRESS, TRANSITION_CANCEL, STATE_CANCELED)
            .build();
    }

    /**
     * Get the state machine definition for WorkflowStep.
     *
     * <p>
     * States: NOT_STARTED -&gt; IN_PROGRESS -&gt; {BLOCKED, COMPLETED}; BLOCKED -&gt; IN_PROGRESS;
     * COMPLETED -&gt; {APPROVED, REJECTED}.
     */
    public static StateMachineDefinition<String, String> workflowStepMachine() {
        return StateMachineDefinition.<String, String> builder()
            .withInitialState(STEP_STATE_NOT_STARTED)
            .withState(STATE_IN_PROGRESS)
            .withState(STATE_BLOCKED)
            .withState(STATE_COMPLETED)
            .withState(STATE_APPROVED)
            .withState(STATE_REJECTED)
            .withTransition(STEP_STATE_NOT_STARTED, TRANSITION_START, STATE_IN_PROGRESS)
            .withTransition(STATE_IN_PROGRESS, TRANSITION_COMPLETE, STATE_COMPLETED)
            .withTransition(STATE_COMPLETED, TRANSITION_APPROVE, STATE_APPROVED)
            .withTransition(STATE_COMPLETED, TRANSITION_REJECT, STATE_REJECTED)
            .withTransition(STATE_IN_PROGRESS, TRANSITION_BLOCK, STATE_BLOCKED)
            .withTransition(STATE_BLOCKED, TRANSITION_UNBLOCK, STATE_IN_PROGRESS)
            .build();
    }

    /**
     * Get both machines keyed by aggregate type, ready to hand to a {@link StateMachineEngine}.
     */
    public static Map<String, StateMachineDefinition<String, String>> allMachines() {
        return Map.of(
            AGGREGATE_WORKFLOW, workflowMachine(),
            AGGREGATE_WORKFLOW_STEP, workflowStepMachine());
    }
}
