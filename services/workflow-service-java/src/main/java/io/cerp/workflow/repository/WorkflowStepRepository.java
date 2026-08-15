package io.cerp.workflow.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.cerp.workflow.domain.WorkflowStep;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, UUID> {

    List<WorkflowStep> findByWorkflowIdOrderByStepOrder(UUID workflowId);

    @Query("SELECT s FROM WorkflowStep s WHERE s.workflow.id = :workflowId AND s.status = 'NOT_STARTED' "
            + "ORDER BY s.stepOrder ASC LIMIT 1")
    Optional<WorkflowStep> findFirstNotStartedByWorkflowId(@Param("workflowId") UUID workflowId);

    List<WorkflowStep> findByWorkflowIdAndStatus(UUID workflowId, WorkflowStep.StepStatus status);

    long countByWorkflowId(UUID workflowId);

    long countByWorkflowIdAndStatus(UUID workflowId, WorkflowStep.StepStatus status);

    /**
     * Find all steps assigned to a specific user.
     */
    List<WorkflowStep> findByAssigneeUserId(UUID assigneeUserId);

    /**
     * Find all steps assigned to a specific user, ordered by workflow and step order.
     */
    @Query("SELECT s FROM WorkflowStep s WHERE s.assigneeUserId = :assigneeUserId AND s.workflow.deleted = false ORDER BY s.workflow.id, s.stepOrder")
    List<WorkflowStep> findByAssigneeUserIdWithWorkflow(@Param("assigneeUserId") UUID assigneeUserId);
}
