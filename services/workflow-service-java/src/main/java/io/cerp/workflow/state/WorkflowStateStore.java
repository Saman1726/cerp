package io.cerp.workflow.state;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import io.cerp.workflow.domain.Workflow;
import io.cerp.workflow.repository.WorkflowRepository;

/**
 * Adapts {@link StateMachineEngine.StateStore} onto the existing {@code workflow.workflows.status}
 * column -- no separate state table. That column is already the indexed, CHECK-constrained source
 * of truth, so a parallel state table would just create a second one to desync from.
 */
@Component
public class WorkflowStateStore implements StateMachineEngine.StateStore {

    private final WorkflowRepository workflowRepository;

    public WorkflowStateStore(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @Override
    public StateMachineEngine.StateRecord getState(String aggregateType, UUID aggregateId) {
        return workflowRepository.findById(aggregateId)
            .map(workflow -> new StateMachineEngine.StateRecord(
                aggregateId, aggregateType, workflow.getStatus().name(), null, Map.of()))
            .orElse(null);
    }

    @Override
    public void saveState(StateMachineEngine.StateRecord record) {
        Workflow workflow = workflowRepository.findById(record.aggregateId())
            .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + record.aggregateId()));
        workflow.setStatus(Workflow.WorkflowStatus.valueOf(record.state()));
        workflowRepository.save(workflow);
    }
}
