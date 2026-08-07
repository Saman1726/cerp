package io.cerp.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Resource-quota limits for a given Plan. One row per Plan (exactly 3, ever) - the enum itself is the primary key, since every lookup is by plan and a surrogate id would go
 * unused. NULL in any max_* column means "no cap".
 */
@Entity
@Table(name = "plan_limits", schema = "identity")
public class PlanLimits {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, updatable = false)
    private Plan plan;

    @Column(name = "max_construction_sites")
    private Integer maxConstructionSites;

    @Column(name = "max_buildings")
    private Integer maxBuildings;

    @Column(name = "max_owners_per_project")
    private Integer maxOwnersPerProject;

    @Column(name = "max_managers_per_project")
    private Integer maxManagersPerProject;

    @Column(name = "max_members_per_project")
    private Integer maxMembersPerProject;

    @Column(name = "starting_bid_grant", nullable = false)
    private Long startingBidGrant;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public PlanLimits() {
    }

    public Plan getPlan() {
        return plan;
    }

    public PlanLimits setPlan(Plan plan) {
        this.plan = plan;
        return this;
    }

    public Integer getMaxConstructionSites() {
        return maxConstructionSites;
    }

    public PlanLimits setMaxConstructionSites(Integer maxConstructionSites) {
        this.maxConstructionSites = maxConstructionSites;
        return this;
    }

    public Integer getMaxBuildings() {
        return maxBuildings;
    }

    public PlanLimits setMaxBuildings(Integer maxBuildings) {
        this.maxBuildings = maxBuildings;
        return this;
    }

    public Integer getMaxOwnersPerProject() {
        return maxOwnersPerProject;
    }

    public PlanLimits setMaxOwnersPerProject(Integer maxOwnersPerProject) {
        this.maxOwnersPerProject = maxOwnersPerProject;
        return this;
    }

    public Integer getMaxManagersPerProject() {
        return maxManagersPerProject;
    }

    public PlanLimits setMaxManagersPerProject(Integer maxManagersPerProject) {
        this.maxManagersPerProject = maxManagersPerProject;
        return this;
    }

    public Integer getMaxMembersPerProject() {
        return maxMembersPerProject;
    }

    public PlanLimits setMaxMembersPerProject(Integer maxMembersPerProject) {
        this.maxMembersPerProject = maxMembersPerProject;
        return this;
    }

    public Long getStartingBidGrant() {
        return startingBidGrant;
    }

    public PlanLimits setStartingBidGrant(Long startingBidGrant) {
        this.startingBidGrant = startingBidGrant;
        return this;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public PlanLimits setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
}
