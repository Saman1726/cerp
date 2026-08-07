package io.cerp.workflow.state;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import io.cerp.workflow.domain.WorkflowStep;
import io.cerp.workflow.repository.WorkflowStepRepository;

/**
 * Adapts {@link StateMachineEngine.StateStore} onto the existing
 * {@code workflow.workflow_steps.status} column -- see {@link WorkflowStateStore} for why no
 * separate state table is used.
 */
@Component
public class WorkflowStepStateStore implements StateMachineEngine.StateStore {

    private final WorkflowStepRepository workflowStepRepository;

    public WorkflowStepStateStore(WorkflowStepRepository workflowStepRepository) {
        this.workflowStepRepository = workflowStepRepository;
    }

    @Override
    public StateMachineEngine.StateRecord getState(String aggregateType, UUID aggregateId) {
        return workflowStepRepository.findById(aggregateId)
            .map(step -> new StateMachineEngine.StateRecord(
                aggregateId, aggregateType, step.getStatus().name(), null, Map.of()))
            .orElse(null);
    }

    @Override
    public void saveState(StateMachineEngine.StateRecord record) {
        WorkflowStep step = workflowStepRepository.findById(record.aggregateId())
            .orElseThrow(() -> new IllegalArgumentException("WorkflowStep not found: " + record.aggregateId()));
        step.setStatus(WorkflowStep.StepStatus.valueOf(record.state()));
        workflowStepRepository.save(step);
    }
}
