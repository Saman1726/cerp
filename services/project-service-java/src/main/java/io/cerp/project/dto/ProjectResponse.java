package io.cerp.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
import java.util.HashMap;

import io.cerp.project.domain.Project;

/**
 * Response DTO for project data.
 */
public record ProjectResponse(
        UUID id,
        String tenantId,
        UUID orgId,
        String externalId,
        String code,
        String name,
        String description,
        String constructionSiteId,
        UUID propertyId,
        String addressLine1,
        String addressLine2,
        String postalCode,
        String city,
        String countryCode,
        String status,
        String projectType,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate actualStartDate,
        LocalDate actualEndDate,
        BigDecimal progress,
        BigDecimal budget,
        String currencyCode,
        String clientName,
        String clientEmail,
        String clientPhone,
        String syncStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> assignedUserIds,
        Map<String, String> assignedUserRoles,
        List<SubProjectResponse> subProjects) {
    public static ProjectResponse from(Project p) {
        List<String> assignedUserIds = p.getAssignedUserIds() != null
                ? Arrays.asList(p.getAssignedUserIds())
                : List.of();
        Map<String, String> assignedUserRoles = p.getAssignedUserRoles() != null
                ? new HashMap<>(p.getAssignedUserRoles())
                : Map.of();
        return new ProjectResponse(
            p.getId(),
            p.getTenantId(),
            p.getOrgId(),
            p.getExternalId(),
            p.getCode(),
            p.getName(),
            p.getDescription(),
            p.getConstructionSiteId(),
            p.getPropertyId(),
            p.getAddressLine1(),
            p.getAddressLine2(),
            p.getPostalCode(),
            p.getCity(),
            p.getCountryCode(),
            p.getStatus() != null ? p.getStatus().name() : null,
            p.getProjectType() != null ? p.getProjectType().name() : null,
            p.getStartDate(),
            p.getEndDate(),
            p.getActualStartDate(),
            p.getActualEndDate(),
            p.getProgress(),
            p.getBudget(),
            p.getCurrencyCode(),
            p.getClientName(),
            p.getClientEmail(),
            p.getClientPhone(),
            p.getSyncStatus() != null ? p.getSyncStatus().name() : null,
            p.getCreatedAt(),
            p.getUpdatedAt(),
            assignedUserIds,
            assignedUserRoles,
            p.getSubProjects() != null ? p.getSubProjects().stream()
                .filter(sp -> !Boolean.TRUE.equals(sp.getDeleted()))
                .map(SubProjectResponse::fromEntity)
                .toList() : List.of());
    }
}
