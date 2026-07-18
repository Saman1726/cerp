package io.cerp.workflow.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.cerp.workflow.domain.TemplatePhaseMapping.BuildingTypeGroup;
import io.cerp.workflow.dto.TemplatePhaseMappingRequest;
import io.cerp.workflow.dto.TemplatePhaseMappingResponse;
import io.cerp.workflow.service.TemplatePhaseMappingService;

/**
 * REST controller for managing template-phase mappings.
 */
@RestController
@RequestMapping("/api/v1/{tenantId}/template-phase-mappings")
public class TemplatePhaseMappingController {

    private final TemplatePhaseMappingService templatePhaseMappingService;

    public TemplatePhaseMappingController(TemplatePhaseMappingService templatePhaseMappingService) {
        this.templatePhaseMappingService = templatePhaseMappingService;
    }

    /**
     * Create a new template-phase mapping.
     */
    @PostMapping
    public ResponseEntity<TemplatePhaseMappingResponse> createMapping(
            @PathVariable String tenantId,
            @Valid @RequestBody TemplatePhaseMappingRequest request) {
        TemplatePhaseMappingResponse response = templatePhaseMappingService.createMapping(
            tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all template-phase mappings for a tenant.
     */
    @GetMapping
    public ResponseEntity<List<TemplatePhaseMappingResponse>> getAllMappings(
            @PathVariable String tenantId) {
        List<TemplatePhaseMappingResponse> responses = templatePhaseMappingService.getAllMappings(tenantId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get template-phase mappings filtered by phase type and building type group.
     */
    @GetMapping(params = "phaseType")
    public ResponseEntity<List<TemplatePhaseMappingResponse>> getMappingsByPhase(
            @PathVariable String tenantId,
            @org.springframework.web.bind.annotation.RequestParam String phaseType,
            @org.springframework.web.bind.annotation.RequestParam(required = false) BuildingTypeGroup buildingTypeGroup) {
        List<TemplatePhaseMappingResponse> responses;
        if (buildingTypeGroup != null) {
            responses = templatePhaseMappingService.getMappingsByPhaseAndBuildingType(
                tenantId, phaseType, buildingTypeGroup);
        } else {
            responses = templatePhaseMappingService.getMappingsByPhase(tenantId, phaseType);
        }
        return ResponseEntity.ok(responses);
    }

    /**
     * Get a specific template-phase mapping by ID.
     */
    @GetMapping("/{mappingId}")
    public ResponseEntity<TemplatePhaseMappingResponse> getMapping(
            @PathVariable String tenantId,
            @PathVariable UUID mappingId) {
        TemplatePhaseMappingResponse response = templatePhaseMappingService.getMapping(tenantId, mappingId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Update a template-phase mapping.
     */
    @PutMapping("/{mappingId}")
    public ResponseEntity<TemplatePhaseMappingResponse> updateMapping(
            @PathVariable String tenantId,
            @PathVariable UUID mappingId,
            @Valid @RequestBody TemplatePhaseMappingRequest request) {
        TemplatePhaseMappingResponse response = templatePhaseMappingService.updateMapping(
            tenantId, mappingId, request);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a template-phase mapping.
     */
    @DeleteMapping("/{mappingId}")
    public ResponseEntity<Void> deleteMapping(
            @PathVariable String tenantId,
            @PathVariable UUID mappingId) {
        boolean deleted = templatePhaseMappingService.deleteMapping(tenantId, mappingId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle the enabled status of a mapping.
     */
    @PutMapping("/{mappingId}/toggle")
    public ResponseEntity<TemplatePhaseMappingResponse> toggleMapping(
            @PathVariable String tenantId,
            @PathVariable UUID mappingId) {
        TemplatePhaseMappingResponse response = templatePhaseMappingService.toggleMapping(tenantId, mappingId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Get all distinct phase types for a tenant.
     */
    @GetMapping("/phase-types")
    public ResponseEntity<List<String>> getPhaseTypes(
            @PathVariable String tenantId) {
        List<String> phaseTypes = templatePhaseMappingService.getPhaseTypes(tenantId);
        return ResponseEntity.ok(phaseTypes);
    }

    /**
     * Get all building type groups that have mappings.
     */
    @GetMapping("/building-type-groups")
    public ResponseEntity<List<BuildingTypeGroup>> getBuildingTypeGroups(
            @PathVariable String tenantId) {
        List<BuildingTypeGroup> groups = templatePhaseMappingService.getBuildingTypeGroups(tenantId);
        return ResponseEntity.ok(groups);
    }
}
