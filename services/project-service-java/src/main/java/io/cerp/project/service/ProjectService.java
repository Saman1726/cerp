package io.cerp.project.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.cerp.project.domain.Project;
import io.cerp.project.dto.AssignedUserResponse;
import io.cerp.project.dto.ProjectCreateRequest;
import io.cerp.project.dto.ProjectResponse;
import io.cerp.project.dto.ProjectUpdateRequest;
import io.cerp.project.dto.ProjectUserAssignmentRequest;
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
    private final io.cerp.project.client.IdentitySubscriptionClient identitySubscriptionClient;
    private final io.cerp.project.client.IdentityUserClient identityUserClient;
    private final io.cerp.project.client.WorkflowServiceClient workflowServiceClient;
    private final SubProjectService subProjectService;

    public ProjectService(ProjectRepository repository,
            KafkaTemplate<String, Object> kafkaTemplate,
            io.cerp.project.client.IdentitySubscriptionClient identitySubscriptionClient,
            io.cerp.project.client.IdentityUserClient identityUserClient,
            io.cerp.project.client.WorkflowServiceClient workflowServiceClient,
            SubProjectService subProjectService) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.identitySubscriptionClient = identitySubscriptionClient;
        this.identityUserClient = identityUserClient;
        this.workflowServiceClient = workflowServiceClient;
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
    public ProjectResponse create(String tenantId, UUID orgId, ProjectCreateRequest request, String userId, String callerToken) {
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

        // The creator is automatically and permanently the project's primary Owner (see
        // Project.primaryOwnerId's javadoc) - distinct from, but also immediately reflected
        // as, an OWNER entry in assignedUserRoles, so they show up as an assignee too.
        UUID primaryOwnerId = UUID.fromString(userId);
        project.setPrimaryOwnerId(primaryOwnerId);
        project.setAssignedUserIds(new String[] { userId });
        project.setAssignedUserRoles(new java.util.HashMap<>(java.util.Map.of(userId, "OWNER")));

        Project saved = saveGuardingConstructionSiteUniqueness(project, request.constructionSiteId());
        log.info("Created project {} for tenant: {}, org: {}", saved.getId(), tenantId, orgId);

        // Mirror the owner's OWNER entry into identity-service's resource_members so
        // write-permission checks elsewhere (e.g. workflow-service's saga creation) see it too -
        // assignedUserRoles alone is project-service-local and nothing else reads it.
        identityUserClient.grantProjectMembership(callerToken, saved.getId(), primaryOwnerId, "owner");

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

        Project saved = saveGuardingConstructionSiteUniqueness(project, project.getConstructionSiteId());
        log.info("Updated project {} for tenant: {}", saved.getId(), tenantId);

        publishEvent(TOPIC_UPDATED, saved.getCode(), saved);

        return ProjectResponse.from(saved);
    }

    /**
     * The findByConstructionSiteIdAndDeletedFalse checks in create()/update() are application-level only and race-prone (two concurrent requests can both pass the check
     * before either commits) - the V2 migration's idx_projects_construction_site_id_active unique index is the real backstop. Translate a violation of that specific
     * index into the same domain exception the application-level check throws; re-throw anything else untouched so an unrelated DB error isn't mislabeled.
     */
    private Project saveGuardingConstructionSiteUniqueness(Project project, String constructionSiteId) {
        try {
            // saveAndFlush, not save: save() alone just queues the INSERT for a later,
            // transaction-commit-time flush - by then we're outside this try/catch and the
            // unique-index violation surfaces as a raw 500 instead of the domain exception below.
            return repository.saveAndFlush(project);
        } catch (DataIntegrityViolationException e) {
            // Postgres poisons the whole transaction on a constraint violation - any further
            // statement on this same connection (e.g. looking up the existing project for a
            // nicer message) fails with "current transaction is aborted." The upfront check in
            // create()/update() already gives the detailed message for the common case; this
            // catch only fires for the genuine race window, so a simpler message is fine here.
            String msg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
            if (msg != null && msg.contains("idx_projects_construction_site_id_active") && constructionSiteId != null) {
                throw new ConstructionSiteAlreadyHasProjectException(constructionSiteId);
            }
            throw e;
        }
    }

    /**
     * Assign users to a project with specific roles. Each assignment includes a user ID and a role (OWNER, MANAGER, SUPERVISOR, or VIEWER).
     */
    @Transactional
    public ProjectResponse assignUsersWithRoles(String tenantId, UUID projectId, List<ProjectUserAssignmentRequest.UserAssignment> assignments, String userId,
            String callerToken) {
        Project project = repository.findById(projectId)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new ProjectNotFoundException(tenantId, projectId));

        // Resolve each assignment's userId (an email, per the modal - or a UUID, if re-submitted
        // from already-resolved display data) to a real identity-service UUID up front. This
        // keeps assignedUserIds consistently UUID-keyed (matching the owner's format instead of
        // mixing in raw emails) and gives the permission grant below a real UUID to target.
        record Resolved(UUID userId, String role) {
        }
        List<Resolved> resolvedAssignments = assignments.stream()
            .map(a -> new Resolved(
                identityUserClient.resolveUserId(a.userId())
                    .orElseThrow(() -> new IllegalArgumentException("No registered user found for: " + a.userId())),
                a.role()))
            .toList();

        String primaryOwnerId = project.getPrimaryOwnerId() != null ? project.getPrimaryOwnerId().toString() : null;
        for (Resolved r : resolvedAssignments) {
            if (r.userId().toString().equals(primaryOwnerId) && !"OWNER".equals(r.role())) {
                throw new IllegalArgumentException("Cannot change the primary Owner's role - it is permanent, set at project creation.");
            }
        }

        // Merge existing assigned user IDs with new ones
        String[] currentIds = project.getAssignedUserIds();
        java.util.Set<String> combined = new java.util.LinkedHashSet<>();
        if (currentIds != null) {
            java.util.Collections.addAll(combined, currentIds);
        }
        resolvedAssignments.forEach(r -> combined.add(r.userId().toString()));

        // Merge existing roles with new ones
        java.util.Map<String, String> rolesToUpdate = new java.util.HashMap<>();
        java.util.Map<String, String> currentRoles = project.getAssignedUserRoles();
        if (currentRoles != null) {
            rolesToUpdate.putAll(currentRoles);
        }
        resolvedAssignments.forEach(r -> rolesToUpdate.put(r.userId().toString(), r.role()));

        enforceMemberCap(project, combined, rolesToUpdate);

        project.setAssignedUserIds(combined.toArray(new String[0]));
        project.setAssignedUserRoles(rolesToUpdate);
        project.setUpdatedBy(userId);

        Project saved = repository.save(project);
        log.info("Assigned users with roles {} to project {}", assignments, projectId);

        // Mirror into identity-service's resource_members so write-permission checks elsewhere
        // (e.g. workflow-service's saga creation) see this assignment too.
        resolvedAssignments.forEach(r -> identityUserClient.grantProjectMembership(callerToken, projectId, r.userId(), toResourceRole(r.role())));

        return ProjectResponse.from(saved);
    }

    /**
     * Maps project-service's role vocabulary (OWNER/MANAGER/SUPERVISOR/VIEWER) onto identity-service's resource-membership role names (owner/manager/member/viewer).
     * SUPERVISOR maps to "member" (read+write, no delete/assign) as this app's day-to-day execution role.
     */
    private static String toResourceRole(String projectRole) {
        return switch (projectRole) {
            case "OWNER" -> "owner";
            case "MANAGER" -> "manager";
            case "SUPERVISOR" -> "member";
            default -> "viewer";
        };
    }

    /**
     * Assign users to a project (legacy method - assigns users without roles for backward compatibility).
     */
    @Transactional
    public ProjectResponse assignUsers(String tenantId, UUID projectId, List<String> userIds, String userId, String callerToken) {
        Project project = repository.findById(projectId)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new ProjectNotFoundException(tenantId, projectId));

        List<UUID> resolvedIds = userIds.stream()
            .map(id -> identityUserClient.resolveUserId(id)
                .orElseThrow(() -> new IllegalArgumentException("No registered user found for: " + id)))
            .toList();

        // Merge existing assigned user IDs with new ones
        String[] currentIds = project.getAssignedUserIds();
        java.util.Set<String> combined = new java.util.LinkedHashSet<>();
        if (currentIds != null) {
            java.util.Collections.addAll(combined, currentIds);
        }
        resolvedIds.forEach(id -> combined.add(id.toString()));

        enforceMemberCap(project, combined, project.getAssignedUserRoles());

        project.setAssignedUserIds(combined.toArray(new String[0]));
        project.setUpdatedBy(userId);

        Project saved = repository.save(project);
        log.info("Assigned users {} to project {}", userIds, projectId);

        // Legacy endpoint has no role concept - "viewer" matches its own javadoc ("assigns users
        // with VIEWER role by default" in ProjectController).
        resolvedIds.forEach(id -> identityUserClient.grantProjectMembership(callerToken, projectId, id, "viewer"));

        return ProjectResponse.from(saved);
    }

    /**
     * Counts the resulting roster by role bucket (OWNER/MANAGER/everything else - SUPERVISOR, VIEWER, or a legacy no-role assignment) and checks it against the
     * project's primary owner's plan limits - the owner's plan governs the project, not whoever happens to be making this call (consistent with Company-owned-project
     * billing). Projects created before this shipped have no primaryOwnerId - nothing to check a quota against, so they're not retroactively capped.
     */
    private void enforceMemberCap(Project project, java.util.Set<String> resultingUserIds, java.util.Map<String, String> resultingRoles) {
        if (project.getPrimaryOwnerId() == null) {
            return;
        }
        io.cerp.project.client.IdentitySubscriptionClient.PlanLimits limits = identitySubscriptionClient.getPlanLimits(project.getPrimaryOwnerId())
            .orElseThrow(() -> new QuotaCheckUnavailableException("Could not verify project member quota"));

        long owners = 0;
        long managers = 0;
        long others = 0;
        for (String memberId : resultingUserIds) {
            String role = resultingRoles != null ? resultingRoles.getOrDefault(memberId, "") : "";
            if ("OWNER".equals(role)) {
                owners++;
            } else if ("MANAGER".equals(role)) {
                managers++;
            } else {
                others++;
            }
        }
        if (limits.maxOwnersPerProject() != null && owners > limits.maxOwnersPerProject()) {
            throw new QuotaExceededException("This project's plan allows at most " + limits.maxOwnersPerProject() + " Owner(s).");
        }
        if (limits.maxManagersPerProject() != null && managers > limits.maxManagersPerProject()) {
            throw new QuotaExceededException("This project's plan allows at most " + limits.maxManagersPerProject() + " Manager(s).");
        }
        if (limits.maxMembersPerProject() != null && others > limits.maxMembersPerProject()) {
            throw new QuotaExceededException(
                "This project's plan allows at most " + limits.maxMembersPerProject() + " other member(s) (Supervisor/Viewer).");
        }
    }

    @Transactional
    public ProjectResponse removeUser(String tenantId, UUID projectId, String userIdToRemove, String callerToken) {
        Project project = repository.findById(projectId)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new ProjectNotFoundException(tenantId, projectId));

        if (project.getPrimaryOwnerId() != null && userIdToRemove.equals(project.getPrimaryOwnerId().toString())) {
            throw new IllegalArgumentException("Cannot remove the primary Owner from their own project.");
        }

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

        // Resolve rather than assume userIdToRemove is already a UUID: projects assigned before
        // this fix may still hold a raw email for this entry.
        identityUserClient.resolveUserId(userIdToRemove)
            .ifPresent(resolvedId -> identityUserClient.revokeProjectMembership(callerToken, projectId, resolvedId));

        return ProjectResponse.from(saved);
    }

    /**
     * Assigned users enriched with their emails, for display (e.g. the Assign Users popup) - assignedUserIds itself is UUID-keyed going forward, which isn't
     * human-readable on its own.
     */
    @Transactional(readOnly = true)
    public List<AssignedUserResponse> getAssignedUsersWithEmails(String tenantId, UUID projectId, String callerToken) {
        Project project = repository.findById(projectId)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new ProjectNotFoundException(tenantId, projectId));

        String[] assignedIds = project.getAssignedUserIds();
        if (assignedIds == null || assignedIds.length == 0) {
            return List.of();
        }
        java.util.Map<String, String> roles = project.getAssignedUserRoles() != null ? project.getAssignedUserRoles() : java.util.Map.of();
        return java.util.Arrays.stream(assignedIds)
            .map(id -> new AssignedUserResponse(id, resolveDisplayEmail(callerToken, id), roles.getOrDefault(id, "VIEWER")))
            .toList();
    }

    /**
     * Resolves an assignedUserIds entry to an email for display. Entries are UUIDs going forward, but projects assigned before this fix may still hold a raw email
     * (assign wasn't resolving through identity-service yet) - those already look like the display value, so no lookup is needed for them.
     */
    private String resolveDisplayEmail(String callerToken, String assignedId) {
        try {
            UUID uuid = UUID.fromString(assignedId);
            return identityUserClient.resolveEmail(callerToken, uuid).orElse(assignedId);
        } catch (IllegalArgumentException notAUuid) {
            return assignedId;
        }
    }

    /**
     * Delete a project. Cascades to archive (soft-delete, not purge - see SubProject.deleted's
     * javadoc) everything hanging off it: its SubProjects locally, and its Workflows and
     * Bidding/Procurement sagas via workflow-service. The cascade runs BEFORE the project itself
     * is marked deleted, so if workflow-service is unreachable the whole delete fails with the
     * project (and everything under it) left fully intact and the operation safely retryable,
     * rather than leaving an inconsistent half-deleted project with live, visible child data.
     */
    @Transactional
    public void delete(String tenantId, UUID id, String callerToken) {
        Project project = repository.findById(id)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new ProjectNotFoundException(tenantId, id));

        String code = project.getCode();
        String constructionSiteId = project.getConstructionSiteId();

        workflowServiceClient.archiveByProjectId(callerToken, id);
        subProjectService.archiveSubProjectsByProject(id);

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

}
