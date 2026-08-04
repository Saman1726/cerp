package io.cerp.workflow.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Domain event emitted when a new workflow template is created.
 */
public class WorkflowTemplateCreated extends WorkflowEvent {

    private final UUID templateId;
    private final String code;
    private final String name;
    private final String phaseType;
    private final Integer templateVersion;
    private final String parametersSchema;
    private final List<String> requiredDocuments;

    public WorkflowTemplateCreated(UUID templateId, String code, String name,
            String phaseType, Integer templateVersion, String parametersSchema, List<String> requiredDocuments) {
        super(WorkflowEvent.builder()
            .eventType("WorkflowTemplateCreated")
            .sagaType("WorkflowTemplate")
            .sagaId(null)
            .aggregateId(null)
            .aggregateType("WorkflowTemplate")
            .tenantId(null)
            .userId(null)
            .data(buildData(templateId, code, name, phaseType, templateVersion, parametersSchema, requiredDocuments)));
        this.templateId = templateId;
        this.code = code;
        this.name = name;
        this.phaseType = phaseType;
        this.templateVersion = templateVersion;
        this.parametersSchema = parametersSchema;
        this.requiredDocuments = requiredDocuments;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getPhaseType() {
        return phaseType;
    }

    public Integer getTemplateVersion() {
        return templateVersion;
    }

    public String getParametersSchema() {
        return parametersSchema;
    }

    public List<String> getRequiredDocuments() {
        return requiredDocuments;
    }

    private static Map<String, Object> buildData(UUID templateId, String code, String name,
            String phaseType, Integer templateVersion, String parametersSchema, List<String> requiredDocuments) {
        Map<String, Object> data = new HashMap<>();
        data.put("templateId", templateId);
        data.put("code", code);
        data.put("name", name);
        data.put("phaseType", phaseType);
        data.put("templateVersion", templateVersion);
        if (parametersSchema != null) {
            data.put("parametersSchema", parametersSchema);
        }
        if (requiredDocuments != null) {
            data.put("requiredDocuments", requiredDocuments);
        }
        return data;
    }
}
