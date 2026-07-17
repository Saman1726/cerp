package io.cerp.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import io.cerp.project.domain.SubProject;

/**
 * Response DTO for SubProject (building) data.
 *
 * <p>
 * Contains all information about a SubProject including its associated floor IDs and building part IDs for workflow scoping.
 */
public class SubProjectResponse {

    private String id;
    private String tenantId;
    private String orgId;
    private String projectId;
    private String externalId;
    private String code;
    private String name;
    private String description;
    private String buildingType;
    private String constructionPhase;
    private String buildingLevel;
    private List<String> floorIds;
    private List<String> partIds;
    private BigDecimal usableArea;
    private BigDecimal totalArea;
    private Integer floorCount;
    private Integer unitCount;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal progress;
    private BigDecimal budget;
    private String currencyCode;
    private String syncStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    // ── Constructors ──

    public SubProjectResponse() {
    }

    /**
     * Create a response from a SubProject entity.
     */
    public static SubProjectResponse fromEntity(SubProject subProject) {
        SubProjectResponse response = new SubProjectResponse();
        response.setId(subProject.getId() != null ? subProject.getId().toString() : null);
        response.setTenantId(subProject.getTenantId());
        response.setOrgId(subProject.getOrgId() != null ? subProject.getOrgId().toString() : null);
        response.setProjectId(subProject.getProjectId() != null ? subProject.getProjectId().toString() : null);
        response.setExternalId(subProject.getExternalId());
        response.setCode(subProject.getCode());
        response.setName(subProject.getName());
        response.setDescription(subProject.getDescription());
        response.setBuildingType(subProject.getBuildingType() != null ? subProject.getBuildingType().name() : null);
        response.setConstructionPhase(subProject.getConstructionPhase() != null ? subProject.getConstructionPhase().name() : null);
        response.setBuildingLevel(subProject.getBuildingLevel() != null ? subProject.getBuildingLevel().name() : null);
        response.setFloorIds(subProject.getFloorIds());
        response.setPartIds(subProject.getPartIds());
        response.setUsableArea(subProject.getUsableArea());
        response.setTotalArea(subProject.getTotalArea());
        response.setFloorCount(subProject.getFloorCount());
        response.setUnitCount(subProject.getUnitCount());
        response.setStatus(subProject.getStatus() != null ? subProject.getStatus().name() : null);
        response.setStartDate(subProject.getStartDate());
        response.setEndDate(subProject.getEndDate());
        response.setProgress(subProject.getProgress());
        response.setBudget(subProject.getBudget());
        response.setCurrencyCode(subProject.getCurrencyCode());
        response.setSyncStatus(subProject.getSyncStatus() != null ? subProject.getSyncStatus().name() : null);
        response.setCreatedAt(subProject.getCreatedAt());
        response.setUpdatedAt(subProject.getUpdatedAt());
        response.setCreatedBy(subProject.getCreatedBy());
        response.setUpdatedBy(subProject.getUpdatedBy());
        return response;
    }

    // ── Getters and Setters ──

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

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

    public String getBuildingType() {
        return buildingType;
    }

    public void setBuildingType(String buildingType) {
        this.buildingType = buildingType;
    }

    public String getConstructionPhase() {
        return constructionPhase;
    }

    public void setConstructionPhase(String constructionPhase) {
        this.constructionPhase = constructionPhase;
    }

    public String getBuildingLevel() {
        return buildingLevel;
    }

    public void setBuildingLevel(String buildingLevel) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
