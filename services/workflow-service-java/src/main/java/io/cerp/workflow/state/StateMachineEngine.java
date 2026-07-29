package io.cerp.workflow.state;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * State machine engine for managing aggregate state transitions.
 *
 * <p>
 * Each workflow aggregate (ProcurementRequest, PurchaseOrder, Delivery, etc.) has its own state machine that enforces valid state transitions and validates business
 * invariants before allowing state changes.
 *
 * <p>
 * State machines are defined using the {@link StateMachineDefinition} class, which specifies states, transitions, and guard conditions.
 *
 * @see StateMachineDefinition
 * @see StateTransition
 */
public class StateMachineEngine {

    private final Map<String, StateMachineDefinition<String, String>> stateMachines;
    private final Map<String, Map<String, TransitionState>> transitionTable;

    public StateMachineEngine() {
        this.stateMachines = Map.of();
        this.transitionTable = Map.of();
    }

    public StateMachineEngine(Map<String, StateMachineDefinition<String, String>> stateMachines) {
        this.stateMachines = new LinkedHashMap<>(stateMachines);
        this.transitionTable = buildTransitionTable(stateMachines);
    }

    /**
     * Build the transition table from state machine definitions.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, TransitionState>> buildTransitionTable(
            Map<String, StateMachineDefinition<String, String>> stateMachines) {
        Map<String, Map<String, TransitionState>> table = new HashMap<>();
        for (Map.Entry<String, StateMachineDefinition<String, String>> entry : stateMachines.entrySet()) {
            String machineName = entry.getKey();
            StateMachineDefinition<String, String> def = entry.getValue();
            Map<String, TransitionState> machineTable = new HashMap<>();
            for (String stateName : def.getStates()) {
                Map<String, String> validTransitions = def.getValidTransitions(stateName);
                machineTable.put(stateName, new TransitionState(stateName, validTransitions));
            }
            table.put(machineName, Map.copyOf(machineTable));
        }
        return Map.copyOf(table);
    }

    /**
     * Create a state machine engine with a single state machine definition.
     */
    public static StateMachineEngine create(String machineName, StateMachineDefinition<String, String> definition) {
        var machines = Map.<String, StateMachineDefinition<String, String>> of(machineName, definition);
        return new StateMachineEngine(machines);
    }

    /**
     * Get the current state of an aggregate.
     */
    public StateRecord getCurrentState(String aggregateType, UUID aggregateId, StateStore store) {
        return store.getState(aggregateType, aggregateId);
    }

    /**
     * Get the transition table for all state machines.
     */
    public Map<String, Map<String, TransitionState>> getTransitionTable() {
        return transitionTable;
    }

    /**
     * Check if a transition is valid for the given aggregate and transition.
     */
    @SuppressWarnings("unchecked")
    public boolean canTransition(String aggregateType, UUID aggregateId, String transitionCode,
            StateStore store, Map<String, Object> context) {
        StateRecord current = store.getState(aggregateType, aggregateId);
        if (current == null) {
            return false;
        }

        StateMachineDefinition<String, String> machine = stateMachines.get(aggregateType);
        if (machine == null) {
            return false;
        }

        return machine.isValidTransition(current.state(), transitionCode, context);
    }

    /**
     * Execute a state transition for an aggregate.
     *
     * @return the updated state record
     * @throws StateTransitionException
     *             if the transition is invalid
     */
    @SuppressWarnings("unchecked")
    public StateRecord executeTransition(String aggregateType, UUID aggregateId, String transitionCode,
            StateStore store, Map<String, Object> context) {
        StateMachineDefinition<String, String> machine = stateMachines.get(aggregateType);
        if (machine == null) {
            throw new StateTransitionException("No state machine found for aggregate type: " + aggregateType);
        }

        StateRecord current = store.getState(aggregateType, aggregateId);
        String nextState;

        if (current == null) {
            // First state - use the initial state
            nextState = machine.getInitialState();
        } else {
            // Validate and get next state
            if (!machine.isValidTransition(current.state(), transitionCode, context)) {
                throw new StateTransitionException(
                    String.format("Invalid transition from '%s' with '%s'", current.state(), transitionCode));
            }
            nextState = machine.getNextState(current.state(), transitionCode);
        }

        // Update store
        StateRecord nextRecord;
        if (current == null) {
            nextRecord = new StateRecord(aggregateId, aggregateType, nextState, transitionCode, context);
        } else {
            nextRecord = current.withNewState(nextState, transitionCode);
        }

        store.saveState(nextRecord);

        return nextRecord;
    }

    /**
     * Get all valid transitions for the current state of an aggregate.
     */
    @SuppressWarnings("unchecked")
    public TransitionsInfo getValidTransitions(String aggregateType, UUID aggregateId,
            StateStore store) {
        StateMachineDefinition<String, String> machine = stateMachines.get(aggregateType);
        if (machine == null) {
            return new TransitionsInfo(aggregateId, aggregateType, Map.of());
        }

        StateRecord current = store.getState(aggregateType, aggregateId);
        if (current == null) {
            return new TransitionsInfo(aggregateId, aggregateType,
                Map.of("INITIALIZE", machine.getInitialState()));
        }

        return new TransitionsInfo(aggregateId, aggregateType, machine.getValidTransitions(current.state()));
    }

    /**
     * Get the list of all states for an aggregate type.
     */
    public java.util.List<String> getStates(String aggregateType) {
        StateMachineDefinition<String, String> machine = stateMachines.get(aggregateType);
        if (machine == null) {
            return java.util.List.of();
        }
        return java.util.List.copyOf(machine.getStates());
    }

    // --- Inner classes ---

    /**
     * Record of current state for an aggregate.
     */
    public record StateRecord(UUID aggregateId, String aggregateType, String state, String lastTransition,
            Map<String, Object> context, Instant lastUpdated) {

        public StateRecord {
            if (aggregateId == null) {
                throw new IllegalArgumentException("aggregateId cannot be null");
            }
            if (aggregateType == null) {
                throw new IllegalArgumentException("aggregateType cannot be null");
            }
            if (state == null) {
                throw new IllegalArgumentException("state cannot be null");
            }
        }

        public StateRecord(UUID aggregateId, String aggregateType, String state, String lastTransition,
                Map<String, Object> context) {
            this(aggregateId, aggregateType, state, lastTransition, context, Instant.now());
        }

        public StateRecord withNewState(String newState, String transitionCode) {
            return new StateRecord(aggregateId, aggregateType, newState, transitionCode, context, Instant.now());
        }
    }

    /**
     * Interface for state persistence.
     */
    public interface StateStore {
        StateRecord getState(String aggregateType, UUID aggregateId);

        void saveState(StateRecord record);
    }

    /**
     * Valid transitions info for an aggregate.
     */
    public record TransitionsInfo(UUID aggregateId, String aggregateType, Map<String, String> validTransitions) {
    }

    /**
     * Record representing a state and its valid transitions in the transition table.
     */
    public record TransitionState(String state, Map<String, String> validTransitions) {
    }

    /**
     * Exception thrown when a state transition is invalid.
     */
    public static class StateTransitionException extends RuntimeException {
        public StateTransitionException(String message) {
            super(message);
        }

        public StateTransitionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
