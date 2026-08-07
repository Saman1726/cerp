package io.cerp.workflow.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.cerp.workflow.domain.WorkflowStep;
import io.cerp.workflow.repository.WorkflowStepRepository;

class WorkflowStepStateStoreTest {

    private WorkflowStepRepository workflowStepRepository;
    private WorkflowStepStateStore store;

    @BeforeEach
    void setUp() {
        workflowStepRepository = mock(WorkflowStepRepository.class);
        store = new WorkflowStepStateStore(workflowStepRepository);
    }

    private WorkflowStep newStep(WorkflowStep.StepStatus status) {
        WorkflowStep step = new WorkflowStep(UUID.randomUUID(), 1, "STEP-1", "Step One");
        step.setId(UUID.randomUUID());
        step.setStatus(status);
        return step;
    }

    @Test
    void getState_mapsCurrentStatusOntoStateRecord() {
        WorkflowStep step = newStep(WorkflowStep.StepStatus.IN_PROGRESS);
        when(workflowStepRepository.findById(step.getId())).thenReturn(Optional.of(step));

        StateMachineEngine.StateRecord record = store.getState(WorkflowStateMachines.AGGREGATE_WORKFLOW_STEP,
            step.getId());

        assertEquals(step.getId(), record.aggregateId());
        assertEquals(WorkflowStateMachines.AGGREGATE_WORKFLOW_STEP, record.aggregateType());
        assertEquals("IN_PROGRESS", record.state());
    }

    @Test
    void getState_returnsNullWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(workflowStepRepository.findById(id)).thenReturn(Optional.empty());

        assertNull(store.getState(WorkflowStateMachines.AGGREGATE_WORKFLOW_STEP, id));
    }

    @Test
    void saveState_writesNewStatusBackOntoTheEntity() {
        WorkflowStep step = newStep(WorkflowStep.StepStatus.NOT_STARTED);
        when(workflowStepRepository.findById(step.getId())).thenReturn(Optional.of(step));
        when(workflowStepRepository.save(any(WorkflowStep.class))).thenAnswer(inv -> inv.getArgument(0));

        StateMachineEngine.StateRecord record = new StateMachineEngine.StateRecord(
            step.getId(), WorkflowStateMachines.AGGREGATE_WORKFLOW_STEP, "IN_PROGRESS",
            WorkflowStateMachines.TRANSITION_START, Map.of());
        store.saveState(record);

        assertEquals(WorkflowStep.StepStatus.IN_PROGRESS, step.getStatus());
        verify(workflowStepRepository).save(step);
    }

    @Test
    void saveState_throwsWhenStepNotFound() {
        UUID id = UUID.randomUUID();
        when(workflowStepRepository.findById(id)).thenReturn(Optional.empty());
        StateMachineEngine.StateRecord record = new StateMachineEngine.StateRecord(
            id, WorkflowStateMachines.AGGREGATE_WORKFLOW_STEP, "IN_PROGRESS", "Start", Map.of());

        assertThrows(IllegalArgumentException.class, () -> store.saveState(record));
    }
}
