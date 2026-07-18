package io.cerp.project.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.cerp.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Project entities.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("SELECT p FROM Project p WHERE p.tenantId = :tenantId AND p.deleted = false")
    List<Project> findByTenantIdAndNotDeleted(@Param("tenantId") String tenantId);

    @Query("SELECT p FROM Project p WHERE p.tenantId = :tenantId AND p.deleted = false")
    List<Project> findByTenantId(@Param("tenantId") String tenantId);

    List<Project> findByTenantIdAndOrgId(String tenantId, UUID orgId);

    List<Project> findByTenantIdAndStatus(String tenantId, Project.ProjectStatus status);

    List<Project> findByTenantIdAndOrgIdAndStatus(String tenantId, UUID orgId, Project.ProjectStatus status);

    Optional<Project> findByTenantIdAndCode(String tenantId, String code);

    Optional<Project> findByTenantIdAndExternalId(String tenantId, String externalId);

    boolean existsByTenantIdAndCodeAndIdNot(String tenantId, String code, UUID excludedId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Project p WHERE p.tenantId = :tenantId AND p.code = :code AND p.deleted = false")
    boolean existsByTenantIdAndCodeNotDeleted(@Param("tenantId") String tenantId, @Param("code") String code);

    boolean existsByTenantIdAndCode(String tenantId, String code);

    Optional<Project> findByConstructionSiteIdAndDeletedFalse(String constructionSiteId);

    long countByTenantIdAndStatus(String tenantId, Project.ProjectStatus status);

    @Query("SELECT p FROM Project p WHERE p.syncStatus IN ('PENDING_SYNC', 'CONFLICT') AND p.tenantId = :tenantId")
    List<Project> findPendingSyncProjects(@Param("tenantId") String tenantId);

    long countByTenantId(String tenantId);

    void deleteByTenantId(String tenantId);
}
