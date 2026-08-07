package io.cerp.workflow.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class StateMachineDefinitionTest {

    private static final String STATE_A = "A";
    private static final String STATE_B = "B";
    private static final String STATE_C = "C";
    private static final String STATE_D = "D";
    private static final String SHARED_CODE = "Advance";

    /**
     * Reproduces the shape of PredefinedStateMachines.purchaseOrderMachine(), where the same
     * transition code is registered from two different source states with two different targets.
     * Before the fix, the second withTransition() call silently overwrote the first in a flat
     * code-keyed map, so this resolved incorrectly regardless of source state.
     */
    private StateMachineDefinition<String, String> ambiguousCodeMachine() {
        return StateMachineDefinition.<String, String>builder()
            .withInitialState(STATE_A)
            .withState(STATE_B)
            .withState(STATE_C)
            .withState(STATE_D)
            .withTransition(STATE_A, SHARED_CODE, STATE_C)
            .withTransition(STATE_B, SHARED_CODE, STATE_D)
            .build();
    }

    @Test
    void getNextState_resolvesPerSourceStateWhenTransitionCodeIsReused() {
        StateMachineDefinition<String, String> machine = ambiguousCodeMachine();

        assertEquals(STATE_C, machine.getNextState(STATE_A, SHARED_CODE));
        assertEquals(STATE_D, machine.getNextState(STATE_B, SHARED_CODE));
    }

    @Test
    void isValidTransition_resolvesPerSourceStateWhenTransitionCodeIsReused() {
        StateMachineDefinition<String, String> machine = ambiguousCodeMachine();

        assertTrue(machine.isValidTransition(STATE_A, SHARED_CODE, Map.of()));
        assertTrue(machine.isValidTransition(STATE_B, SHARED_CODE, Map.of()));
        assertFalse(machine.isValidTransition(STATE_C, SHARED_CODE, Map.of()));
    }

    @Test
    void getNextState_returnsNullForUndefinedTransition() {
        StateMachineDefinition<String, String> machine = ambiguousCodeMachine();

        assertNull(machine.getNextState(STATE_A, "NoSuchTransition"));
        assertNull(machine.getNextState(STATE_C, SHARED_CODE));
    }

    @Test
    void getValidTransitions_onlyListsTransitionsFromGivenState() {
        StateMachineDefinition<String, String> machine = ambiguousCodeMachine();

        Map<String, String> fromA = machine.getValidTransitions(STATE_A);
        assertEquals(1, fromA.size());
        assertEquals(STATE_A + " -> " + STATE_C, fromA.get(SHARED_CODE));

        Map<String, String> fromB = machine.getValidTransitions(STATE_B);
        assertEquals(1, fromB.size());
        assertEquals(STATE_B + " -> " + STATE_D, fromB.get(SHARED_CODE));
    }

    @Test
    void isValidTransition_respectsGuard() {
        StateMachineDefinition<String, String> machine = StateMachineDefinition.<String, String>builder()
            .withInitialState(STATE_A)
            .withTransition(STATE_A, SHARED_CODE, STATE_C, ctx -> Boolean.TRUE.equals(ctx.get("allowed")))
            .build();

        assertTrue(machine.isValidTransition(STATE_A, SHARED_CODE, Map.of("allowed", true)));
        assertFalse(machine.isValidTransition(STATE_A, SHARED_CODE, Map.of("allowed", false)));
    }
}
