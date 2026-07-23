package io.cerp.workflow.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.cerp.workflow.domain.Tenant;

/**
 * Repository for Tenant entity.
 * 
 * <p>
 * Provides lookup of tenants by their unique code (e.g., 'default', 'acme-corp') and by UUID. Used by TenantService to resolve tenant IDs from incoming requests.
 * </p>
 */
@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Find an active tenant by its unique code.
     * 
     * @param code
     *            the tenant code (e.g., 'default')
     * @return Optional containing the tenant if found and active, empty otherwise
     */
    Optional<Tenant> findByCodeAndActiveIsTrue(String code);

    /**
     * Find a tenant by UUID regardless of active status.
     * 
     * @param id
     *            the tenant UUID
     * @return Optional containing the tenant if found, empty otherwise
     */
    Optional<Tenant> findById(UUID id);
}
