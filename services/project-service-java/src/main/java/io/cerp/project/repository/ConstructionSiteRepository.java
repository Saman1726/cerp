package io.cerp.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.cerp.project.domain.ConstructionSite;

@Repository
public interface ConstructionSiteRepository extends JpaRepository<ConstructionSite, UUID> {

    List<ConstructionSite> findByTenantId(String tenantId);

    /**
     * Counts a creator's Construction Sites for quota enforcement - quota is per-creator, not per-tenant, since many users can share a tenant (see the shared default
     * tenant introduced alongside this).
     */
    long countByCreatedBy(String createdBy);
}
