package io.cerp.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.cerp.project.domain.ConstructionSite;

@Repository
public interface ConstructionSiteRepository extends JpaRepository<ConstructionSite, UUID> {

    List<ConstructionSite> findByTenantId(String tenantId);
}
