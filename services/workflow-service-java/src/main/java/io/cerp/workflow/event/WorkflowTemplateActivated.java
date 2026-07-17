package io.cerp.workflow.event;

/**
 * Domain event emitted when a draft workflow template is activated. Status transition: DRAFT → ACTIVE.
 */
public class WorkflowTemplateActivated extends WorkflowEvent {

    private final String code;
    private final Integer templateVersion;

    public WorkflowTemplateActivated(String code, Integer templateVersion) {
        super("WorkflowTemplateActivated", code);
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
