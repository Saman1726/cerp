package io.cerp.project.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.cerp.project.domain.SubProject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating a new SubProject (building).
 *
 * <p>
 * Each SubProject represents a Building or major building component within a Project. Workflows are scoped to the SubProject level and can target the whole building,
 * specific floors, or specific building parts.
 */
public class SubProjectCreateRequest {

    /**
     * SubProject code (e.g., "PRJ-2024-001-BLDG-A"). Must be unique within the project.
     */
    @NotBlank(message = "Code is required")
    private String code;

    /**
     * SubProject/building name (e.g., "Residential Building A").
     */
    @NotBlank(message = "Name is required")
    private String name;

    /**
     * SubProject description.
     */
    private String description;

    /**
     * Building type/classification.
     */
    private SubProject.BuildingType buildingType;

    /**
     * Construction phase for determining applicable workflows.
     */
    private SubProject.ConstructionPhase constructionPhase;

    /**
     * Scope level for workflows: WHOLE, FLOOR, or PART.
     */
    @NotNull(message = "Building level is required")
    private SubProject.BuildingLevel buildingLevel;

    /**
     * Floor identifiers that this SubProject workflow applies to. Used when buildingLevel = FLOOR.
     */
    private List<String> floorIds;

    /**
     * Building part identifiers (e.g., "facade", "basement", "roof"). Used when buildingLevel = PART.
     */
    private List<String> partIds;

    /**
     * Total usable area in square meters.
     */
    private BigDecimal usableArea;

    /**
     * Total gross area in square meters.
     */
    private BigDecimal totalArea;

    /**
     * Number of floors in this building/SubProject.
     */
    private Integer floorCount;

    /**
     * Number of units/apartments in this building/SubProject.
     */
    private Integer unitCount;

    /**
     * SubProject budget.
     */
    private BigDecimal budget;

    /**
     * Budget currency (ISO 4217).
     */
    private String currencyCode;

    /**
     * External system identifier.
     */
    private String externalId;

    // ── Constructors ──

    public SubProjectCreateRequest() {
    }

    // ── Getters and Setters ──

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SubProject.BuildingType getBuildingType() {
        return buildingType;
    }

    public void setBuildingType(SubProject.BuildingType buildingType) {
        this.buildingType = buildingType;
    }

    public SubProject.ConstructionPhase getConstructionPhase() {
        return constructionPhase;
    }

    public void setConstructionPhase(SubProject.ConstructionPhase constructionPhase) {
        this.constructionPhase = constructionPhase;
    }

    public SubProject.BuildingLevel getBuildingLevel() {
        return buildingLevel;
    }

    public void setBuildingLevel(SubProject.BuildingLevel buildingLevel) {
        this.buildingLevel = buildingLevel;
    }

    public List<String> getFloorIds() {
        return floorIds;
    }

    public void setFloorIds(List<String> floorIds) {
        this.floorIds = floorIds;
    }

    public List<String> getPartIds() {
        return partIds;
    }

    public void setPartIds(List<String> partIds) {
        this.partIds = partIds;
    }

    public BigDecimal getUsableArea() {
        return usableArea;
    }

    public void setUsableArea(BigDecimal usableArea) {
        this.usableArea = usableArea;
    }

    public BigDecimal getTotalArea() {
        return totalArea;
    }

    public void setTotalArea(BigDecimal totalArea) {
        this.totalArea = totalArea;
    }

    public Integer getFloorCount() {
        return floorCount;
    }

    public void setFloorCount(Integer floorCount) {
        this.floorCount = floorCount;
    }

    public Integer getUnitCount() {
        return unitCount;
    }

    public void setUnitCount(Integer unitCount) {
        this.unitCount = unitCount;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
}
