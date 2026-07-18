package io.cerp.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.cerp.project.domain.Building;

@Repository
public interface BuildingRepository extends JpaRepository<Building, UUID> {

    List<Building> findByConstructionSiteId(UUID constructionSiteId);
}
