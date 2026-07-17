package io.cerp.workflow.event;

/**
 * Domain event emitted when an active workflow template is deprecated. Status transition: ACTIVE → DEPRECATED.
 */
public class WorkflowTemplateDeprecated extends WorkflowEvent {

    private final String code;
    private final Integer templateVersion;

    public WorkflowTemplateDeprecated(String code, Integer templateVersion) {
        super("WorkflowTemplateDeprecated", code);
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
