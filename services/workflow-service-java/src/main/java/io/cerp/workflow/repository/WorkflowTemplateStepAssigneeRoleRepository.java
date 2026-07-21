package io.cerp.workflow.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.cerp.workflow.domain.WorkflowTemplateStepAssigneeRole;

/**
 * Repository for WorkflowTemplateStepAssigneeRole entities.
 */
@Repository
public interface WorkflowTemplateStepAssigneeRoleRepository extends JpaRepository<WorkflowTemplateStepAssigneeRole, UUID> {

    /**
     * Find all assignee roles for a given template step, ordered by roleOrder.
     */
    List<WorkflowTemplateStepAssigneeRole> findByTemplateStepIdOrderByRoleOrderAsc(UUID templateStepId);

    /**
     * Find all assignee roles for a given template step ID.
     */
    List<WorkflowTemplateStepAssigneeRole> findByTemplateStepId(UUID templateStepId);

    /**
     * Delete all assignee roles for a given template step.
     */
    void deleteByTemplateStepId(UUID templateStepId);

    /**
     * Check if a template step has any assignee roles.
     */
    boolean existsByTemplateStepId(UUID templateStepId);

    /**
     * Find the primary assignee role for a template step.
     */
    Optional<WorkflowTemplateStepAssigneeRole> findFirstByTemplateStepIdAndIsPrimaryTrueOrderByRoleOrderAsc(UUID templateStepId);

    /**
     * Find all distinct role names for a template step.
     */
    List<String> findRoleNameByTemplateStepId(UUID templateStepId);
}
