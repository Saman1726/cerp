package io.cerp.project.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for assigning users to a project with roles.
 */
public record ProjectUserAssignmentRequest(
        @NotEmpty(message = "Assignments list cannot be empty") List<UserAssignment> assignments) {

    /**
     * Individual user assignment with role.
     */
    public record UserAssignment(
            @NotBlank(message = "User ID (email) is required") String userId,

            @Pattern(regexp = "^(OWNER|MANAGER|SUPERVISOR|VIEWER)$", message = "Role must be one of: OWNER, MANAGER, SUPERVISOR, VIEWER") String role) {
    }
}
