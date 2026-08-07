package io.cerp.workflow.state;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Exhaustively checks workflowMachine()/workflowStepMachine() against the exact allow-lists
 * previously hand-rolled in Workflow#requireStatus / WorkflowStep#requireStatus, across every
 * (state x transition) combination -- not just the reachable ones.
 */
class WorkflowStateMachinesTest {

    private static final StateMachineDefinition<String, String> WORKFLOW_MACHINE = WorkflowStateMachines
        .workflowMachine();
    private static final StateMachineDefinition<String, String> STEP_MACHINE = WorkflowStateMachines
        .workflowStepMachine();

    private static final String[] WORKFLOW_STATES = {
            WorkflowStateMachines.STATE_PLANNED, WorkflowStateMachines.STATE_IN_PROGRESS,
            WorkflowStateMachines.STATE_BLOCKED, WorkflowStateMachines.STATE_ON_HOLD,
            WorkflowStateMachines.STATE_COMPLETED, WorkflowStateMachines.STATE_APPROVED,
            WorkflowStateMachines.STATE_REJECTED, WorkflowStateMachines.STATE_CANCELED,
    };

    private static final String[] WORKFLOW_TRANSITIONS = {
            WorkflowStateMachines.TRANSITION_START, WorkflowStateMachines.TRANSITION_BLOCK,
            WorkflowStateMachines.TRANSITION_UNBLOCK, WorkflowStateMachines.TRANSITION_HOLD,
            WorkflowStateMachines.TRANSITION_UNHOLD, WorkflowStateMachines.TRANSITION_COMPLETE,
            WorkflowStateMachines.TRANSITION_APPROVE, WorkflowStateMachines.TRANSITION_REJECT,
            WorkflowStateMachines.TRANSITION_CANCEL,
    };

    private static final String[] STEP_STATES = {
            WorkflowStateMachines.STEP_STATE_NOT_STARTED, WorkflowStateMachines.STATE_IN_PROGRESS,
            WorkflowStateMachines.STATE_BLOCKED, WorkflowStateMachines.STATE_COMPLETED,
            WorkflowStateMachines.STATE_APPROVED, WorkflowStateMachines.STATE_REJECTED,
    };

    private static final String[] STEP_TRANSITIONS = {
            WorkflowStateMachines.TRANSITION_START, WorkflowStateMachines.TRANSITION_COMPLETE,
            WorkflowStateMachines.TRANSITION_APPROVE, WorkflowStateMachines.TRANSITION_REJECT,
            WorkflowStateMachines.TRANSITION_BLOCK, WorkflowStateMachines.TRANSITION_UNBLOCK,
    };

    private static boolean workflowTransitionAllowed(String fromState, String transition) {
        return switch (transition) {
            case "Start" -> fromState.equals(WorkflowStateMachines.STATE_PLANNED);
            case "Block" -> fromState.equals(WorkflowStateMachines.STATE_IN_PROGRESS);
            case "Unblock" -> fromState.equals(WorkflowStateMachines.STATE_BLOCKED);
            case "Hold" -> fromState.equals(WorkflowStateMachines.STATE_IN_PROGRESS)
                    || fromState.equals(WorkflowStateMachines.STATE_BLOCKED);
            case "Unhold" -> fromState.equals(WorkflowStateMachines.STATE_ON_HOLD);
            case "Complete" -> fromState.equals(WorkflowStateMachines.STATE_IN_PROGRESS);
            case "Approve", "Reject" -> fromState.equals(WorkflowStateMachines.STATE_COMPLETED);
            case "Cancel" -> fromState.equals(WorkflowStateMachines.STATE_PLANNED)
                    || fromState.equals(WorkflowStateMachines.STATE_IN_PROGRESS);
            default -> throw new IllegalArgumentException("Unknown transition: " + transition);
        };
    }

    private static boolean stepTransitionAllowed(String fromState, String transition) {
        return switch (transition) {
            case "Start" -> fromState.equals(WorkflowStateMachines.STEP_STATE_NOT_STARTED);
            case "Complete" -> fromState.equals(WorkflowStateMachines.STATE_IN_PROGRESS);
            case "Approve", "Reject" -> fromState.equals(WorkflowStateMachines.STATE_COMPLETED);
            case "Block" -> fromState.equals(WorkflowStateMachines.STATE_IN_PROGRESS);
            case "Unblock" -> fromState.equals(WorkflowStateMachines.STATE_BLOCKED);
            default -> throw new IllegalArgumentException("Unknown transition: " + transition);
        };
    }

    @Test
    void workflowMachine_exhaustiveStateByTransitionMatrix() {
        for (String state : WORKFLOW_STATES) {
            for (String transition : WORKFLOW_TRANSITIONS) {
                boolean expected = workflowTransitionAllowed(state, transition);
                boolean actual = WORKFLOW_MACHINE.isValidTransition(state, transition, Map.of());
                assertEquals(expected, actual, "workflowMachine: " + state + " --" + transition + "--> ?");
            }
        }
    }

    @Test
    void stepMachine_exhaustiveStateByTransitionMatrix() {
        for (String state : STEP_STATES) {
            for (String transition : STEP_TRANSITIONS) {
                boolean expected = stepTransitionAllowed(state, transition);
                boolean actual = STEP_MACHINE.isValidTransition(state, transition, Map.of());
                assertEquals(expected, actual, "stepMachine: " + state + " --" + transition + "--> ?");
            }
        }
    }

    @Test
    void workflowMachine_holdTargetsOnHoldFromBothSourceStates() {
        assertEquals(WorkflowStateMachines.STATE_ON_HOLD, WORKFLOW_MACHINE
            .getNextState(WorkflowStateMachines.STATE_IN_PROGRESS, WorkflowStateMachines.TRANSITION_HOLD));
        assertEquals(WorkflowStateMachines.STATE_ON_HOLD, WORKFLOW_MACHINE
            .getNextState(WorkflowStateMachines.STATE_BLOCKED, WorkflowStateMachines.TRANSITION_HOLD));
    }

    @Test
    void workflowMachine_cancelTargetsCanceledFromBothSourceStates() {
        assertEquals(WorkflowStateMachines.STATE_CANCELED, WORKFLOW_MACHINE
            .getNextState(WorkflowStateMachines.STATE_PLANNED, WorkflowStateMachines.TRANSITION_CANCEL));
        assertEquals(WorkflowStateMachines.STATE_CANCELED, WORKFLOW_MACHINE
            .getNextState(WorkflowStateMachines.STATE_IN_PROGRESS, WorkflowStateMachines.TRANSITION_CANCEL));
    }

    @Test
    void initialStates_matchEntityDefaults() {
        assertEquals(WorkflowStateMachines.STATE_PLANNED, WORKFLOW_MACHINE.getInitialState());
        assertEquals(WorkflowStateMachines.STEP_STATE_NOT_STARTED, STEP_MACHINE.getInitialState());
    }
}
