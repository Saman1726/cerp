package io.cerp.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.cerp.project.domain.SubProject;

/**
 * Request body for updating an existing SubProject (building).
 *
 * <p>
 * All fields are optional - only provided fields will be updated.
 */
public class SubProjectUpdateRequest {

    private String name;
    private String description;
    private SubProject.BuildingType buildingType;
    private SubProject.ConstructionPhase constructionPhase;
    private SubProject.BuildingLevel buildingLevel;
    private List<String> floorIds;
    private List<String> partIds;
    private BigDecimal usableArea;
    private BigDecimal totalArea;
    private Integer floorCount;
    private Integer unitCount;
    private SubProject.SubProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal progress;
    private BigDecimal budget;
    private String currencyCode;

    // ── Constructors ──

    public SubProjectUpdateRequest() {
    }

    // ── Getters and Setters ──

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

    public SubProject.SubProjectStatus getStatus() {
        return status;
    }

    public void setStatus(SubProject.SubProjectStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getProgress() {
        return progress;
    }

    public void setProgress(BigDecimal progress) {
        this.progress = progress;
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
}
