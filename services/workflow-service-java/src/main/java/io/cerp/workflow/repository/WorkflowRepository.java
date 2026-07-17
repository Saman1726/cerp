package io.cerp.workflow.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.cerp.workflow.domain.Workflow;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {

        List<Workflow> findByTenantId(String tenantId);

        List<Workflow> findByTenantIdAndProjectId(String tenantId, UUID projectId);

        List<Workflow> findByTenantIdAndProjectIdAndSubprojectId(String tenantId, UUID projectId, UUID subprojectId);

        List<Workflow> findByTenantIdAndStatus(String tenantId, Workflow.WorkflowStatus status);

        List<Workflow> findByTenantIdAndProjectIdAndSubprojectIdAndStatus(String tenantId, UUID projectId,
                        UUID subprojectId, Workflow.WorkflowStatus status);

        @Query("SELECT DISTINCT w FROM Workflow w LEFT JOIN FETCH w.steps WHERE w.tenantId = :tenantId ORDER BY w.code")
        List<Workflow> findByTenantIdWithSteps(@Param("tenantId") String tenantId);

        List<Workflow> findByTenantIdAndStatusAndProgressPercentLessThan(String tenantId,
                        Workflow.WorkflowStatus status, BigDecimal progressThreshold);

        @Query("SELECT w FROM Workflow w LEFT JOIN FETCH w.steps WHERE w.id = :id")
        Optional<Workflow> findByIdWithSteps(@Param("id") UUID id);

        @Query("SELECT w FROM Workflow w LEFT JOIN FETCH w.steps "
                        + "WHERE w.tenantId = :tenantId AND w.projectId = :projectId AND w.subprojectId = :subprojectId")
        Optional<Workflow> findBySubprojectIdWithSteps(
                        @Param("tenantId") String tenantId,
                        @Param("projectId") UUID projectId,
                        @Param("subprojectId") UUID subprojectId);

        @Query("SELECT DISTINCT w.tenantId FROM Workflow w")
        List<String> findDistinctTenantIds();

        @Query("SELECT w.code FROM Workflow w WHERE w.subprojectId = :subprojectId AND w.templateId = :templateId")
        Optional<String> findExistingWorkflowCode(
                        @Param("subprojectId") UUID subprojectId,
                        @Param("templateId") UUID templateId);
}
