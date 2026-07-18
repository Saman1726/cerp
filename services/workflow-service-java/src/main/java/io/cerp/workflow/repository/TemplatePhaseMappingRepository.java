package io.cerp.workflow.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.cerp.workflow.domain.TemplatePhaseMapping;
import io.cerp.workflow.domain.TemplatePhaseMapping.BuildingTypeGroup;

public interface TemplatePhaseMappingRepository extends JpaRepository<TemplatePhaseMapping, UUID> {

    /**
     * Find all enabled mappings for a tenant, phase type, and building type group, ordered by priority ascending.
     */
    @Query("SELECT m FROM TemplatePhaseMapping m WHERE m.tenantId = :tenantId "
            + "AND m.phaseType = :phaseType "
            + "AND m.buildingTypeGroup = :buildingTypeGroup "
            + "AND m.enabled = true "
            + "ORDER BY m.priority ASC")
    List<TemplatePhaseMapping> findByTenantIdAndPhaseTypeAndBuildingTypeGroupAndEnabledTrue(
            @Param("tenantId") String tenantId,
            @Param("phaseType") String phaseType,
            @Param("buildingTypeGroup") BuildingTypeGroup buildingTypeGroup);

    /**
     * Find all enabled mappings for a tenant, ordered by priority.
     */
    @Query("SELECT m FROM TemplatePhaseMapping m WHERE m.tenantId = :tenantId AND m.enabled = true ORDER BY m.phaseType, m.buildingTypeGroup, m.priority ASC")
    List<TemplatePhaseMapping> findByTenantIdAndEnabledTrueOrderByPhaseType(@Param("tenantId") String tenantId);

    /**
     * Find a specific mapping by tenant, phase, building type group, and template code.
     */
    Optional<TemplatePhaseMapping> findByTenantIdAndPhaseTypeAndBuildingTypeGroupAndWorkflowTemplateCode(
            String tenantId,
            String phaseType,
            BuildingTypeGroup buildingTypeGroup,
            String workflowTemplateCode);

    /**
     * Check if a specific mapping exists and is enabled.
     */
    boolean existsByTenantIdAndPhaseTypeAndBuildingTypeGroupAndWorkflowTemplateCodeAndEnabledTrue(
            String tenantId,
            String phaseType,
            BuildingTypeGroup buildingTypeGroup,
            String workflowTemplateCode);

    /**
     * Find all distinct phase types for a tenant.
     */
    @Query("SELECT DISTINCT m.phaseType FROM TemplatePhaseMapping m WHERE m.tenantId = :tenantId AND m.enabled = true")
    List<String> findDistinctPhaseTypesByTenantId(@Param("tenantId") String tenantId);

    /**
     * Find all mappings for a specific template code and tenant.
     */
    @Query("SELECT m FROM TemplatePhaseMapping m WHERE m.tenantId = :tenantId AND m.workflowTemplateCode = :templateCode AND m.enabled = true")
    List<TemplatePhaseMapping> findByTenantIdAndWorkflowTemplateCodeAndEnabledTrue(
            @Param("tenantId") String tenantId,
            @Param("templateCode") String templateCode);

    /**
     * Delete all mappings for a tenant and phase type.
     */
    void deleteByTenantIdAndPhaseType(String tenantId, String phaseType);

    /**
     * Count enabled mappings for a tenant.
     */
    long countByTenantIdAndEnabledTrue(String tenantId);
}
