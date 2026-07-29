package io.cerp.workflow.event;

import java.util.Map;

/**
 * Domain event emitted when an active workflow template is deprecated. Status transition: ACTIVE → DEPRECATED.
 */
public class WorkflowTemplateDeprecated extends WorkflowEvent {

    private final String code;
    private final Integer templateVersion;

    public WorkflowTemplateDeprecated(String code, Integer templateVersion) {
        super(WorkflowEvent.builder()
            .eventType("WorkflowTemplateDeprecated")
            .sagaType("WorkflowTemplate")
            .sagaId(null)
            .aggregateId(null)
            .aggregateType("WorkflowTemplate")
            .tenantId(null)
            .userId(null)
            .data(Map.of(
                "code", code,
                "templateVersion", templateVersion)));
        this.code = code;
        this.templateVersion = templateVersion;
    }

    public String getCode() {
        return code;
    }

    public Integer getTemplateVersion() {
        return templateVersion;
    }
}
