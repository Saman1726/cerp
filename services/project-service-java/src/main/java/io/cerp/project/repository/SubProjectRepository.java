package io.cerp.project.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.cerp.project.domain.SubProject;

/**
 * Repository for SubProject entity operations.
 *
 * <p>
 * Provides CRUD and query operations for SubProjects (buildings) within Projects. All queries are scoped by tenant_id for multi-tenant isolation.
 */
@Repository
public interface SubProjectRepository extends JpaRepository<SubProject, UUID> {

    /**
     * Find all non-deleted SubProjects for a specific Project.
     */
    List<SubProject> findByProjectIdAndDeletedFalseOrderByCodeAsc(UUID projectId);

    /**
     * Find all active (non-completed, non-cancelled, non-deleted) SubProjects for a Project.
     */
    @Query("SELECT s FROM SubProject s WHERE s.projectId = :projectId AND s.deleted = false AND s.status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY s.code ASC")
    List<SubProject> findActiveByProjectId(@Param("projectId") UUID projectId);

    /**
     * Find SubProject by its unique code within a tenant and project.
     */
    Optional<SubProject> findByTenantIdAndProjectIdAndCode(String tenantId, UUID projectId, String code);

    /**
     * Find SubProject by external ID.
     */
    Optional<SubProject> findByExternalId(String externalId);

    /**
     * Find SubProjects by status.
     */
    List<SubProject> findByTenantIdAndStatus(String tenantId, SubProject.SubProjectStatus status);

    /**
     * Find SubProjects by building type.
     */
    @Query("SELECT s FROM SubProject s WHERE s.tenantId = :tenantId AND s.buildingType = :buildingType ORDER BY s.name ASC")
    List<SubProject> findByBuildingType(@Param("tenantId") String tenantId, @Param("buildingType") SubProject.BuildingType buildingType);

    /**
     * Find SubProjects by construction phase.
     */
    @Query("SELECT s FROM SubProject s WHERE s.tenantId = :tenantId AND s.constructionPhase = :phase ORDER BY s.name ASC")
    List<SubProject> findByConstructionPhase(@Param("tenantId") String tenantId, @Param("phase") SubProject.ConstructionPhase phase);

    /**
     * Find SubProjects by building level.
     */
    @Query("SELECT s FROM SubProject s WHERE s.tenantId = :tenantId AND s.buildingLevel = :level ORDER BY s.name ASC")
    List<SubProject> findByBuildingLevel(@Param("tenantId") String tenantId, @Param("level") SubProject.BuildingLevel level);

    /**
     * Count SubProjects by status for a project.
     */
    long countByTenantIdAndProjectIdAndStatus(String tenantId, UUID projectId, SubProject.SubProjectStatus status);

    /**
     * Find all SubProjects for an organization.
     */
    @Query("SELECT s FROM SubProject s WHERE s.orgId = :orgId ORDER BY s.projectId, s.code ASC")
    List<SubProject> findByOrgIdOrderByProjectAndCode(@Param("orgId") UUID orgId);

    /**
     * Check if a code is already in use within a project.
     */
    boolean existsByTenantIdAndProjectIdAndCode(String tenantId, UUID projectId, String code);

    /**
     * Check if a Building already has a SubProject linked to it (enforces the 1:1 relationship at the application level, ahead of the DB unique index).
     */
    boolean existsByBuildingId(UUID buildingId);

    /**
     * Archive (soft-delete) all SubProjects for a given Project - used when the Project itself is
     * deleted. A bulk update, not a load-each-then-save loop, so it stays cheap regardless of how
     * many SubProjects the Project has; the tradeoff (per Spring Data JPA bulk updates) is that
     * updatedAt/@PreUpdate doesn't fire - acceptable here since this replaces what used to be an
     * unconditional hard delete via deleteByProjectId, which had the same limitation.
     */
    @Modifying
    @Transactional
    @Query("UPDATE SubProject s SET s.deleted = true, s.status = :status WHERE s.projectId = :projectId")
    int archiveByProjectId(@Param("projectId") UUID projectId, @Param("status") SubProject.SubProjectStatus status);
}
