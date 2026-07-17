package io.cerp.project.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.cerp.project.domain.SubProject;
import io.cerp.project.dto.SubProjectCreateRequest;
import io.cerp.project.dto.SubProjectResponse;
import io.cerp.project.dto.SubProjectUpdateRequest;
import io.cerp.project.service.SubProjectService;
import io.cerp.project.service.ProjectService;

/**
 * REST controller for managing SubProjects (buildings) within Projects.
 *
 * <p>
 * SubProject endpoints follow the pattern: /api/v1/projects/{projectId}/subprojects Each SubProject represents a Building on a Construction Site, with workflows scoped
 * to the whole building, specific floors, or specific building parts.
 */
@RestController
@RequestMapping("/api/v1/projects")
public class SubProjectController {

    private final SubProjectService subProjectService;
    private final ProjectService projectService;

    public SubProjectController(SubProjectService subProjectService, ProjectService projectService) {
        this.subProjectService = subProjectService;
        this.projectService = projectService;
    }

    /**
     * Get all SubProjects for a specific Project.
     */
    @GetMapping("/{projectId}/subprojects")
    public ResponseEntity<List<SubProjectResponse>> getSubProjects(
            @PathVariable("projectId") UUID projectId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        List<SubProjectResponse> subProjects = subProjectService.getSubProjectsByProject(projectId);
        return ResponseEntity.ok(subProjects);
    }

    /**
     * Get all active SubProjects for a specific Project.
     */
    @GetMapping("/{projectId}/subprojects/active")
    public ResponseEntity<List<SubProjectResponse>> getActiveSubProjects(
            @PathVariable("projectId") UUID projectId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        List<SubProjectResponse> subProjects = subProjectService.getActiveSubProjectsByProject(projectId);
        return ResponseEntity.ok(subProjects);
    }

    /**
     * Get a single SubProject by ID.
     */
    @GetMapping("/{projectId}/subprojects/{subProjectId}")
    public ResponseEntity<SubProjectResponse> getSubProject(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("subProjectId") UUID subProjectId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        SubProjectResponse subProject = subProjectService.getSubProjectById(subProjectId);
        return ResponseEntity.ok(subProject);
    }

    /**
     * Get a SubProject by its code within a Project.
     */
    @GetMapping("/{projectId}/subprojects/code/{code}")
    public ResponseEntity<SubProjectResponse> getSubProjectByCode(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("code") String code,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        SubProjectResponse subProject = subProjectService.getSubProjectByCode(tenantId, projectId, code);
        return ResponseEntity.ok(subProject);
    }

    /**
     * Get a SubProject by external ID.
     */
    @GetMapping("/subprojects/external/{externalId}")
    public ResponseEntity<SubProjectResponse> getSubProjectByExternalId(
            @PathVariable("externalId") String externalId,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        SubProjectResponse subProject = subProjectService.getSubProjectByExternalId(externalId);
        return ResponseEntity.ok(subProject);
    }

    /**
     * Get SubProjects filtered by status.
     */
    @GetMapping("/{projectId}/subprojects/status")
    public ResponseEntity<List<SubProjectResponse>> getSubProjectsByStatus(
            @PathVariable("projectId") UUID projectId,
            @RequestParam("status") SubProject.SubProjectStatus status,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        List<SubProjectResponse> subProjects = subProjectService.getSubProjectsByStatus(projectId, status);
        return ResponseEntity.ok(subProjects);
    }

    /**
     * Get SubProjects filtered by building type.
     */
    @GetMapping("/subprojects/by-type")
    public ResponseEntity<List<SubProjectResponse>> getSubProjectsByBuildingType(
            @RequestParam("buildingType") SubProject.BuildingType buildingType,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        List<SubProjectResponse> subProjects = subProjectService.getSubProjectsByBuildingType(tenantId, buildingType);
        return ResponseEntity.ok(subProjects);
    }

    /**
     * Get SubProjects filtered by construction phase.
     */
    @GetMapping("/subprojects/by-phase")
    public ResponseEntity<List<SubProjectResponse>> getSubProjectsByConstructionPhase(
            @RequestParam("constructionPhase") SubProject.ConstructionPhase phase,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        List<SubProjectResponse> subProjects = subProjectService.getSubProjectsByConstructionPhase(tenantId, phase);
        return ResponseEntity.ok(subProjects);
    }

    /**
     * Get SubProjects filtered by building level (scope).
     */
    @GetMapping("/subprojects/by-level")
    public ResponseEntity<List<SubProjectResponse>> getSubProjectsByBuildingLevel(
            @RequestParam("buildingLevel") SubProject.BuildingLevel level,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        List<SubProjectResponse> subProjects = subProjectService.getSubProjectsByBuildingLevel(tenantId, level);
        return ResponseEntity.ok(subProjects);
    }

    /**
     * Create a new SubProject within a Project.
     */
    @PostMapping("/{projectId}/subprojects")
    public ResponseEntity<SubProjectResponse> createSubProject(
            @PathVariable("projectId") UUID projectId,
            @RequestBody SubProjectCreateRequest request,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @RequestHeader(value = "X-Org-Id", required = false) String orgIdHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        UUID orgId = orgIdHeader != null ? UUID.fromString(orgIdHeader) : null;

        if (orgId == null) {
            // Try to get orgId from the Project
            var projectResponse = projectService.findById(tenantId, projectId);
            orgId = projectResponse.orgId();
        }

        SubProjectResponse created = subProjectService.createSubProject(projectId, request, tenantId, orgId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing SubProject.
     */
    @PutMapping("/{projectId}/subprojects/{subProjectId}")
    public ResponseEntity<SubProjectResponse> updateSubProject(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("subProjectId") UUID subProjectId,
            @RequestBody SubProjectUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        SubProjectResponse updated = subProjectService.updateSubProject(subProjectId, request, userId);
        return ResponseEntity.ok(updated);
    }

    // Note: PATCH is handled by PUT as Spring Boot 3.x @PatchMapping may not be available
    // Use PUT for partial updates with null-checking in the service

    /**
     * Delete a SubProject.
     */
    @DeleteMapping("/{projectId}/subprojects/{subProjectId}")
    public ResponseEntity<Void> deleteSubProject(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("subProjectId") UUID subProjectId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        subProjectService.deleteSubProject(subProjectId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete all SubProjects for a given Project.
     */
    @DeleteMapping("/{projectId}/subprojects")
    public ResponseEntity<Void> deleteSubProjectsByProject(
            @PathVariable("projectId") UUID projectId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        subProjectService.deleteSubProjectsByProject(projectId);
        return ResponseEntity.noContent().build();
    }
}
