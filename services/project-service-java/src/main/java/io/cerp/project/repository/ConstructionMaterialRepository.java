package io.cerp.project.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.cerp.project.domain.ConstructionMaterial;
import io.cerp.project.domain.ConstructionMaterial.PhysicalObjectType;

@Repository
public interface ConstructionMaterialRepository extends JpaRepository<ConstructionMaterial, UUID> {

    List<ConstructionMaterial> findByConstructionSiteId(UUID constructionSiteId);

    List<ConstructionMaterial> findByConstructionSiteIdAndPhysicalObjectType(
            UUID constructionSiteId, PhysicalObjectType physicalObjectType);

    Optional<ConstructionMaterial> findByConstructionSiteIdAndMaterialServiceCodeAndPhysicalObjectTypeAndPhysicalObjectId(
            UUID constructionSiteId, String materialServiceCode,
            PhysicalObjectType physicalObjectType, UUID physicalObjectId);
}