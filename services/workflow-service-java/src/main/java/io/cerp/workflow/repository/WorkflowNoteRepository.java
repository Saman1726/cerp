package io.cerp.workflow.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.cerp.workflow.domain.WorkflowNote;

public interface WorkflowNoteRepository extends JpaRepository<WorkflowNote, UUID> {

    List<WorkflowNote> findByWorkflowIdOrderByCreatedAtDesc(UUID workflowId);

    List<WorkflowNote> findByWorkflowIdAndStepIdOrderByCreatedAtDesc(UUID workflowId, UUID stepId);
}
