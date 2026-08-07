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
    private final Map<TState, Map<TTransition, Transition<TState, TTransition>>> transitionsByState;

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
        Map<TTransition, Transition<TState, TTransition>> outgoing = transitionsByState.get(currentState);
        if (outgoing == null) {
            return result;
        }
        for (Map.Entry<TTransition, Transition<TState, TTransition>> entry : outgoing.entrySet()) {
            result.put(entry.getKey().toString(),
                currentState.toString() + " -> " + entry.getValue().nextState().toString());
        }
        return result;
    }

    /**
     * Check if a transition is valid from the given state.
     */
    public boolean isValidTransition(TState currentState, TTransition transitionCode,
            Map<String, Object> context) {
        Map<TTransition, Transition<TState, TTransition>> outgoing = transitionsByState.get(currentState);
        if (outgoing == null) {
            return false;
        }

        Transition<TState, TTransition> transition = outgoing.get(transitionCode);
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
        Map<TTransition, Transition<TState, TTransition>> outgoing = transitionsByState.get(currentState);
        if (outgoing == null) {
            return null;
        }
        Transition<TState, TTransition> transition = outgoing.get(transitionCode);
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
        Map<TTransition, Transition<TState, TTransition>> outgoing = transitionsByState.get(currentState);
        if (outgoing == null) {
            return result;
        }
        for (Map.Entry<TTransition, Transition<TState, TTransition>> entry : outgoing.entrySet()) {
            result.put(entry.getKey(), entry.getValue().nextState());
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
        private final Map<TState, Map<TTransition, Transition<TState, TTransition>>> transitionsByState = new HashMap<>();

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

            transitionsByState.computeIfAbsent(fromState, k -> new HashMap<>())
                .put(transitionCode, new Transition<>(transitionCode, toState, guard));

            return this;
        }

        /**
         * Build the state machine definition.
         */
        public StateMachineDefinition<TState, TTransition> build() {
            if (initialState == null) {
                throw new IllegalStateException("Initial state must be set");
            }

            return new StateMachineDefinition<>(initialState, Set.copyOf(states), transitionsByState);
        }
    }

    // --- Private constructor ---

    private StateMachineDefinition(TState initialState, Set<TState> states,
            Map<TState, Map<TTransition, Transition<TState, TTransition>>> transitionsByState) {
        this.initialState = initialState;
        this.states = Set.copyOf(states);
        Map<TState, Map<TTransition, Transition<TState, TTransition>>> immutableCopy = new HashMap<>();
        for (Map.Entry<TState, Map<TTransition, Transition<TState, TTransition>>> entry
                : transitionsByState.entrySet()) {
            immutableCopy.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        this.transitionsByState = Map.copyOf(immutableCopy);
    }
}
