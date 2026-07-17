package io.cerp.workflow.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.cerp.workflow.domain.WorkflowTemplate;
import io.cerp.workflow.domain.WorkflowTemplateStep;

public interface WorkflowTemplateRepository extends JpaRepository<WorkflowTemplate, UUID> {

        List<WorkflowTemplate> findByTenantIdAndStatus(String tenantId, WorkflowTemplate.TemplateStatus status);

        List<WorkflowTemplate> findByTenantId(String tenantId);

        @Query("SELECT s FROM WorkflowTemplate t JOIN t.steps s "
                        + "WHERE t.tenantId = :tenantId AND t.status = 'ACTIVE' "
                        + "AND t.code = :templateCode AND s.code = :stepCode")
        Optional<WorkflowTemplateStep> findStepByTemplateAndStepCode(
                        @Param("tenantId") String tenantId,
                        @Param("templateCode") String templateCode,
                        @Param("stepCode") String stepCode);

        @Query("SELECT DISTINCT t FROM WorkflowTemplate t LEFT JOIN FETCH t.steps WHERE t.tenantId = :tenantId ORDER BY t.code")
        List<WorkflowTemplate> findByTenantIdWithSteps(@Param("tenantId") String tenantId);

        boolean existsByTenantIdAndCode(String tenantId, String code);

        Optional<WorkflowTemplate> findByTenantIdAndCodeAndStatus(String tenantId, String code,
                        WorkflowTemplate.TemplateStatus status);
}
