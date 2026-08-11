package io.cerp.workflow.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.cerp.workflow.domain.WorkflowStepAttachment;

public interface WorkflowStepAttachmentRepository extends JpaRepository<WorkflowStepAttachment, UUID> {

    List<WorkflowStepAttachment> findByWorkflowIdOrderByCreatedAtDesc(UUID workflowId);

    List<WorkflowStepAttachment> findByWorkflowIdAndStepIdOrderByCreatedAtDesc(UUID workflowId, UUID stepId);

    Optional<WorkflowStepAttachment> findByIdAndWorkflowIdAndStepId(UUID id, UUID workflowId, UUID stepId);
}
