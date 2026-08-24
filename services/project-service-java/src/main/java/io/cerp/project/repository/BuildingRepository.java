package io.cerp.project.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.cerp.project.domain.Building;

@Repository
public interface BuildingRepository extends JpaRepository<Building, UUID> {

    List<Building> findByConstructionSiteId(UUID constructionSiteId);

    Optional<Building> findByConstructionSiteIdAndName(UUID constructionSiteId, String name);

    List<Building> findByConstructionSiteIdOrderByNameAsc(UUID constructionSiteId);

    /**
     * Counts a creator's Buildings for quota enforcement. Building has no createdBy of its own - join through its ConstructionSite (whoever created the site is
     * attributed as the creator of the buildings added to it).
     */
    @Query("SELECT COUNT(b) FROM Building b WHERE b.constructionSite.createdBy = :userId")
    long countByConstructionSiteCreatedBy(@Param("userId") String userId);
}
