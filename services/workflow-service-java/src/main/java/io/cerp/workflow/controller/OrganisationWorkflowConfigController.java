package io.cerp.workflow.controller;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.cerp.workflow.dto.OrganisationWorkflowConfigRequest;
import io.cerp.workflow.dto.OrganisationWorkflowConfigResponse;
import io.cerp.workflow.service.OrganisationWorkflowConfigService;

/**
 * REST controller for managing organisation workflow configurations.
 */
@RestController
@RequestMapping("/api/v1/{tenantId}/organisations/{orgId}/workflow-config")
public class OrganisationWorkflowConfigController {

    private final OrganisationWorkflowConfigService organisationWorkflowConfigService;

    public OrganisationWorkflowConfigController(OrganisationWorkflowConfigService organisationWorkflowConfigService) {
        this.organisationWorkflowConfigService = organisationWorkflowConfigService;
    }

    /**
     * Get or create a workflow config for an organisation.
     */
    @GetMapping
    public ResponseEntity<OrganisationWorkflowConfigResponse> getConfig(
            @PathVariable String tenantId,
            @PathVariable UUID orgId) {
        OrganisationWorkflowConfigResponse response = organisationWorkflowConfigService.getOrCreateConfig(tenantId, orgId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update organisation workflow configuration.
     */
    @PutMapping
    public ResponseEntity<OrganisationWorkflowConfigResponse> updateConfig(
            @PathVariable String tenantId,
            @PathVariable UUID orgId,
            @Valid @RequestBody OrganisationWorkflowConfigRequest request) {
        OrganisationWorkflowConfigResponse response = organisationWorkflowConfigService.updateConfig(tenantId, orgId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Enable auto-generation for an organisation.
     */
    @PostMapping("/auto-generation/enable")
    public ResponseEntity<OrganisationWorkflowConfigResponse> enableAutoGeneration(
            @PathVariable String tenantId,
            @PathVariable UUID orgId) {
        OrganisationWorkflowConfigResponse response = organisationWorkflowConfigService.enableAutoGeneration(tenantId, orgId);
        return ResponseEntity.ok(response);
    }

    /**
     * Disable auto-generation for an organisation.
     */
    @PostMapping("/auto-generation/disable")
    public ResponseEntity<OrganisationWorkflowConfigResponse> disableAutoGeneration(
            @PathVariable String tenantId,
            @PathVariable UUID orgId) {
        OrganisationWorkflowConfigResponse response = organisationWorkflowConfigService.disableAutoGeneration(tenantId, orgId);
        return ResponseEntity.ok(response);
    }

    /**
     * Set selected template codes.
     */
    @PutMapping("/selected-templates")
    public ResponseEntity<OrganisationWorkflowConfigResponse> setSelectedTemplateCodes(
            @PathVariable String tenantId,
            @PathVariable UUID orgId,
            @RequestBody java.util.List<String> templateCodes) {
        OrganisationWorkflowConfigResponse response = organisationWorkflowConfigService.setSelectedTemplateCodes(tenantId, orgId, templateCodes);
        return ResponseEntity.ok(response);
    }

    /**
     * Set custom phase mappings.
     */
    @PutMapping("/custom-phase-mappings")
    public ResponseEntity<OrganisationWorkflowConfigResponse> setCustomPhaseMappings(
            @PathVariable String tenantId,
            @PathVariable UUID orgId,
            @RequestBody Map<String, Map<String, java.util.List<String>>> customMappings) {
        OrganisationWorkflowConfigResponse response = organisationWorkflowConfigService.setCustomPhaseMappings(tenantId, orgId, customMappings);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if auto-generation is enabled.
     */
    @GetMapping("/auto-generation/status")
    public ResponseEntity<Map<String, Boolean>> getAutoGenerationStatus(
            @PathVariable String tenantId,
            @PathVariable UUID orgId) {
        boolean enabled = organisationWorkflowConfigService.isAutoGenerationEnabled(tenantId, orgId);
        return ResponseEntity.ok(Map.of("enabled", enabled));
    }
}
