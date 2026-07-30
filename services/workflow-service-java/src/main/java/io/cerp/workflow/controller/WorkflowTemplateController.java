package io.cerp.workflow.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.cerp.workflow.domain.WorkflowTemplate;
import io.cerp.workflow.domain.WorkflowTemplateStep;
import io.cerp.workflow.dto.WorkflowTemplateRequest;
import io.cerp.workflow.service.WorkflowTemplateService;

/**
 * REST API for WorkflowTemplate aggregate.
 *
 * <p>
 * Endpoints per Section 23:
 * <ul>
 * <li>POST /api/v1/workflow-templates – create</li>
 * <li>GET /api/v1/workflow-templates – list active</li>
 * <li>GET /api/v1/workflow-templates/{id} – get by ID</li>
 * <li>PATCH /api/v1/workflow-templates/{code} – update (draft only)</li>
 * <li>POST /api/v1/workflow-templates/{code}/activate</li>
 * <li>POST /api/v1/workflow-templates/{code}/deprecate</li>
 * <li>GET /api/v1/workflow-templates/{code}/steps</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/workflow-templates")
public class WorkflowTemplateController {

    private final WorkflowTemplateService templateService;

    public WorkflowTemplateController(WorkflowTemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    public ResponseEntity<WorkflowTemplate> create(@Valid @RequestBody WorkflowTemplateRequest request) {
        WorkflowTemplate created = templateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<WorkflowTemplate>> listActive() {
        return ResponseEntity.ok(templateService.listActiveTemplates());
    }

    @GetMapping(params = { "all=true" })
    public ResponseEntity<List<WorkflowTemplate>> listAll() {
        return ResponseEntity.ok(templateService.listAllTemplates());
    }

    /**
     * Get a template by ID with its steps eagerly loaded (LEFT JOIN FETCH). Following the same pattern as WorkflowController.getWorkflowDetail.
     */
    @Transactional(readOnly = true)
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) {
        UUID templateId = UUID.fromString(id);
        Optional<WorkflowTemplate> templateOpt = templateService.getTemplateWithSteps(templateId);

        Map<String, Object> response = new HashMap<>();
        templateOpt.ifPresent(t -> {
            response.put("template", t);
            response.put("steps", t.getSteps());
        });

        return templateOpt.isPresent()
                ? ResponseEntity.ok(response)
                : ResponseEntity.notFound().build();
    }

    /**
     * Get a template detail with steps by ID. Returns: { template: {...}, steps: [...] }
     */
    @Transactional(readOnly = true)
    @GetMapping("/{id}/detail")
    public ResponseEntity<Map<String, Object>> getTemplateDetail(@PathVariable String id) {
        UUID templateId = UUID.fromString(id);
        Optional<WorkflowTemplate> templateOpt = templateService.getTemplateWithSteps(templateId);

        Map<String, Object> response = new HashMap<>();
        templateOpt.ifPresent(t -> {
            response.put("template", t);
            response.put("steps", t.getSteps());
        });

        return templateOpt.isPresent()
                ? ResponseEntity.ok(response)
                : ResponseEntity.notFound().build();
    }

    @PatchMapping("/{code}")
    public ResponseEntity<WorkflowTemplate> update(@PathVariable String code,
            @Valid @RequestBody WorkflowTemplateRequest request) {
        WorkflowTemplate updated = templateService.updateTemplate(code, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{code}/activate")
    public ResponseEntity<WorkflowTemplate> activate(@PathVariable String code) {
        WorkflowTemplate activated = templateService.activateTemplate(code);
        return ResponseEntity.ok(activated);
    }

    @PostMapping("/{code}/deprecate")
    public ResponseEntity<WorkflowTemplate> deprecate(@PathVariable String code) {
        WorkflowTemplate deprecated = templateService.deactivateTemplate(code);
        return ResponseEntity.ok(deprecated);
    }

    @GetMapping("/{code}/steps")
    public ResponseEntity<List<WorkflowTemplateStep>> getSteps(@PathVariable String code) {
        return ResponseEntity.ok(templateService.getTemplateSteps(code));
    }

    /**
     * Get an active template by code with its steps eagerly loaded (LEFT JOIN FETCH). This endpoint accepts template codes like "PRE_CONSTRUCTION" instead of UUIDs.
     */
    @Transactional(readOnly = true)
    @GetMapping("/by-code/{code}")
    public ResponseEntity<Map<String, Object>> getByCode(@PathVariable String code) {
        Optional<WorkflowTemplate> templateOpt = templateService.getTemplateByCodeWithSteps(code);

        Map<String, Object> response = new HashMap<>();
        templateOpt.ifPresent(t -> {
            response.put("template", t);
            response.put("steps", t.getSteps());
        });

        return templateOpt.isPresent()
                ? ResponseEntity.ok(response)
                : ResponseEntity.notFound().build();
    }
}
