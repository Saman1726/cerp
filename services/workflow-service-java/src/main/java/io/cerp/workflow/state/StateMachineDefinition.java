package io.cerp.workflow.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Immutable definition of a state machine for a workflow aggregate.
 *
 * <p>
 * A state machine consists of:
 * <ul>
 * <li>A set of valid states</li>
 * <li>An initial state</li>
 * <li>Transitions between states, each with optional guard conditions</li>
 * </ul>
 *
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * var smd = StateMachineDefinition.<String, String> builder()
 *     .withInitialState("DRAFT")
 *     .withState("DRAFT")
 *     .withState("PENDING_APPROVAL")
 *     .withState("APPROVED")
 *     .withState("REJECTED")
 *     .withState("CANCELLED")
 *     .withTransition("DRAFT", "SUBMIT", "PENDING_APPROVAL")
 *     .withTransition("PENDING_APPROVAL", "APPROVE", "APPROVED")
 *     .withTransition("PENDING_APPROVAL", "REJECT", "REJECTED")
 *     .withTransition("DRAFT", "CANCEL", "CANCELLED")
 *     .withTransition("PENDING_APPROVAL", "CANCEL", "CANCELLED")
 *     .withTransition("APPROVED", "CANCEL", "CANCELLED")
 *     .build();
 * }</pre>
 */
public class StateMachineDefinition<TState, TTransition> {

    private final TState initialState;
    private final Set<TState> states;
    private final Map<TTransition, Transition<TState, TTransition>> transitions;
    private final Map<TState, Set<TTransition>> outgoingTransitions;

    /**
     * Get the initial state of this state machine.
     */
    public TState getInitialState() {
        return initialState;
    }

    /**
     * Get all defined states.
     */
    public Set<TState> getStates() {
        return Set.copyOf(states);
    }

    /**
     * Get all valid transitions from a given state.
     */
    public Map<String, String> getValidTransitions(TState currentState) {
        Map<String, String> result = new HashMap<>();
        Set<TTransition> outgoing = outgoingTransitions.get(currentState);
        if (outgoing == null) {
            return result;
        }
        for (TTransition transition : outgoing) {
            Transition<TState, TTransition> t = transitions.get(transition);
            if (t != null) {
                result.put(transition.toString(), currentState.toString() + " -> " + t.nextState().toString());
            }
        }
        return result;
    }

    /**
     * Check if a transition is valid from the given state.
     */
    public boolean isValidTransition(TState currentState, TTransition transitionCode,
            Map<String, Object> context) {
        Set<TTransition> outgoing = outgoingTransitions.get(currentState);
        if (outgoing == null || !outgoing.contains(transitionCode)) {
            return false;
        }

        Transition<TState, TTransition> transition = transitions.get(transitionCode);
        if (transition == null) {
            return false;
        }

        // Check guard condition if present
        Function<Map<String, Object>, Boolean> guard = transition.guard();
        if (guard != null && context != null) {
            return guard.apply(context);
        }
        return true;
    }

    /**
     * Get the next state for a given current state and transition.
     *
     * @return the next state, or null if the transition is not valid
     */
    public TState getNextState(TState currentState, TTransition transitionCode) {
        Transition<TState, TTransition> transition = transitions.get(transitionCode);
        if (transition == null) {
            return null;
        }
        return transition.nextState();
    }

    /**
     * Get all possible next states from a given state.
     */
    public Map<TTransition, TState> getNextStates(TState currentState) {
        Map<TTransition, TState> result = new HashMap<>();
        Set<TTransition> outgoing = outgoingTransitions.get(currentState);
        if (outgoing == null) {
            return result;
        }
        for (TTransition transition : outgoing) {
            Transition<TState, TTransition> t = transitions.get(transition);
            if (t != null) {
                result.put(transition, t.nextState());
            }
        }
        return result;
    }

    // --- Transition record ---

    /**
     * Represents a single state transition.
     */
    public record Transition<TState, TTransition>(
            TTransition code,
            TState nextState,
            Function<Map<String, Object>, Boolean> guard) {
    }

    // --- Builder ---

    /**
     * Create a new state machine builder.
     */
    public static <TState, TTransition> StateMachineBuilder<TState, TTransition> builder() {
        return new StateMachineBuilder<>();
    }

    /**
     * Builder for StateMachineDefinition.
     */
    public static class StateMachineBuilder<TState, TTransition> {

        private TState initialState;
        private final Set<TState> states = new HashSet<>();
        private final Map<TTransition, Transition<TState, TTransition>> transitions = new HashMap<>();
        private final Map<TState, Set<TTransition>> outgoingTransitions = new HashMap<>();

        private StateMachineBuilder() {
        }

        /**
         * Set the initial state.
         */
        public StateMachineBuilder<TState, TTransition> withInitialState(TState initialState) {
            this.initialState = initialState;
            states.add(initialState);
            return this;
        }

        /**
         * Add a state to the state machine.
         */
        public StateMachineBuilder<TState, TTransition> withState(TState state) {
            states.add(state);
            return this;
        }

        /**
         * Add a transition from one state to another.
         */
        public StateMachineBuilder<TState, TTransition> withTransition(
                TState fromState, TTransition transitionCode, TState toState) {
            return withTransition(fromState, transitionCode, toState, null);
        }

        /**
         * Add a transition with a guard condition.
         */
        public StateMachineBuilder<TState, TTransition> withTransition(
                TState fromState, TTransition transitionCode, TState toState,
                Function<Map<String, Object>, Boolean> guard) {
            states.add(fromState);
            states.add(toState);

            transitions.put(transitionCode, new Transition<>(transitionCode, toState, guard));

            outgoingTransitions.computeIfAbsent(fromState, k -> new HashSet<>()).add(transitionCode);

            return this;
        }

        /**
         * Build the state machine definition.
         */
        public StateMachineDefinition<TState, TTransition> build() {
            if (initialState == null) {
                throw new IllegalStateException("Initial state must be set");
            }

            return new StateMachineDefinition<>(initialState, Set.copyOf(states),
                transitions, outgoingTransitions);
        }
    }

    // --- Private constructor ---

    private StateMachineDefinition(TState initialState, Set<TState> states,
            Map<TTransition, Transition<TState, TTransition>> transitions,
            Map<TState, Set<TTransition>> outgoingTransitions) {
        this.initialState = initialState;
        this.states = Set.copyOf(states);
        this.transitions = Map.copyOf(transitions);
        this.outgoingTransitions = Map.copyOf(outgoingTransitions);
    }
}
