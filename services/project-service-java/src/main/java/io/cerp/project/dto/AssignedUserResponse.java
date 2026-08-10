package io.cerp.project.dto;

/**
 * A project assignment enriched with the assignee's email, for display. userId is whatever is actually stored in Project.assignedUserIds (a UUID for
 * assignments made after the identity-service sync fix, potentially a raw email for older data); email is always the best available human-readable form.
 */
public record AssignedUserResponse(String userId, String email, String role) {
}
