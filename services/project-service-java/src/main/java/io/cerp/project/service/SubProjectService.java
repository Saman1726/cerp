package io.cerp.project.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cerp.project.domain.SubProject;
import io.cerp.project.dto.SubProjectCreateRequest;
import io.cerp.project.dto.SubProjectResponse;
import io.cerp.project.dto.SubProjectUpdateRequest;
import io.cerp.project.repository.SubProjectRepository;

/**
 * Service for managing SubProjects (buildings) within Projects.
 *
 * <p>
 * SubProjects represent buildings or major building components within a construction Project. Each SubProject can have workflows scoped to the whole building, specific
 * floors, or specific building parts.
 */
@Service
@Transactional(readOnly = true)
public class SubProjectService {

    private static final Logger log = LoggerFactory.getLogger(SubProjectService.class);

    private final SubProjectRepository subProjectRepository;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SubProjectService(SubProjectRepository subProjectRepository,
            org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate) {
        this.subProjectRepository = subProjectRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Get all SubProjects for a specific Project.
     */
    public List<SubProjectResponse> getSubProjectsByProject(UUID projectId) {
        log.debug("Fetching all SubProjects for Project: {}", projectId);
        return subProjectRepository.findByProjectIdOrderByCodeAsc(projectId)
            .stream()
            .map(SubProjectResponse::fromEntity)
            .toList();
    }

    /**
     * Get all active SubProjects for a specific Project.
     */
    public List<SubProjectResponse> getActiveSubProjectsByProject(UUID projectId) {
        log.debug("Fetching active SubProjects for Project: {}", projectId);
        return subProjectRepository.findActiveByProjectId(projectId)
            .stream()
            .map(SubProjectResponse::fromEntity)
            .toList();
    }

    /**
     * Get a single SubProject by ID.
     */
    public SubProjectResponse getSubProjectById(UUID subProjectId) {
        log.debug("Fetching SubProject by ID: {}", subProjectId);
        SubProject subProject = subProjectRepository.findById(subProjectId)
            .orElseThrow(() -> new IllegalArgumentException("SubProject not found with id: " + subProjectId));
        return SubProjectResponse.fromEntity(subProject);
    }

    /**
     * Get a SubProject by its code within a Project.
     */
    public SubProjectResponse getSubProjectByCode(String tenantId, UUID projectId, String code) {
        log.debug("Fetching SubProject by code: {} for Project: {}", code, projectId);
        SubProject subProject = subProjectRepository.findByTenantIdAndProjectIdAndCode(tenantId, projectId, code)
            .orElseThrow(() -> new IllegalArgumentException(
                "SubProject not found with code: " + code + " for Project: " + projectId));
        return SubProjectResponse.fromEntity(subProject);
    }

    /**
     * Get a SubProject by external ID.
     */
    public SubProjectResponse getSubProjectByExternalId(String externalId) {
        log.debug("Fetching SubProject by external ID: {}", externalId);
        SubProject subProject = subProjectRepository.findByExternalId(externalId)
            .orElseThrow(() -> new IllegalArgumentException("SubProject not found with externalId: " + externalId));
        return SubProjectResponse.fromEntity(subProject);
    }

    /**
     * Get SubProjects filtered by status.
     */
    public List<SubProjectResponse> getSubProjectsByStatus(UUID projectId, SubProject.SubProjectStatus status) {
        log.debug("Fetching SubProjects by status: {} for Project: {}", status, projectId);
        // First get all for the project, then filter by status
        return subProjectRepository.findByProjectIdOrderByCodeAsc(projectId)
            .stream()
            .filter(sp -> sp.getStatus() == status)
            .map(SubProjectResponse::fromEntity)
            .toList();
    }

    /**
     * Get SubProjects filtered by building type.
     */
    public List<SubProjectResponse> getSubProjectsByBuildingType(String tenantId, SubProject.BuildingType buildingType) {
        log.debug("Fetching SubProjects by building type: {}", buildingType);
        return subProjectRepository.findByBuildingType(tenantId, buildingType)
            .stream()
            .map(SubProjectResponse::fromEntity)
            .toList();
    }

    /**
     * Get SubProjects filtered by construction phase.
     */
    public List<SubProjectResponse> getSubProjectsByConstructionPhase(String tenantId, SubProject.ConstructionPhase phase) {
        log.debug("Fetching SubProjects by construction phase: {}", phase);
        return subProjectRepository.findByConstructionPhase(tenantId, phase)
            .stream()
            .map(SubProjectResponse::fromEntity)
            .toList();
    }

    /**
     * Get SubProjects filtered by building level.
     */
    public List<SubProjectResponse> getSubProjectsByBuildingLevel(String tenantId, SubProject.BuildingLevel level) {
        log.debug("Fetching SubProjects by building level: {}", level);
        return subProjectRepository.findByBuildingLevel(tenantId, level)
            .stream()
            .map(SubProjectResponse::fromEntity)
            .toList();
    }

    /**
     * Create a new SubProject within a Project.
     */
    @Transactional
    public SubProjectResponse createSubProject(UUID projectId, SubProjectCreateRequest request, String tenantId, UUID orgId, String userId) {
        log.debug("Creating new SubProject: {} for Project: {}", request.getCode(), projectId);

        // Check if code already exists in this project
        if (subProjectRepository.existsByTenantIdAndProjectIdAndCode(tenantId, projectId, request.getCode())) {
            throw new IllegalArgumentException(
                "SubProject code already exists: " + request.getCode() + " for Project: " + projectId);
        }

        SubProject subProject = new SubProject(tenantId, orgId, projectId, request.getCode(), request.getName());

        // Set optional fields
        subProject.setDescription(request.getDescription());
        subProject.setBuildingType(request.getBuildingType());
        subProject.setConstructionPhase(request.getConstructionPhase());
        subProject.setBuildingLevel(request.getBuildingLevel());
        subProject.setUsableArea(request.getUsableArea());
        subProject.setTotalArea(request.getTotalArea());
        subProject.setFloorCount(request.getFloorCount());
        subProject.setUnitCount(request.getUnitCount());
        subProject.setBudget(request.getBudget());
        subProject.setCurrencyCode(request.getCurrencyCode() != null ? request.getCurrencyCode() : "EUR");
        subProject.setExternalId(request.getExternalId());

        // Set floor IDs
        if (request.getFloorIds() != null) {
            subProject.getFloorIds().addAll(request.getFloorIds());
        }

        // Set part IDs
        if (request.getPartIds() != null) {
            subProject.getPartIds().addAll(request.getPartIds());
        }

        subProject.setCreatedBy(userId);
        subProject.setUpdatedBy(userId);

        SubProject saved = subProjectRepository.save(subProject);
        log.info("Created SubProject: {} with ID: {}", saved.getCode(), saved.getId());

        // Publish subproject created event to trigger workflow auto-generation
        try {
            String buildingType = saved.getBuildingType() != null ? saved.getBuildingType().name() : null;
            Map<String, Object> event = Map.of(
                "tenantId", saved.getTenantId(),
                "orgId", saved.getOrgId().toString(),
                "subprojectId", saved.getId().toString(),
                "projectId", saved.getProjectId().toString(),
                "buildingType", buildingType);

            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("project.subproject.created", saved.getId().toString(), eventJson);
            log.info("Published subproject created event for subproject: {}", saved.getId());
        } catch (Exception e) {
            log.warn("Failed to publish subproject created event for subproject: {}", saved.getId(), e);
        }

        return SubProjectResponse.fromEntity(saved);
    }

    /**
     * Update an existing SubProject.
     */
    @Transactional
    public SubProjectResponse updateSubProject(UUID subProjectId, SubProjectUpdateRequest request, String userId) {
        log.debug("Updating SubProject: {}", subProjectId);

        SubProject subProject = subProjectRepository.findById(subProjectId)
            .orElseThrow(() -> new IllegalArgumentException("SubProject not found with id: " + subProjectId));

        // Update fields
        if (request.getName() != null) {
            subProject.setName(request.getName());
        }
        if (request.getDescription() != null) {
            subProject.setDescription(request.getDescription());
        }
        if (request.getBuildingType() != null) {
            subProject.setBuildingType(request.getBuildingType());
        }
        if (request.getConstructionPhase() != null) {
            subProject.setConstructionPhase(request.getConstructionPhase());
        }
        if (request.getBuildingLevel() != null) {
            subProject.setBuildingLevel(request.getBuildingLevel());
        }
        if (request.getFloorIds() != null) {
            subProject.setFloorIds(request.getFloorIds());
        }
        if (request.getPartIds() != null) {
            subProject.setPartIds(request.getPartIds());
        }
        if (request.getUsableArea() != null) {
            subProject.setUsableArea(request.getUsableArea());
        }
        if (request.getTotalArea() != null) {
            subProject.setTotalArea(request.getTotalArea());
        }
        if (request.getFloorCount() != null) {
            subProject.setFloorCount(request.getFloorCount());
        }
        if (request.getUnitCount() != null) {
            subProject.setUnitCount(request.getUnitCount());
        }
        if (request.getStatus() != null) {
            subProject.setStatus(request.getStatus());
        }
        if (request.getStartDate() != null) {
            subProject.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            subProject.setEndDate(request.getEndDate());
        }
        if (request.getProgress() != null) {
            subProject.setProgress(request.getProgress());
        }
        if (request.getBudget() != null) {
            subProject.setBudget(request.getBudget());
        }
        if (request.getCurrencyCode() != null) {
            subProject.setCurrencyCode(request.getCurrencyCode());
        }

        subProject.setUpdatedBy(userId);

        SubProject saved = subProjectRepository.save(subProject);
        log.info("Updated SubProject: {}", saved.getCode());

        return SubProjectResponse.fromEntity(saved);
    }

    /**
     * Delete a SubProject and all its associated workflows.
     */
    @Transactional
    public void deleteSubProject(UUID subProjectId, String userId) {
        log.debug("Deleting SubProject: {}", subProjectId);

        SubProject subProject = subProjectRepository.findById(subProjectId)
            .orElseThrow(() -> new IllegalArgumentException("SubProject not found with id: " + subProjectId));

        // Delete floor IDs and part IDs (element collections are cascade deleted)
        subProject.getFloorIds().clear();
        subProject.getPartIds().clear();

        subProject.setUpdatedBy(userId);

        subProjectRepository.delete(subProject);
        log.info("Deleted SubProject: {}", subProject.getCode());
    }

    /**
     * Delete all SubProjects for a given Project (cascade).
     */
    @Transactional
    public void deleteSubProjectsByProject(UUID projectId) {
        log.debug("Deleting all SubProjects for Project: {}", projectId);
        subProjectRepository.deleteByProjectId(projectId);
        log.info("Deleted all SubProjects for Project: {}", projectId);
    }

    /**
     * Get SubProject count by status for a project.
     */
    public long getSubProjectCountByStatus(UUID projectId, SubProject.SubProjectStatus status) {
        // Need to get tenantId first - this is a simplified version
        return 0;
    }
}
