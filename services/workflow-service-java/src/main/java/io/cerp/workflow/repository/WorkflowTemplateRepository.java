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

        /**
         * Find active templates ordered by their construction phase order number.
         */
        @Query("SELECT t FROM WorkflowTemplate t WHERE t.tenantId = :tenantId AND t.status = 'ACTIVE' ORDER BY COALESCE(t.orderNum, 9999), t.code")
        List<WorkflowTemplate> findByTenantIdAndStatusOrderByOrderNum(@Param("tenantId") String tenantId);

        /**
         * Find all templates ordered by their construction phase order number.
         */
        @Query("SELECT t FROM WorkflowTemplate t WHERE t.tenantId = :tenantId ORDER BY COALESCE(t.orderNum, 9999), t.code")
        List<WorkflowTemplate> findByTenantIdOrderByOrderNum(@Param("tenantId") String tenantId);

        @Query("SELECT s FROM WorkflowTemplate t JOIN t.steps s "
                        + "WHERE t.tenantId = :tenantId AND t.status = 'ACTIVE' "
                        + "AND t.code = :templateCode AND s.code = :stepCode")
        Optional<WorkflowTemplateStep> findStepByTemplateAndStepCode(
                        @Param("tenantId") String tenantId,
                        @Param("templateCode") String templateCode,
                        @Param("stepCode") String stepCode);

        @Query("SELECT DISTINCT t FROM WorkflowTemplate t LEFT JOIN FETCH t.steps WHERE t.tenantId = :tenantId ORDER BY COALESCE(t.orderNum, 9999), t.code")
        List<WorkflowTemplate> findByTenantIdWithSteps(@Param("tenantId") String tenantId);

        /**
         * Find templates by phase type ordered by construction phase order.
         */
        List<WorkflowTemplate> findByTenantIdAndStatusAndPhaseTypeOrderByOrderNum(String tenantId, WorkflowTemplate.TemplateStatus status, String phaseType);

        boolean existsByTenantIdAndCode(String tenantId, String code);

        boolean existsByCode(String code);

        Optional<WorkflowTemplate> findByTenantIdAndCode(String tenantId, String code);

        Optional<WorkflowTemplate> findByCode(String code);

        Optional<WorkflowTemplate> findByTenantIdAndCodeAndStatus(String tenantId, String code,
                        WorkflowTemplate.TemplateStatus status);
}
