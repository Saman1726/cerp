package io.cerp.workflow.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cerp.workflow.domain.OrganisationWorkflowConfig;
import io.cerp.workflow.dto.OrganisationWorkflowConfigRequest;
import io.cerp.workflow.dto.OrganisationWorkflowConfigResponse;
import io.cerp.workflow.repository.OrganisationWorkflowConfigRepository;

/**
 * Service for managing organisation workflow configurations.
 */
@Service
public class OrganisationWorkflowConfigService {

    private static final Logger log = LoggerFactory.getLogger(OrganisationWorkflowConfigService.class);

    private final OrganisationWorkflowConfigRepository organisationWorkflowConfigRepository;
    private final ObjectMapper objectMapper;

    public OrganisationWorkflowConfigService(OrganisationWorkflowConfigRepository organisationWorkflowConfigRepository,
            ObjectMapper objectMapper) {
        this.organisationWorkflowConfigRepository = organisationWorkflowConfigRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Get or create a workflow config for an organisation.
     */
    public OrganisationWorkflowConfigResponse getOrCreateConfig(String tenantId, UUID orgId) {
        Optional<OrganisationWorkflowConfig> configOpt =
                organisationWorkflowConfigRepository.findByTenantIdAndOrgId(tenantId, orgId);

        if (configOpt.isEmpty()) {
            log.info("Creating new workflow config for org {} (tenant: {})", orgId, tenantId);
            OrganisationWorkflowConfig config = new OrganisationWorkflowConfig(tenantId, orgId);
            config.setObjectMapper(objectMapper);
            config.setSelectedTemplateCodesJson("[]");
            config.setCustomPhaseMappingsJson("{}");
            config = organisationWorkflowConfigRepository.save(config);
            return toResponse(config);
        }

        return toResponse(configOpt.get());
    }

    /**
     * Update organisation workflow configuration.
     */
    @Transactional
    public OrganisationWorkflowConfigResponse updateConfig(String tenantId, UUID orgId,
            OrganisationWorkflowConfigRequest request) {
        Optional<OrganisationWorkflowConfig> configOpt =
                organisationWorkflowConfigRepository.findByTenantIdAndOrgId(tenantId, orgId);

        if (configOpt.isEmpty()) {
            throw new IllegalArgumentException(
                "No workflow config found for org " + orgId + " (tenant: " + tenantId + ")");
        }

        OrganisationWorkflowConfig config = configOpt.get();
        config.setObjectMapper(objectMapper);

        if (request.getAutoGenerateOnSubprojectCreation() != null) {
            if (request.getAutoGenerateOnSubprojectCreation()) {
                config.enableAutoGeneration();
            } else {
                config.disableAutoGeneration();
            }
        }

        if (request.getSelectedTemplateCodes() != null) {
            config.setSelectedTemplateCodes(request.getSelectedTemplateCodes());
        }

        if (request.getCustomPhaseMappings() != null) {
            config.setCustomPhaseMappings(request.getCustomPhaseMappings());
        }

        OrganisationWorkflowConfig saved = organisationWorkflowConfigRepository.save(config);
        return toResponse(saved);
    }

    /**
     * Enable auto-generation for an organisation.
     */
    @Transactional
    public OrganisationWorkflowConfigResponse enableAutoGeneration(String tenantId, UUID orgId) {
        Optional<OrganisationWorkflowConfig> configOpt =
                organisationWorkflowConfigRepository.findByTenantIdAndOrgId(tenantId, orgId);

        if (configOpt.isEmpty()) {
            throw new IllegalArgumentException(
                "No workflow config found for org " + orgId + " (tenant: " + tenantId + ")");
        }

        OrganisationWorkflowConfig config = configOpt.get();
        config.enableAutoGeneration();
        OrganisationWorkflowConfig saved = organisationWorkflowConfigRepository.save(config);
        return toResponse(saved);
    }

    /**
     * Disable auto-generation for an organisation.
     */
    @Transactional
    public OrganisationWorkflowConfigResponse disableAutoGeneration(String tenantId, UUID orgId) {
        Optional<OrganisationWorkflowConfig> configOpt =
                organisationWorkflowConfigRepository.findByTenantIdAndOrgId(tenantId, orgId);

        if (configOpt.isEmpty()) {
            throw new IllegalArgumentException(
                "No workflow config found for org " + orgId + " (tenant: " + tenantId + ")");
        }

        OrganisationWorkflowConfig config = configOpt.get();
        config.disableAutoGeneration();
        OrganisationWorkflowConfig saved = organisationWorkflowConfigRepository.save(config);
        return toResponse(saved);
    }

    /**
     * Set selected template codes for an organisation.
     */
    @Transactional
    public OrganisationWorkflowConfigResponse setSelectedTemplateCodes(String tenantId, UUID orgId,
            List<String> templateCodes) {
        Optional<OrganisationWorkflowConfig> configOpt =
                organisationWorkflowConfigRepository.findByTenantIdAndOrgId(tenantId, orgId);

        if (configOpt.isEmpty()) {
            throw new IllegalArgumentException(
                "No workflow config found for org " + orgId + " (tenant: " + tenantId + ")");
        }

        OrganisationWorkflowConfig config = configOpt.get();
        config.setObjectMapper(objectMapper);
        config.setSelectedTemplateCodes(templateCodes);
        OrganisationWorkflowConfig saved = organisationWorkflowConfigRepository.save(config);
        return toResponse(saved);
    }

    /**
     * Set custom phase mappings for an organisation.
     */
    @Transactional
    public OrganisationWorkflowConfigResponse setCustomPhaseMappings(String tenantId, UUID orgId,
            Map<String, Map<String, List<String>>> customMappings) {
        Optional<OrganisationWorkflowConfig> configOpt =
                organisationWorkflowConfigRepository.findByTenantIdAndOrgId(tenantId, orgId);

        if (configOpt.isEmpty()) {
            throw new IllegalArgumentException(
                "No workflow config found for org " + orgId + " (tenant: " + tenantId + ")");
        }

        OrganisationWorkflowConfig config = configOpt.get();
        config.setObjectMapper(objectMapper);
        config.setCustomPhaseMappings(customMappings);
        OrganisationWorkflowConfig saved = organisationWorkflowConfigRepository.save(config);
        return toResponse(saved);
    }

    /**
     * Check if auto-generation is enabled for an organisation.
     */
    public boolean isAutoGenerationEnabled(String tenantId, UUID orgId) {
        return organisationWorkflowConfigRepository.findByTenantIdAndOrgId(tenantId, orgId)
            .map(OrganisationWorkflowConfig::getAutoGenerateOnSubprojectCreation)
            .orElse(false);
    }

    /**
     * Convert a domain entity to a response DTO.
     */
    private OrganisationWorkflowConfigResponse toResponse(OrganisationWorkflowConfig config) {
        OrganisationWorkflowConfigResponse response = new OrganisationWorkflowConfigResponse();
        response.setId(config.getId());
        response.setTenantId(config.getTenantId());
        response.setOrgId(config.getOrgId());
        response.setAutoGenerateOnSubprojectCreation(config.getAutoGenerateOnSubprojectCreation());
        response.setCreatedAt(config.getCreatedAt());
        response.setUpdatedAt(config.getUpdatedAt());

        // Deserialize JSON fields
        config.setObjectMapper(objectMapper);
        response.setSelectedTemplateCodes(config.getSelectedTemplateCodes());
        response.setCustomPhaseMappings(config.getCustomPhaseMappings());

        return response;
    }
}
