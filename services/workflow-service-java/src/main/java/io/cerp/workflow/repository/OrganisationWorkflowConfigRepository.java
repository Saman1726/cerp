package io.cerp.workflow.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import io.cerp.workflow.domain.OrganisationWorkflowConfig;

public interface OrganisationWorkflowConfigRepository extends JpaRepository<OrganisationWorkflowConfig, UUID> {

    /**
     * Find workflow config for a specific tenant and organization.
     */
    Optional<OrganisationWorkflowConfig> findByTenantIdAndOrgId(String tenantId, UUID orgId);

    /**
     * Check if a workflow config exists for a tenant and organization.
     */
    boolean existsByTenantIdAndOrgId(String tenantId, UUID orgId);

    /**
     * Find all workflow configs for a tenant.
     */
    Optional<OrganisationWorkflowConfig> findByOrgId(UUID orgId);
}
