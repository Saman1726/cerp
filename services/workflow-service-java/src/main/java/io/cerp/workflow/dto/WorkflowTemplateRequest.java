package io.cerp.workflow.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for creating/updating a WorkflowTemplate.
 */
public record WorkflowTemplateRequest(
        @NotBlank(message = "code is required") @Pattern(regexp = "^[A-Z_]+$", message = "code must be uppercase with underscores") String code,

        @NotBlank(message = "name is required") String name,

        String description,

        @NotBlank(message = "phaseType is required") @Pattern(regexp = "^[A-Z_]+$", message = "phaseType must be uppercase with underscores") String phaseType,

        String parametersSchema,

        String defaultSla,

        List<String> requiredDocuments,

        List<WorkflowTemplateStepRequest> steps,

        Integer orderNum) {
}
