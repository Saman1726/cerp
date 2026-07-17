package io.cerp.workflow.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

/**
 * Request DTO for a WorkflowTemplateStep.
 */
public record WorkflowTemplateStepRequest(
        @NotNull(message = "stepOrder is required") @Min(value = 1, message = "stepOrder must be >= 1") Integer stepOrder,

        @NotBlank(message = "code is required") String code,

        @NotBlank(message = "name is required") String name,

        @NotNull(message = "stepType is required") StepType type,

        List<String> assigneeRoles,

        Boolean requiresEvidence,

        Boolean requiresApproval,

        Integer dueOffsetDays,

        Integer slaHours,

        BigDecimal weight,

        String checklists,

        String gatingCondition) {

    public enum StepType {
        TASK, INSPECTION, APPROVAL, MILESTONE
    }
}
