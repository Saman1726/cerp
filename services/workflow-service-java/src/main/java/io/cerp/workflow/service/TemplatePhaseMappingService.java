package io.cerp.workflow.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import io.cerp.workflow.domain.TemplatePhaseMapping;
import io.cerp.workflow.domain.TemplatePhaseMapping.BuildingTypeGroup;
import io.cerp.workflow.dto.TemplatePhaseMappingRequest;
import io.cerp.workflow.dto.TemplatePhaseMappingResponse;
import io.cerp.workflow.repository.TemplatePhaseMappingRepository;

/**
 * Service for managing template-phase mappings.
 */
@Service
public class TemplatePhaseMappingService {

    private final TemplatePhaseMappingRepository templatePhaseMappingRepository;

    public TemplatePhaseMappingService(TemplatePhaseMappingRepository templatePhaseMappingRepository) {
        this.templatePhaseMappingRepository = templatePhaseMappingRepository;
    }

    /**
     * Create a new template-phase mapping.
     */
    public TemplatePhaseMappingResponse createMapping(String tenantId, TemplatePhaseMappingRequest request) {
        TemplatePhaseMapping mapping = new TemplatePhaseMapping();
        mapping.setTenantId(tenantId);
        mapping.setPhaseType(request.getPhaseType());
        mapping.setBuildingTypeGroup(request.getBuildingTypeGroup());
        mapping.setWorkflowTemplateCode(request.getWorkflowTemplateCode());
        mapping.setPriority(request.getPriority());
        mapping.setEnabled(request.getEnabled());

        TemplatePhaseMapping saved = templatePhaseMappingRepository.save(mapping);
        return toResponse(saved);
    }

    /**
     * Get all template-phase mappings for a tenant.
     */
    public List<TemplatePhaseMappingResponse> getAllMappings(String tenantId) {
        List<TemplatePhaseMapping> mappings = templatePhaseMappingRepository.findByTenantIdAndEnabledTrueOrderByPhaseType(tenantId);
        return mappings.stream().map(this::toResponse).toList();
    }

    /**
     * Get template-phase mappings filtered by phase type.
     */
    public List<TemplatePhaseMappingResponse> getMappingsByPhase(String tenantId, String phaseType) {
        List<BuildingTypeGroup> groups = List.of(BuildingTypeGroup.values());
        List<TemplatePhaseMapping> allMappings = new java.util.ArrayList<>();
        for (BuildingTypeGroup group : groups) {
            allMappings.addAll(templatePhaseMappingRepository
                .findByTenantIdAndPhaseTypeAndBuildingTypeGroupAndEnabledTrue(
                    tenantId, phaseType, group));
        }
        return allMappings.stream().map(this::toResponse).toList();
    }

    /**
     * Get template-phase mappings filtered by phase type and building type group.
     */
    public List<TemplatePhaseMappingResponse> getMappingsByPhaseAndBuildingType(
            String tenantId, String phaseType, BuildingTypeGroup buildingTypeGroup) {
        List<TemplatePhaseMapping> mappings = templatePhaseMappingRepository
            .findByTenantIdAndPhaseTypeAndBuildingTypeGroupAndEnabledTrue(
                tenantId, phaseType, buildingTypeGroup);
        return mappings.stream().map(this::toResponse).toList();
    }

    /**
     * Get a specific template-phase mapping by ID.
     */
    public TemplatePhaseMappingResponse getMapping(String tenantId, UUID mappingId) {
        return templatePhaseMappingRepository.findById(mappingId)
            .filter(m -> m.getTenantId().equals(tenantId))
            .map(this::toResponse)
            .orElse(null);
    }

    /**
     * Update a template-phase mapping.
     */
    public TemplatePhaseMappingResponse updateMapping(String tenantId, UUID mappingId, TemplatePhaseMappingRequest request) {
        return templatePhaseMappingRepository.findById(mappingId)
            .filter(m -> m.getTenantId().equals(tenantId))
            .map(mapping -> {
                mapping.setPhaseType(request.getPhaseType());
                mapping.setBuildingTypeGroup(request.getBuildingTypeGroup());
                mapping.setWorkflowTemplateCode(request.getWorkflowTemplateCode());
                mapping.setPriority(request.getPriority());
                mapping.setEnabled(request.getEnabled());
                TemplatePhaseMapping saved = templatePhaseMappingRepository.save(mapping);
                return toResponse(saved);
            })
            .orElse(null);
    }

    /**
     * Delete a template-phase mapping.
     */
    public boolean deleteMapping(String tenantId, UUID mappingId) {
        return templatePhaseMappingRepository.findById(mappingId)
            .filter(m -> m.getTenantId().equals(tenantId))
            .map(mapping -> {
                templatePhaseMappingRepository.delete(mapping);
                return true;
            })
            .orElse(false);
    }

    /**
     * Toggle the enabled status of a mapping.
     */
    public TemplatePhaseMappingResponse toggleMapping(String tenantId, UUID mappingId) {
        return templatePhaseMappingRepository.findById(mappingId)
            .filter(m -> m.getTenantId().equals(tenantId))
            .map(mapping -> {
                mapping.setEnabled(!mapping.getEnabled());
                TemplatePhaseMapping saved = templatePhaseMappingRepository.save(mapping);
                return toResponse(saved);
            })
            .orElse(null);
    }

    /**
     * Get all distinct phase types for a tenant.
     */
    public List<String> getPhaseTypes(String tenantId) {
        return templatePhaseMappingRepository.findDistinctPhaseTypesByTenantId(tenantId);
    }

    /**
     * Get all building type groups that have mappings.
     */
    public List<BuildingTypeGroup> getBuildingTypeGroups(String tenantId) {
        List<TemplatePhaseMapping> mappings = templatePhaseMappingRepository.findByTenantIdAndEnabledTrueOrderByPhaseType(tenantId);
        return mappings.stream()
            .map(TemplatePhaseMapping::getBuildingTypeGroup)
            .distinct()
            .toList();
    }

    /**
     * Convert a domain entity to a response DTO.
     */
    private TemplatePhaseMappingResponse toResponse(TemplatePhaseMapping mapping) {
        return new TemplatePhaseMappingResponse(
            mapping.getId(),
            mapping.getTenantId(),
            mapping.getPhaseType(),
            mapping.getBuildingTypeGroup(),
            mapping.getWorkflowTemplateCode(),
            mapping.getPriority(),
            mapping.getEnabled(),
            mapping.getCreatedAt(),
            mapping.getUpdatedAt());
    }
}
