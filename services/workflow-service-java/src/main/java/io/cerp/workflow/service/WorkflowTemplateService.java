package io.cerp.workflow.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.cerp.workflow.config.KafkaEventPublisher;
import io.cerp.workflow.config.TenantContext;
import io.cerp.workflow.domain.WorkflowTemplate;
import io.cerp.workflow.domain.WorkflowTemplateStep;
import io.cerp.workflow.domain.WorkflowTemplateStepAssigneeRole;
import io.cerp.workflow.dto.WorkflowTemplateRequest;
import io.cerp.workflow.dto.WorkflowTemplateStepRequest;
import io.cerp.workflow.event.WorkflowTemplateActivated;
import io.cerp.workflow.event.WorkflowTemplateCreated;
import io.cerp.workflow.event.WorkflowTemplateDeprecated;
import io.cerp.workflow.repository.WorkflowTemplateRepository;

/**
 * Service layer for WorkflowTemplate aggregate.
 */
@Service
public class WorkflowTemplateService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTemplateService.class);

    private final WorkflowTemplateRepository templateRepository;
    private final KafkaEventPublisher eventPublisher;
    private final TenantService tenantService;
    private String tenantId;

    public WorkflowTemplateService(WorkflowTemplateRepository templateRepository,
            KafkaEventPublisher eventPublisher, TenantService tenantService) {
        this.templateRepository = templateRepository;
        this.eventPublisher = eventPublisher;
        this.tenantService = tenantService;
        Optional<UUID> tenant = tenantService.getTenantIdByCode("default");
        tenantId = tenant.get().toString();
    }

    /**
     * Create a new WorkflowTemplate with its step definitions.
     *
     * <p>
     * Status starts as DRAFT. Use {@link #activate(String, String)} to activate.
     */
    @Transactional
    public WorkflowTemplate createTemplate(WorkflowTemplateRequest request) {

        if (templateRepository.existsByTenantIdAndCode(tenantId, request.code())) {
            throw new IllegalArgumentException("Template code '" + request.code() + "' already exists for tenant");
        }

        WorkflowTemplate template = new WorkflowTemplate();
        template.setTenantId(tenantId);
        template.setCode(request.code());
        template.setName(request.name());
        template.setDescription(request.description());
        template.setPhaseType(request.phaseType());
        template.setParametersSchema(request.parametersSchema());
        template.setDefaultSla(request.defaultSla());
        template.setRequiredDocuments(request.requiredDocuments() != null ? request.requiredDocuments() : java.util.Collections.emptyList());
        template.setOrderNum(request.orderNum());

        if (request.steps() != null) {
            for (int i = 0; i < request.steps().size(); i++) {
                WorkflowTemplateStepRequest stepReq = request.steps().get(i);
                WorkflowTemplateStep step = mapStepRequestToEntity(stepReq, i + 1);
                template.addStep(step);
            }
        }

        WorkflowTemplate saved = templateRepository.save(template);

        eventPublisher.publish(new WorkflowTemplateCreated(
            saved.getId(), saved.getCode(), saved.getName(),
            saved.getPhaseType(), saved.getVersion(),
            saved.getParametersSchema(), saved.getRequiredDocuments()));

        log.info("Created template {} (code: {}) for tenant {}", saved.getId(), request.code(), tenantId);
        return saved;
    }

    /**
     * Get an active template by code.
     */
    @Transactional(readOnly = true)
    public WorkflowTemplate getActiveTemplate(String code) {
        return templateRepository.findByTenantIdAndCodeAndStatus(tenantId, code, WorkflowTemplate.TemplateStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("Active template not found: " + code));
    }

    /**
     * List all active templates for the current tenant.
     */
    @Transactional(readOnly = true)
    public List<WorkflowTemplate> listActiveTemplates() {
        return templateRepository.findByTenantIdAndStatusOrderByOrderNum(tenantId);
    }

    /**
     * List all templates (any status) for the current tenant, ordered by construction phase.
     */
    @Transactional(readOnly = true)
    public List<WorkflowTemplate> listAllTemplates() {
        return templateRepository.findByTenantIdOrderByOrderNum(tenantId);
    }

    /**
     * Update an active template (Draft-only per spec).
     */
    @Transactional
    public WorkflowTemplate updateTemplate(String code, WorkflowTemplateRequest request) {
        WorkflowTemplate template = templateRepository.findByTenantIdAndCodeAndStatus(tenantId, code,
            WorkflowTemplate.TemplateStatus.DRAFT)
            .orElseThrow(() -> new IllegalArgumentException("Draft template not found: " + code));

        template.update(request.name(), request.description(), request.phaseType(),
            request.requiredDocuments(), null, null, request.orderNum());

        templateRepository.save(template);
        return template;
    }

    /**
     * Activate a draft template. Status transitions: DRAFT → ACTIVE.
     */
    @Transactional
    public WorkflowTemplate activateTemplate(String code) {
        WorkflowTemplate template = templateRepository.findByTenantIdAndCodeAndStatus(tenantId, code,
            WorkflowTemplate.TemplateStatus.DRAFT)
            .orElseThrow(() -> new IllegalArgumentException("Draft template not found: " + code));

        template.activate();
        WorkflowTemplate saved = templateRepository.save(template);

        eventPublisher.publish(new WorkflowTemplateActivated(saved.getCode(),
            saved.getVersion()));

        log.info("Activated template {} (code: {})", saved.getId(), code);
        return saved;
    }

    /**
     * Deprecate an active template. Status transitions: ACTIVE → DEPRECATED.
     */
    @Transactional
    public WorkflowTemplate deactivateTemplate(String code) {
        WorkflowTemplate template = templateRepository.findByTenantIdAndCodeAndStatus(tenantId, code,
            WorkflowTemplate.TemplateStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("Active template not found: " + code));

        template.deprecate();
        WorkflowTemplate saved = templateRepository.save(template);

        eventPublisher.publish(new WorkflowTemplateDeprecated(saved.getCode(),
            saved.getVersion()));

        log.info("Deprecated template {} (code: {})", saved.getId(), code);
        return saved;
    }

    /**
     * Get template steps by template code.
     */
    @Transactional(readOnly = true)
    public List<WorkflowTemplateStep> getTemplateSteps(String code) {
        WorkflowTemplate template = templateRepository.findByTenantIdAndCodeAndStatus(tenantId, code,
            WorkflowTemplate.TemplateStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("Active template not found: " + code));
        return template.getSteps();
    }

    private WorkflowTemplateStep mapStepRequestToEntity(WorkflowTemplateStepRequest stepReq, Integer order) {
        WorkflowTemplateStep step = new WorkflowTemplateStep(
            order, stepReq.code(), stepReq.name(),
            io.cerp.workflow.domain.WorkflowTemplateStep.StepType.valueOf(stepReq.type().name()));

        step.setAssigneeRoles(stepReq.assigneeRoles());
        step.setRequiresEvidence(stepReq.requiresEvidence() != null ? stepReq.requiresEvidence() : false);
        step.setRequiresApproval(stepReq.requiresApproval() != null ? stepReq.requiresApproval() : false);
        step.setDueOffsetDays(stepReq.dueOffsetDays());
        step.setSlaHours(stepReq.slaHours());
        step.setWeight(stepReq.weight() != null ? stepReq.weight() : BigDecimal.ONE);
        step.setChecklists(stepReq.checklists());
        step.setGatingCondition(stepReq.gatingCondition());

        // Populate normalized assignee role entities
        if (stepReq.assigneeRoles() != null && !stepReq.assigneeRoles().isEmpty()) {
            for (int i = 0; i < stepReq.assigneeRoles().size(); i++) {
                String roleName = stepReq.assigneeRoles().get(i);
                WorkflowTemplateStepAssigneeRole role = new WorkflowTemplateStepAssigneeRole(
                    step, roleName, i + 1, i == 0); // First role is primary
                step.addAssigneeRole(role);
            }
        }

        return step;
    }
}
