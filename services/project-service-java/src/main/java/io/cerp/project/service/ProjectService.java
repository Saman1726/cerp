package io.cerp.project.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.cerp.project.domain.Project;
import io.cerp.project.dto.ProjectCreateRequest;
import io.cerp.project.dto.ProjectResponse;
import io.cerp.project.dto.ProjectUpdateRequest;
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

    public ProjectService(ProjectRepository repository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> findAll(String tenantId) {
        log.debug("Finding all projects for tenant: {}", tenantId);
        return repository.findByTenantId(tenantId).stream()
            .map(ProjectResponse::from)
            .toList();
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
        if (repository.existsByTenantIdAndCode(tenantId, request.code())) {
            throw new ProjectCodeAlreadyExistsException(tenantId, request.code());
        }

        Project project = new Project(tenantId, orgId, request.code(), request.name());

        applyUpdateFields(project, request);
        project.setCreatedBy(userId);
        project.setUpdatedBy(userId);

        Project saved = repository.save(project);
        log.info("Created project {} for tenant: {}, org: {}", saved.getId(), tenantId, orgId);

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

        applyUpdateFields(project, request);
        project.setUpdatedBy(userId);

        Project saved = repository.save(project);
        log.info("Updated project {} for tenant: {}", saved.getId(), tenantId);

        publishEvent(TOPIC_UPDATED, saved.getCode(), saved);

        return ProjectResponse.from(saved);
    }

    @Transactional
    public void delete(String tenantId, UUID id) {
        Project project = repository.findById(id)
            .filter(p -> p.getTenantId().equals(tenantId))
            .orElseThrow(() -> new ProjectNotFoundException(tenantId, id));

        String code = project.getCode();
        repository.delete(project);
        log.info("Deleted project {} for tenant: {}", id, tenantId);

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
            project.setProjectType(Project.ProjectType.valueOf(request.projectType()));
        if (request.status() != null)
            project.setStatus(Project.ProjectStatus.valueOf(request.status()));
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
}
