package io.cerp.project.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import io.cerp.project.domain.Project;
import io.cerp.project.dto.ProjectCreateRequest;
import io.cerp.project.dto.ProjectResponse;
import io.cerp.project.dto.ProjectUpdateRequest;
import io.cerp.project.dto.ProjectUserAssignmentRequest;
import io.cerp.project.dto.SubProjectCreateRequest;
import io.cerp.project.repository.ProjectRepository;

/**
 * Service for managing projects.
 *
 * <p>
 * Handles CRUD operations, tenant/org isolation, code uniqueness, and publishes domain events to Kafka topics.
 */
@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);
    private static final String TOPIC_CREATED = "organization.project.created";
    private static final String TOPIC_UPDATED = "organization.project.updated";
    private static final String TOPIC_DELETED = "organization.project.deleted";

    private final ProjectRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;
    private final io.cerp.project.service.SubProjectService subProjectService;

    public ProjectService(ProjectRepository repository,
            KafkaTemplate<String, Object> kafkaTemplate,
            RestTemplate restTemplate,
            io.cerp.project.service.SubProjectService subProjectService) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.restTemplate = restTemplate;
        this.subProjectService = subProjectService;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> findAll(String tenantId) {
        log.debug("Finding all projects for tenant: {}", tenantId);
        return repository.findByTenantId(tenantId).stream()
            .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
            .map(ProjectResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> findByAssignedUserId(String tenantId, String assignedUserId) {
        log.debug("Finding projects for tenant: {}, assigned user: {}", tenantId, assignedUserId);
        return repository.findByTenantId(tenantId).stream()
            .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
            .filter(p -> isUserAssigned(p, assignedUserId))
            .map(ProjectResponse::from)
            .toList();
    }

    /**
     * Check if a user ID is assigned to the project. Only projects where the user's ID is in assignedUserIds are visible. Projects with empty/null assignedUserIds are
     * NOT visible to vendor users.
     */
    private boolean isUserAssigned(Project project, String userId) {
        String[] assignedIds = project.getAssignedUserIds();
        if (assignedIds == null || assignedIds.length == 0) {
            // If no users are assigned, the project is NOT visible to vendor users
            return false;
        }
        return java.util.Arrays.stream(assignedIds).anyMatch(id -> id.equals(userId));
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> findByOrg(String tenantId, UUID orgId) {
        log.debug("Finding projects for tenant: {}, org: {}", tenantId, orgId);
        return repository.findByTenantIdAndOrgId(tenantId, orgId).stream()
            .map(ProjectResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse findById(String tenantId, UUID id) {
        log.debug("Finding project {} for tenant: {}", id, tenantId);
        return repository.findById(id)
            .filter(p -> p.getTenantId().equals(tenantId))
            .map(ProjectResponse::from)
            .orElseThrow(() -> new ProjectNotFoundException(tenantId, id));
    }

    @Transactional
    public ProjectResponse create(String tenantId, UUID orgId, ProjectCreateRequest request, String userId) {
        if (repository.existsByTenantIdAndCodeNotDeleted(tenantId, request.code())) {
            throw new ProjectCodeAlreadyExistsException(tenantId, request.code());
        }

        // Validate construction site: check if it already has an active project
        if (request.constructionSiteId() != null && !request.constructionSiteId().isBlank()) {
            Project existingProject = repository.findByConstructionSiteIdAndDeletedFalse(request.constructionSiteId()).orElse(null);
            if (existingProject != null) {
                throw new ConstructionSiteAlreadyHasProjectException(request.constructionSiteId(), existingProject.getName(), existingProject.getCode());
            }
        }

        Project project = new Project(tenantId, orgId, request.code(), request.name());

        // Apply create-specific fields
        if (request.description() != null)
            project.setDescription(request.description());
        if (request.constructionSiteId() != null && !request.constructionSiteId().isBlank())
            project.setConstructionSiteId(request.constructionSiteId());
        if (request.propertyId() != null)
            project.setPropertyId(request.propertyId());
        if (request.addressLine1() != null)
            project.setAddressLine1(request.addressLine1());
        if (request.addressLine2() != null)
            project.setAddressLine2(request.addressLine2());
        if (request.postalCode() != null)
            project.setPostalCode(request.postalCode());
        if (request.city() != null)
            project.setCity(request.city());
        if (request.countryCode() != null)
            project.setCountryCode(request.countryCode());
        if (request.projectType() != null)
            project.setProjectType(safeParseProjectType(request.projectType()));
        if (request.startDate() != null)
            project.setStartDate(request.startDate());
        if (request.endDate() != null)
            project.setEndDate(request.endDate());
        if (request.actualStartDate() != null)
            project.setActualStartDate(request.actualStartDate());
        if (request.actualEndDate() != null)
            project.setActualEndDate(request.actualEndDate());
        if (request.progress() != null)
            project.setProgress(request.progress());
        if (request.budget() != null)
            project.setBudget(request.budget());
        if (request.currencyCode() != null)
            project.setCurrencyCode(request.currencyCode());
        if (request.clientName() != null)
            project.setClientName(request.clientName());
        if (request.clientEmail() != null)
            project.setClientEmail(request.clientEmail());
        if (request.clientPhone() != null)
            project.setClientPhone(request.clientPhone());

        project.setCreatedBy(userId);
        project.setUpdatedBy(userId);

        Project saved = repository.save(project);
        log.info("Created project {} for tenant: {}, org: {}", saved.getId(), tenantId, orgId);

        // Auto-create subprojects from construction site buildings
        if (request.constructionSiteId() != null && !request.constructionSiteId().isBlank()) {
            autoCreateSubprojectsFromConstructionSite(saved.getId(), request.constructionSiteId(), tenantId, userId);
            // Refresh to include auto-created subprojects
            saved = repository.findById(saved.getId()).orElse(saved);
        }

        // Publish Kafka event
        publishEvent(TOPIC_CREATED, saved.getCode(), saved);

        return ProjectResponse.from(saved);
    }

    @Transactional
    public ProjectResponse update(String tenantId, UUID id, ProjectUpdateRequest request, String userId) {
        Project project = repository.findById(id)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new ProjectNotFoundException(tenantId, id));

        if (request.code() != null && !request.code().equals(project.getCode())) {
            if (repository.existsByTenantIdAndCodeAndIdNot(tenantId, request.code(), id)) {
                throw new ProjectCodeAlreadyExistsException(tenantId, request.code());
            }
        }

        // Validate construction site if being changed
        if (request.constructionSiteId() != null && !request.constructionSiteId().isBlank()) {
            if (!request.constructionSiteId().equals(project.getConstructionSiteId())) {
                Project existingProject = repository.findByConstructionSiteIdAndDeletedFalse(request.constructionSiteId()).orElse(null);
                if (existingProject != null && !existingProject.getId().equals(id)) {
                    throw new ConstructionSiteAlreadyHasProjectException(request.constructionSiteId(), existingProject.getName(), existingProject.getCode());
                }
            }
        }

        applyUpdateFields(project, request);
        project.setUpdatedBy(userId);

        Project saved = repository.save(project);
        log.info("Updated project {} for tenant: {}", saved.getId(), tenantId);

        publishEvent(TOPIC_UPDATED, saved.getCode(), saved);

        return ProjectResponse.from(saved);
    }

    /**
     * Assign users to a project with specific roles. Each assignment includes a user ID and a role (VIEWER, EDITOR, or ADMIN).
     */
    @Transactional
    public ProjectResponse assignUsersWithRoles(String tenantId, UUID projectId, List<ProjectUserAssignmentRequest.UserAssignment> assignments, String userId) {
        Project project = repository.findById(projectId)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new ProjectNotFoundException(tenantId, projectId));

        // Merge existing assigned user IDs with new ones
        String[] currentIds = project.getAssignedUserIds();
        java.util.Set<String> combined = new java.util.LinkedHashSet<>();
        if (currentIds != null) {
            java.util.Collections.addAll(combined, currentIds);
        }
        assignments.forEach(a -> combined.add(a.userId()));

        // Merge existing roles with new ones
        java.util.Map<String, String> rolesToUpdate = new java.util.HashMap<>();
        java.util.Map<String, String> currentRoles = project.getAssignedUserRoles();
        if (currentRoles != null) {
            rolesToUpdate.putAll(currentRoles);
        }
        for (ProjectUserAssignmentRequest.UserAssignment a : assignments) {
            rolesToUpdate.put(a.userId(), a.role());
        }

        project.setAssignedUserIds(combined.toArray(new String[0]));
        project.setAssignedUserRoles(rolesToUpdate);
        project.setUpdatedBy(userId);

        Project saved = repository.save(project);
        log.info("Assigned users with roles {} to project {}", assignments, projectId);

        return ProjectResponse.from(saved);
    }

    /**
     * Assign users to a project (legacy method - assigns users without roles for backward compatibility).
     */
    @Transactional
    public ProjectResponse assignUsers(String tenantId, UUID projectId, List<String> userIds, String userId) {
        Project project = repository.findById(projectId)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new ProjectNotFoundException(tenantId, projectId));

        // Merge existing assigned user IDs with new ones
        String[] currentIds = project.getAssignedUserIds();
        java.util.Set<String> combined = new java.util.LinkedHashSet<>();
        if (currentIds != null) {
            java.util.Collections.addAll(combined, currentIds);
        }
        combined.addAll(userIds);

        project.setAssignedUserIds(combined.toArray(new String[0]));
        project.setUpdatedBy(userId);

        Project saved = repository.save(project);
        log.info("Assigned users {} to project {}", userIds, projectId);

        return ProjectResponse.from(saved);
    }

    @Transactional
    public ProjectResponse removeUser(String tenantId, UUID projectId, String userIdToRemove) {
        Project project = repository.findById(projectId)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new ProjectNotFoundException(tenantId, projectId));

        String[] currentIds = project.getAssignedUserIds();
        if (currentIds != null && currentIds.length > 0) {
            String[] filtered = java.util.Arrays.stream(currentIds)
                .filter(id -> !id.equals(userIdToRemove))
                .toArray(String[]::new);
            project.setAssignedUserIds(filtered);
        }

        project.setUpdatedBy(userIdToRemove);

        Project saved = repository.save(project);
        log.info("Removed user {} from project {}", userIdToRemove, projectId);

        return ProjectResponse.from(saved);
    }

    @Transactional
    public void delete(String tenantId, UUID id) {
        Project project = repository.findById(id)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new ProjectNotFoundException(tenantId, id));

        String code = project.getCode();
        String constructionSiteId = project.getConstructionSiteId();

        // Soft delete: mark as deleted and release the construction site link
        project.setDeleted(true);
        project.setStatus(Project.ProjectStatus.CANCELLED);
        if (constructionSiteId != null && !constructionSiteId.isBlank()) {
            project.setConstructionSiteId(null);
            log.info("Released construction site link '{}' from deleted project {}", constructionSiteId, id);
        }

        repository.save(project);
        log.info("Soft-deleted project {} for tenant: {}", id, tenantId);

        publishEvent(TOPIC_DELETED, code, project);
    }

    private void publishEvent(String topic, String key, Project project) {
        try {
            kafkaTemplate.send(topic, key.toString(), project);
            log.debug("Published event to topic {}: key={}", topic, key);
        } catch (Exception e) {
            log.warn("Failed to publish event to topic {}: {}", topic, e.getMessage());
        }
    }

    private void applyUpdateFields(Project project, ProjectUpdateRequest request) {
        if (request.code() != null)
            project.setCode(request.code());
        if (request.name() != null)
            project.setName(request.name());
        if (request.description() != null)
            project.setDescription(request.description());
        if (request.constructionSiteId() != null)
            project.setConstructionSiteId(request.constructionSiteId());
        if (request.propertyId() != null)
            project.setPropertyId(request.propertyId());
        if (request.addressLine1() != null)
            project.setAddressLine1(request.addressLine1());
        if (request.addressLine2() != null)
            project.setAddressLine2(request.addressLine2());
        if (request.postalCode() != null)
            project.setPostalCode(request.postalCode());
        if (request.city() != null)
            project.setCity(request.city());
        if (request.countryCode() != null)
            project.setCountryCode(request.countryCode());
        if (request.projectType() != null)
            project.setProjectType(safeParseProjectType(request.projectType()));
        if (request.status() != null)
            project.setStatus(safeParseProjectStatus(request.status()));
        if (request.startDate() != null)
            project.setStartDate(request.startDate());
        if (request.endDate() != null)
            project.setEndDate(request.endDate());
        if (request.actualStartDate() != null)
            project.setActualStartDate(request.actualStartDate());
        if (request.actualEndDate() != null)
            project.setActualEndDate(request.actualEndDate());
        if (request.progress() != null)
            project.setProgress(request.progress());
        if (request.budget() != null)
            project.setBudget(request.budget());
        if (request.currencyCode() != null)
            project.setCurrencyCode(request.currencyCode());
        if (request.clientName() != null)
            project.setClientName(request.clientName());
        if (request.clientEmail() != null)
            project.setClientEmail(request.clientEmail());
        if (request.clientPhone() != null)
            project.setClientPhone(request.clientPhone());
    }

    public static class ProjectNotFoundException extends RuntimeException {
        public ProjectNotFoundException(String tenantId, UUID id) {
            super(String.format("Project %s not found for tenant %s", id, tenantId));
        }
    }

    public static class ProjectCodeAlreadyExistsException extends RuntimeException {
        public ProjectCodeAlreadyExistsException(String tenantId, String code) {
            super(String.format("Project code '%s' already exists for tenant %s", code, tenantId));
        }
    }

    /**
     * Safely parse project type from string, handling both frontend and backend enum names. Frontend uses: RESIDENTIAL, COMMERCIAL, INDUSTRIAL, INFRASTRUCTURE,
     * MIXED_USE, OTHER Backend uses: NEW_CONSTRUCTION, RENOVATION, MAINTENANCE, INFRASTRUCTURE, COMMERCIAL, RESIDENTIAL, INDUSTRIAL
     */
    private static Project.ProjectType safeParseProjectType(String value) {
        try {
            return Project.ProjectType.valueOf(value);
        } catch (IllegalArgumentException e) {
            // Map frontend enum values to backend equivalents
            return switch (value.toUpperCase()) {
                case "MIXED_USE" -> Project.ProjectType.COMMERCIAL;
                case "OTHER" -> Project.ProjectType.NEW_CONSTRUCTION;
                default -> {
                    log.warn("Unknown project type: {}, defaulting to NEW_CONSTRUCTION", value);
                    yield Project.ProjectType.NEW_CONSTRUCTION;
                }
            };
        }
    }

    /**
     * Safely parse project status from string, handling both frontend and backend enum names. Frontend uses: PLANNED, ACTIVE, ON_HOLD, COMPLETED, CANCELLED Backend uses:
     * PLANNING, ACTIVE, ON_HOLD, COMPLETED, CANCELLED, SUSPENDED
     */
    private static Project.ProjectStatus safeParseProjectStatus(String value) {
        try {
            return Project.ProjectStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            // Map frontend enum values to backend equivalents
            return switch (value.toUpperCase()) {
                case "PLANNED" -> Project.ProjectStatus.PLANNING;
                case "ON_HOLD" -> Project.ProjectStatus.ON_HOLD;
                case "COMPLETED" -> Project.ProjectStatus.COMPLETED;
                case "CANCELLED" -> Project.ProjectStatus.CANCELLED;
                default -> {
                    log.warn("Unknown project status: {}, defaulting to PLANNING", value);
                    yield Project.ProjectStatus.PLANNING;
                }
            };
        }
    }

    /**
     * Auto-create subprojects from buildings of the associated construction site. Each building becomes a subproject with code derived from the building name.
     *
     * @param projectId
     *            the ID of the newly created project
     * @param constructionSiteId
     *            the ID of the construction site (String ID from core-api)
     * @param tenantId
     *            the tenant identifier
     * @param userId
     *            the user creating the subprojects
     */
    /**
     * Auto-create subprojects from buildings of the associated construction site. Each building becomes a subproject with code derived from the building name.
     *
     * @param projectId
     *            the ID of the newly created project
     * @param constructionSiteId
     *            the ID of the construction site (String ID from core-api)
     * @param tenantId
     *            the tenant identifier
     * @param userId
     *            the user creating the subprojects
     */
    private void autoCreateSubprojectsFromConstructionSite(UUID projectId, String constructionSiteId, String tenantId, String userId) {
        log.info("Auto-creating subprojects from construction site '{}' for project {}", constructionSiteId, projectId);

        try {
            // Fetch construction site from core-api to get buildings
            @SuppressWarnings("unchecked")
            Map<String, Object> constructionSite = restTemplate.getForObject(
                "/api/construction-sites/{siteId}", Map.class, constructionSiteId);

            if (constructionSite == null) {
                log.warn("Construction site '{}' not found, skipping subproject auto-creation", constructionSiteId);
                return;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> buildings = (List<Map<String, Object>>) constructionSite.get("buildings");

            if (buildings == null || buildings.isEmpty()) {
                log.info("No buildings found in construction site '{}', skipping subproject auto-creation", constructionSiteId);
                return;
            }

            Optional<Project> projectOpt = repository.findById(projectId);
            UUID orgId = projectOpt.map(Project::getOrgId).orElse(null);

            for (Map<String, Object> building : buildings) {
                String buildingId = (String) building.get("id");
                String buildingName = (String) building.get("name");
                if (buildingName == null || buildingName.isBlank()) {
                    buildingName = "Unnamed Building " + buildingId;
                }

                // Generate subproject code from building name (e.g., "Building A" -> "BLD-A")
                String code = buildingName.toUpperCase()
                    .replaceAll("[^A-Z0-9]", "")
                    .replaceAll("(?<!^){2,}", "-")
                    .substring(0, Math.min(64, buildingName.length()));
                if (code.isBlank()) {
                    code = "BLD-" + buildingId.replace("-", "").substring(0, 8);
                }

                // Check if subproject already exists via SubProjectRepository
                var subProjectRequest = new SubProjectCreateRequest();
                subProjectRequest.setCode(code);
                subProjectRequest.setName(buildingName);
                subProjectRequest.setDescription(null);
                subProjectRequest.setBuildingType(null);
                subProjectRequest.setConstructionPhase(null);
                subProjectRequest.setBuildingLevel(io.cerp.project.domain.SubProject.BuildingLevel.WHOLE);
                subProjectRequest.setFloorIds(List.of());
                subProjectRequest.setPartIds(List.of());
                subProjectRequest.setUsableArea(null);
                subProjectRequest.setTotalArea(null);
                subProjectRequest.setFloorCount(null);
                subProjectRequest.setUnitCount(null);
                subProjectRequest.setBudget(null);
                subProjectRequest.setCurrencyCode("EUR");
                subProjectRequest.setExternalId(buildingId);

                try {
                    subProjectService.createSubProject(projectId, subProjectRequest, tenantId, orgId, userId);
                    log.info("Auto-created subproject '{}' (code: {}) from building '{}'", buildingName, code, buildingId);
                } catch (Exception e) {
                    // Check if it's a duplicate code error
                    if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                        log.info("Subproject with code '{}' already exists for project {}, skipping", code, projectId);
                    } else {
                        log.warn("Failed to create subproject for building '{}': {}", buildingName, e.getMessage());
                    }
                }
            }

            log.info("Completed auto-creation of subprojects from construction site '{}'", constructionSiteId);
        } catch (Exception e) {
            log.warn("Failed to auto-create subprojects from construction site '{}': {}", constructionSiteId, e.getMessage());
        }
    }
}
