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

import io.cerp.workflow.domain.Workflow;
import io.cerp.workflow.repository.WorkflowRepository;

class WorkflowStateStoreTest {

    private WorkflowRepository workflowRepository;
    private WorkflowStateStore store;

    @BeforeEach
    void setUp() {
        workflowRepository = mock(WorkflowRepository.class);
        store = new WorkflowStateStore(workflowRepository);
    }

    private Workflow newWorkflow(Workflow.WorkflowStatus status) {
        Workflow workflow = new Workflow("tenant-1", UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), 1, "CODE-1", "Test Workflow");
        workflow.setId(UUID.randomUUID());
        workflow.setStatus(status);
        return workflow;
    }

    @Test
    void getState_mapsCurrentStatusOntoStateRecord() {
        Workflow workflow = newWorkflow(Workflow.WorkflowStatus.IN_PROGRESS);
        when(workflowRepository.findById(workflow.getId())).thenReturn(Optional.of(workflow));

        StateMachineEngine.StateRecord record = store.getState(WorkflowStateMachines.AGGREGATE_WORKFLOW,
            workflow.getId());

        assertEquals(workflow.getId(), record.aggregateId());
        assertEquals(WorkflowStateMachines.AGGREGATE_WORKFLOW, record.aggregateType());
        assertEquals("IN_PROGRESS", record.state());
    }

    @Test
    void getState_returnsNullWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(workflowRepository.findById(id)).thenReturn(Optional.empty());

        assertNull(store.getState(WorkflowStateMachines.AGGREGATE_WORKFLOW, id));
    }

    @Test
    void saveState_writesNewStatusBackOntoTheEntity() {
        Workflow workflow = newWorkflow(Workflow.WorkflowStatus.PLANNED);
        when(workflowRepository.findById(workflow.getId())).thenReturn(Optional.of(workflow));
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(inv -> inv.getArgument(0));

        StateMachineEngine.StateRecord record = new StateMachineEngine.StateRecord(
            workflow.getId(), WorkflowStateMachines.AGGREGATE_WORKFLOW, "IN_PROGRESS",
            WorkflowStateMachines.TRANSITION_START, Map.of());
        store.saveState(record);

        assertEquals(Workflow.WorkflowStatus.IN_PROGRESS, workflow.getStatus());
        verify(workflowRepository).save(workflow);
    }

    @Test
    void saveState_throwsWhenWorkflowNotFound() {
        UUID id = UUID.randomUUID();
        when(workflowRepository.findById(id)).thenReturn(Optional.empty());
        StateMachineEngine.StateRecord record = new StateMachineEngine.StateRecord(
            id, WorkflowStateMachines.AGGREGATE_WORKFLOW, "IN_PROGRESS", "Start", Map.of());

        assertThrows(IllegalArgumentException.class, () -> store.saveState(record));
    }
}
