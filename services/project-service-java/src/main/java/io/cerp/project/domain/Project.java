package io.cerp.project.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Project entity representing a construction project or project portfolio.
 *
 * <p>
 * Each project belongs to an organization (org_id) and is scoped by tenant (tenant_id). Projects own construction-sites, procurement events, and support billing/cost
 * tracking.
 *
 * <p>
 * Per Section 4 (Business Concept): Projects are the primary work breakdown unit under properties/organizations, containing sites, activities, and contracts.
 */
@Entity
@Table(name = "projects", schema = "project", indexes = {
        @Index(name = "idx_projects_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_projects_org_id", columnList = "org_id"),
        @Index(name = "idx_projects_external_id", columnList = "external_id", unique = true),
        @Index(name = "idx_projects_code", columnList = "code"),
        @Index(name = "idx_projects_status", columnList = "status"),
        @Index(name = "idx_projects_tenant_org", columnList = "tenant_id, org_id")
})
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "external_id", length = 128)
    private String externalId;

    /**
     * Project code/number (e.g., "PRJ-2024-001"). Must be unique within the organization.
     */
    @Column(name = "code", nullable = false, length = 64)
    private String code;

    /**
     * Project name.
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * Project description.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Reference to a construction site this project is assigned to (nullable). Each construction site can only have one active project.
     */
    @Column(name = "construction_site_id", length = 64)
    private String constructionSiteId;

    /**
     * Reference to a property this project belongs to (nullable).
     */
    @Column(name = "property_id")
    private UUID propertyId;

    /**
     * Project location address line 1.
     */
    @Column(name = "address_line_1", length = 255)
    private String addressLine1;

    /**
     * Project location address line 2.
     */
    @Column(name = "address_line_2", length = 255)
    private String addressLine2;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "city", length = 128)
    private String city;

    @Column(name = "country_code", length = 4)
    private String countryCode;

    /**
     * Project status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ProjectStatus status;

    /**
     * Project type/classification.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", length = 64)
    private ProjectType projectType;

    /**
     * Planned start date.
     */
    @Column(name = "start_date")
    private LocalDate startDate;

    /**
     * Planned end/due date.
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Actual start date.
     */
    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    /**
     * Actual end/completion date.
     */
    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    /**
     * Overall project progress (0.00 to 100.00).
     */
    @Column(name = "progress", precision = 5, scale = 2)
    private BigDecimal progress;

    /**
     * Total budget (amount in project currency).
     */
    @Column(name = "budget", precision = 18, scale = 2)
    private BigDecimal budget;

    /**
     * Project currency (ISO 4217).
     */
    @Column(name = "currency_code", length = 8)
    private String currencyCode;

    /**
     * Client/customer name (may differ from org name).
     */
    @Column(name = "client_name", length = 255)
    private String clientName;

    /**
     * Client contact email.
     */
    @Column(name = "client_email", length = 255)
    private String clientEmail;

    /**
     * Client contact phone.
     */
    @Column(name = "client_phone", length = 32)
    private String clientPhone;

    /**
     * Whether this project is soft-deleted/archived. Soft-deleted projects do not occupy the construction site link.
     */
    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false;

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
     * SubProjects (buildings) belonging to this Project.
     */
    @OneToMany(mappedBy = "project", fetch = FetchType.EAGER)
    private java.util.List<SubProject> subProjects = new java.util.ArrayList<>();

    /**
     * List of user IDs assigned to this project (e.g., vendors who should see this project).
     */
    @Column(name = "assigned_user_ids", columnDefinition = "text[]")
    private String[] assignedUserIds = new String[0];

    /**
     * User roles mapping for this project. JSONB format: {"user@example.com": "VIEWER", "admin@example.com": "ADMIN"}. Roles: VIEWER (read-only), EDITOR (can modify),
     * ADMIN (full control including user management).
     */
    @Column(name = "assigned_user_roles", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private java.util.Map<String, String> assignedUserRoles = new java.util.HashMap<>();

    // ── Constructors ──

    protected Project() {
    }

    public Project(String tenantId, UUID orgId, String code, String name) {
        this.tenantId = tenantId;
        this.orgId = orgId;
        this.code = code;
        this.name = name;
        this.status = ProjectStatus.PLANNING;
        this.syncStatus = SyncStatus.LOCAL;
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

    public UUID getPropertyId() {
        return propertyId;
    }

    public String getConstructionSiteId() {
        return constructionSiteId;
    }

    public void setConstructionSiteId(String constructionSiteId) {
        this.constructionSiteId = constructionSiteId;
    }

    public void setPropertyId(UUID propertyId) {
        this.propertyId = propertyId;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public ProjectType getProjectType() {
        return projectType;
    }

    public void setProjectType(ProjectType projectType) {
        this.projectType = projectType;
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

    public LocalDate getActualStartDate() {
        return actualStartDate;
    }

    public void setActualStartDate(LocalDate actualStartDate) {
        this.actualStartDate = actualStartDate;
    }

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }

    public void setActualEndDate(LocalDate actualEndDate) {
        this.actualEndDate = actualEndDate;
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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getClientPhone() {
        return clientPhone;
    }

    public void setClientPhone(String clientPhone) {
        this.clientPhone = clientPhone;
    }

    public SyncStatus getSyncStatus() {
        return syncStatus;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
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

    public java.util.List<SubProject> getSubProjects() {
        return subProjects;
    }

    public void setSubProjects(java.util.List<SubProject> subProjects) {
        this.subProjects = subProjects;
    }

    public String[] getAssignedUserIds() {
        return assignedUserIds;
    }

    public void setAssignedUserIds(String[] assignedUserIds) {
        this.assignedUserIds = assignedUserIds;
    }

    public java.util.Map<String, String> getAssignedUserRoles() {
        return assignedUserRoles;
    }

    public void setAssignedUserRoles(java.util.Map<String, String> assignedUserRoles) {
        this.assignedUserRoles = assignedUserRoles;
    }

    /**
     * Project lifecycle status.
     */
    public enum ProjectStatus {
        PLANNING, ACTIVE, ON_HOLD, COMPLETED, CANCELLED, SUSPENDED
    }

    /**
     * Project type/classification.
     */
    public enum ProjectType {
        NEW_CONSTRUCTION, RENOVATION, MAINTENANCE, INFRASTRUCTURE, COMMERCIAL, RESIDENTIAL, INDUSTRIAL
    }

    /**
     * Data source / sync status.
     */
    public enum SyncStatus {
        LOCAL, REMOTE, CONFLICT, PENDING_SYNC
    }
}
