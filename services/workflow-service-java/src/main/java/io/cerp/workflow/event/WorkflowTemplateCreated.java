package io.cerp.workflow.event;

import java.util.List;
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
        super("WorkflowTemplateCreated", code);
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
}
