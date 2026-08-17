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
import org.springframework.transaction.annotation.Transactional;

import io.cerp.workflow.domain.Workflow;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {

        List<Workflow> findByTenantId(String tenantId);

        List<Workflow> findByTenantIdAndProjectIdAndDeletedFalse(String tenantId, UUID projectId);

        List<Workflow> findByTenantIdAndProjectIdAndSubprojectIdAndDeletedFalse(String tenantId, UUID projectId, UUID subprojectId);

        List<Workflow> findByTenantIdAndSubprojectIdAndDeletedFalse(String tenantId, UUID subprojectId);

        List<Workflow> findByTenantIdAndStatus(String tenantId, Workflow.WorkflowStatus status);

        List<Workflow> findByTenantIdAndProjectIdAndSubprojectIdAndStatus(String tenantId, UUID projectId,
                        UUID subprojectId, Workflow.WorkflowStatus status);

        @Query("SELECT DISTINCT w FROM Workflow w LEFT JOIN FETCH w.steps WHERE w.tenantId = :tenantId AND w.deleted = false ORDER BY w.code")
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

        /**
         * Check if a workflow already exists for the given tenant, project, subproject, and template.
         */
        boolean existsByTenantIdAndProjectIdAndSubprojectIdAndTemplateId(
                        @Param("tenantId") String tenantId,
                        @Param("projectId") UUID projectId,
                        @Param("subprojectId") UUID subprojectId,
                        @Param("templateId") UUID templateId);

        /**
         * Count workflows for a tenant and subproject.
         */
        long countByTenantIdAndSubprojectId(
                        @Param("tenantId") String tenantId,
                        @Param("subprojectId") UUID subprojectId);

        /**
         * Find all workflow IDs for a tenant and subproject.
         */
        @Query("SELECT w.id FROM Workflow w WHERE w.tenantId = :tenantId AND w.subprojectId = :subprojectId")
        List<UUID> findIdsByTenantIdAndSubprojectId(
                        @Param("tenantId") String tenantId,
                        @Param("subprojectId") UUID subprojectId);

        /**
         * Find all workflows for a subproject regardless of tenant. Used as a fallback when TenantContext is empty (e.g., development, migration).
         */
        @Query("SELECT w FROM Workflow w WHERE w.subprojectId = :subprojectId AND w.deleted = false")
        List<Workflow> findBySubprojectIdOnly(@Param("subprojectId") UUID subprojectId);

        /**
         * Archive every Workflow tied to a Project - called when the Project itself is deleted.
         * Does not touch WorkflowSteps, evidence, or approval history; only hides the Workflow
         * rows from default listings.
         */
        @Modifying
        @Transactional
        @Query("UPDATE Workflow w SET w.deleted = true WHERE w.projectId = :projectId")
        int archiveByProjectId(@Param("projectId") UUID projectId);

        /**
         * Find workflows created by a specific user (non-deleted).
         */
        List<Workflow> findByCreatedBy(String createdBy);

        /**
         * Find workflows created by a specific user within a tenant.
         */
        List<Workflow> findByTenantIdAndCreatedBy(String tenantId, String createdBy);

        /**
         * Find all workflows within a tenant where user is creator or has an assigned step.
         */
        @Query("SELECT DISTINCT w FROM Workflow w JOIN w.steps s WHERE " +
                "((w.createdBy = :createdBy) OR (s.assigneeUserId = :assigneeUserId)) " +
                "AND w.tenantId = :tenantId AND w.deleted = false")
        List<Workflow> findByUserAccessTenantScoped(
                @Param("tenantId") String tenantId,
                @Param("createdBy") String createdBy,
                @Param("assigneeUserId") UUID assigneeUserId);
}
