package io.cerp.workflow.dto;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for workflow generation operations.
 */
public class WorkflowGenerationResponse {

    private UUID subprojectId;
    private String tenantId;
    private int totalWorkflowsGenerated;
    private int skippedCount;
    private List<GeneratedWorkflowDetail> generatedWorkflows;
    private List<SkippedWorkflowDetail> skippedDetails;

    // ── Constructors ──

    public WorkflowGenerationResponse() {
    }

    public WorkflowGenerationResponse(UUID subprojectId, String tenantId, int totalWorkflowsGenerated,
            int skippedCount, List<GeneratedWorkflowDetail> generatedWorkflows,
            List<SkippedWorkflowDetail> skippedDetails) {
        this.subprojectId = subprojectId;
        this.tenantId = tenantId;
        this.totalWorkflowsGenerated = totalWorkflowsGenerated;
        this.skippedCount = skippedCount;
        this.generatedWorkflows = generatedWorkflows;
        this.skippedDetails = skippedDetails;
    }

    // ── Getters and Setters ──

    public UUID getSubprojectId() {
        return subprojectId;
    }

    public void setSubprojectId(UUID subprojectId) {
        this.subprojectId = subprojectId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public int getTotalWorkflowsGenerated() {
        return totalWorkflowsGenerated;
    }

    public void setTotalWorkflowsGenerated(int totalWorkflowsGenerated) {
        this.totalWorkflowsGenerated = totalWorkflowsGenerated;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    /**
     * Alias for getTotalWorkflowsGenerated() - returns the number of workflows generated.
     */
    public int getGeneratedCount() {
        return totalWorkflowsGenerated;
    }

    public List<GeneratedWorkflowDetail> getGeneratedWorkflows() {
        return generatedWorkflows;
    }

    public void setGeneratedWorkflows(List<GeneratedWorkflowDetail> generatedWorkflows) {
        this.generatedWorkflows = generatedWorkflows;
    }

    public List<SkippedWorkflowDetail> getSkippedDetails() {
        return skippedDetails;
    }

    public void setSkippedDetails(List<SkippedWorkflowDetail> skippedDetails) {
        this.skippedDetails = skippedDetails;
    }

    // ── Nested DTOs ──

    public static class GeneratedWorkflowDetail {
        private UUID workflowId;
        private String templateCode;
        private String phaseType;
        private String workflowName;
        private String status;

        public GeneratedWorkflowDetail() {
        }

        public GeneratedWorkflowDetail(UUID workflowId, String templateCode, String phaseType,
                String workflowName, String status) {
            this.workflowId = workflowId;
            this.templateCode = templateCode;
            this.phaseType = phaseType;
            this.workflowName = workflowName;
            this.status = status;
        }

        public UUID getWorkflowId() {
            return workflowId;
        }

        public void setWorkflowId(UUID workflowId) {
            this.workflowId = workflowId;
        }

        public String getTemplateCode() {
            return templateCode;
        }

        public void setTemplateCode(String templateCode) {
            this.templateCode = templateCode;
        }

        public String getPhaseType() {
            return phaseType;
        }

        public void setPhaseType(String phaseType) {
            this.phaseType = phaseType;
        }

        public String getWorkflowName() {
            return workflowName;
        }

        public void setWorkflowName(String workflowName) {
            this.workflowName = workflowName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class SkippedWorkflowDetail {
        private String templateCode;
        private String phaseType;
        private String reason;

        public SkippedWorkflowDetail() {
        }

        public SkippedWorkflowDetail(String templateCode, String phaseType, String reason) {
            this.templateCode = templateCode;
            this.phaseType = phaseType;
            this.reason = reason;
        }

        public String getTemplateCode() {
            return templateCode;
        }

        public void setTemplateCode(String templateCode) {
            this.templateCode = templateCode;
        }

        public String getPhaseType() {
            return phaseType;
        }

        public void setPhaseType(String phaseType) {
            this.phaseType = phaseType;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
