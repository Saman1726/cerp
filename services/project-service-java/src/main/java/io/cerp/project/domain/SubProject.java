package io.cerp.project.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * SubProject entity representing a building or major building component as a sub-project within a Project.
 *
 * <p>
 * Each SubProject corresponds to a Building (or significant building component) on a Construction Site. Workflows are created per SubProject, and can be scoped to:
 * <ul>
 * <li>Whole building (buildingLevel = WHOLE)</li>
 * <li>Specific floor (buildingLevel = FLOOR, with associated floorIds)</li>
 * <li>Specific building part (buildingLevel = PART, with associated partIds)</li>
 * </ul>
 *
 * <p>
 * Per Section 4 (Business Concept) and Section 9 (DDD): SubProjects drive scoping for permissions, reporting, and workflow instantiation within a Project.
 */
@Entity
@Table(name = "sub_projects", indexes = {
        @Index(name = "idx_subproject_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_subproject_org_id", columnList = "org_id"),
        @Index(name = "idx_subproject_project_id", columnList = "project_id"),
        @Index(name = "idx_subproject_external_id", columnList = "external_id", unique = true),
        @Index(name = "idx_subproject_code", columnList = "code"),
        @Index(name = "idx_subproject_status", columnList = "status"),
        @Index(name = "idx_subproject_tenant_project", columnList = "tenant_id, project_id")
})
public class SubProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "external_id", length = 128)
    private String externalId;

    /**
     * SubProject code (e.g., "PRJ-2024-001-BLDG-A"). Must be unique within the project.
     */
    @Column(name = "code", nullable = false, length = 64)
    private String code;

    /**
     * SubProject/building name (e.g., "Residential Building A").
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * SubProject description.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Building type/classification.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "building_type", length = 64)
    private BuildingType buildingType;

    /**
     * The construction phase this SubProject is in (e.g., foundation, superstructure). Used to determine which workflow templates are applicable.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "construction_phase", length = 64)
    private ConstructionPhase constructionPhase;

    /**
     * Scope level of workflows for this SubProject. - WHOLE: workflows apply to the entire building - FLOOR: workflows apply to specific floors (see floorIds) - PART:
     * workflows apply to specific building parts (see partIds)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "building_level", nullable = false, length = 32)
    private BuildingLevel buildingLevel = BuildingLevel.WHOLE;

    /**
     * Floor identifiers that this SubProject workflow applies to. Used when buildingLevel = FLOOR.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "floor_ids")
    private List<String> floorIds = new ArrayList<>();

    /**
     * Building part identifiers (e.g., "facade", "basement", "roof", "elevator_shaft"). Used when buildingLevel = PART.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "part_ids")
    private List<String> partIds = new ArrayList<>();

    /**
     * Total usable area in square meters.
     */
    @Column(name = "usable_area", precision = 12, scale = 2)
    private BigDecimal usableArea;

    /**
     * Total gross area in square meters.
     */
    @Column(name = "total_area", precision = 12, scale = 2)
    private BigDecimal totalArea;

    /**
     * Number of floors in this building/SubProject.
     */
    @Column(name = "floor_count")
    private Integer floorCount;

    /**
     * Number of units/apartments in this building/SubProject.
     */
    @Column(name = "unit_count")
    private Integer unitCount;

    /**
     * SubProject status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SubProjectStatus status;

    /**
     * Start date of construction for this SubProject.
     */
    @Column(name = "start_date")
    private LocalDate startDate;

    /**
     * End/completion date of construction for this SubProject.
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Overall SubProject progress (0.00 to 100.00).
     */
    @Column(name = "progress", precision = 5, scale = 2)
    private BigDecimal progress;

    /**
     * SubProject budget.
     */
    @Column(name = "budget", precision = 18, scale = 2)
    private BigDecimal budget;

    /**
     * Budget currency (ISO 4217).
     */
    @Column(name = "currency_code", length = 8)
    private String currencyCode;

    /**
     * External system sync status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 32)
    private SyncStatus syncStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 128)
    private String createdBy;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;

    /**
     * Reference to the parent Project entity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, insertable = false, updatable = false)
    private Project project;

    // ── Constructors ──

    protected SubProject() {
    }

    public SubProject(String tenantId, UUID orgId, UUID projectId, String code, String name) {
        this.tenantId = tenantId;
        this.orgId = orgId;
        this.projectId = projectId;
        this.code = code;
        this.name = name;
        this.status = SubProjectStatus.PLANNING;
        this.syncStatus = SyncStatus.LOCAL;
        this.buildingLevel = BuildingLevel.WHOLE;
        this.progress = BigDecimal.ZERO;
        this.currencyCode = "EUR";
    }

    // ── Pre-persist/update hooks ──

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Business methods ──

    /**
     * Add a floor ID to this SubProject's floor list.
     */
    public void addFloorId(String floorId) {
        if (!this.floorIds.contains(floorId)) {
            this.floorIds.add(floorId);
        }
    }

    /**
     * Remove a floor ID from this SubProject's floor list.
     */
    public void removeFloorId(String floorId) {
        this.floorIds.remove(floorId);
    }

    /**
     * Add a building part ID to this SubProject's part list.
     */
    public void addPartId(String partId) {
        if (!this.partIds.contains(partId)) {
            this.partIds.add(partId);
        }
    }

    /**
     * Remove a building part ID from this SubProject's part list.
     */
    public void removePartId(String partId) {
        this.partIds.remove(partId);
    }

    /**
     * Calculate total usable area from floor areas.
     */
    public void recalculateAreas() {
        // This would typically aggregate from Floor entities if they existed as separate entities
        // For now, it's a placeholder for manual area updates
    }

    /**
     * Transition SubProject to construction started.
     */
    public void startConstruction() {
        requireStatus(SubProjectStatus.PLANNING);
        this.status = SubProjectStatus.IN_CONSTRUCTION;
        this.startDate = LocalDate.now();
    }

    /**
     * Transition SubProject to on hold.
     */
    public void hold() {
        requireStatus(SubProjectStatus.IN_CONSTRUCTION);
        this.status = SubProjectStatus.ON_HOLD;
    }

    /**
     * Resume SubProject from on hold.
     */
    public void resume() {
        requireStatus(SubProjectStatus.ON_HOLD);
        this.status = SubProjectStatus.IN_CONSTRUCTION;
    }

    /**
     * Complete the SubProject.
     */
    public void complete() {
        requireStatus(SubProjectStatus.IN_CONSTRUCTION, SubProjectStatus.ON_HOLD);
        this.status = SubProjectStatus.COMPLETED;
        this.endDate = LocalDate.now();
    }

    private void requireStatus(SubProjectStatus... allowed) {
        for (SubProjectStatus s : allowed) {
            if (this.status == s) {
                return;
            }
        }
        String allowedStr = String.join(", ", java.util.Arrays.toString(allowed));
        throw new IllegalStateException(
            String.format("Current status %s does not allow transition. Allowed from: %s", this.status, allowedStr));
    }

    // ── Getters and Setters ──

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(UUID orgId) {
        this.orgId = orgId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
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

    public BuildingType getBuildingType() {
        return buildingType;
    }

    public void setBuildingType(BuildingType buildingType) {
        this.buildingType = buildingType;
    }

    public ConstructionPhase getConstructionPhase() {
        return constructionPhase;
    }

    public void setConstructionPhase(ConstructionPhase constructionPhase) {
        this.constructionPhase = constructionPhase;
    }

    public BuildingLevel getBuildingLevel() {
        return buildingLevel;
    }

    public void setBuildingLevel(BuildingLevel buildingLevel) {
        this.buildingLevel = buildingLevel;
    }

    public List<String> getFloorIds() {
        return floorIds;
    }

    public void setFloorIds(List<String> floorIds) {
        this.floorIds = floorIds != null ? new ArrayList<>(floorIds) : new ArrayList<>();
    }

    public List<String> getPartIds() {
        return partIds;
    }

    public void setPartIds(List<String> partIds) {
        this.partIds = partIds != null ? new ArrayList<>(partIds) : new ArrayList<>();
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

    public SubProjectStatus getStatus() {
        return status;
    }

    public void setStatus(SubProjectStatus status) {
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

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
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

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    /**
     * SubProject lifecycle status.
     */
    public enum SubProjectStatus {
        PLANNING, IN_CONSTRUCTION, ON_HOLD, COMPLETED, CANCELLED
    }

    /**
     * Building type/classification.
     */
    public enum BuildingType {
        RESIDENTIAL, COMMERCIAL, INDUSTRIAL, MIXED_USE, PUBLIC, PARKING, UTILITY, INFRASTRUCTURE, OTHER
    }

    /**
     * Construction phase indicators for determining applicable workflows.
     */
    public enum ConstructionPhase {
        PRE_CONSTRUCTION, SITE_PREPARATION, FOUNDATION, BASEMENT, SUPERSTRUCTURE, ROOF, BUILDING_ENVELOPE, INTERIOR_PARTITIONS, MEP_ROUGH_IN, INTERIOR_FINISH, EXTERIOR_WORKS, TESTING_COMMISSIONING, SNAGGING, HANDOVER, FACILITY_MANAGEMENT
    }

    /**
     * Building level - determines the scope of workflows. - WHOLE: workflows apply to the entire building - FLOOR: workflows apply to specific floors - PART: workflows
     * apply to specific building parts
     */
    public enum BuildingLevel {
        WHOLE, FLOOR, PART
    }

    /**
     * Data source / sync status.
     */
    public enum SyncStatus {
        LOCAL, REMOTE, CONFLICT, PENDING_SYNC
    }
}
