package io.cerp.organization.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.cerp.organization.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Organization entities.
 *
 * <p>
 * All queries are tenant-scoped to enforce row-level security.
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    /**
     * Find by tenant ID (always used for tenant isolation).
     */
    List<Organization> findByTenantId(String tenantId);

    /**
     * Find by tenant ID and status.
     */
    List<Organization> findByTenantIdAndStatus(String tenantId, Organization.OrganizationStatus status);

    /**
     * Find external ID within a tenant (for conflict detection).
     */
    Optional<Organization> findByTenantIdAndExternalId(String tenantId, String externalId);

    /**
     * Check if an organization with the given name exists in the tenant.
     */
    boolean existsByTenantIdAndNameAndIdNot(String tenantId, String name, UUID excludedId);

    /**
     * Check if an organization with the given name exists in the tenant.
     */
    boolean existsByTenantIdAndName(String tenantId, String name);

    /**
     * Count organizations by status within a tenant.
     */
    long countByTenantIdAndStatus(String tenantId, Organization.OrganizationStatus status);

    /**
     * Find all organizations that need sync (PENDING_SYNC or CONFLICT status).
     */
    @Query("SELECT o FROM Organization o WHERE o.syncStatus IN ('PENDING_SYNC', 'CONFLICT') AND o.tenantId = :tenantId")
    List<Organization> findPendingSyncOrganizations(@Param("tenantId") String tenantId);

    /**
     * Delete all organizations in a tenant (admin operation).
     */
    void deleteByTenantId(String tenantId);
}
