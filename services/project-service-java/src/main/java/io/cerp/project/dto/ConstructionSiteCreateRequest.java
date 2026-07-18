package io.cerp.project.dto;

/**
 * Request DTO for creating a construction site.
 */
public record ConstructionSiteCreateRequest(
        String name,
        String address) {
}
