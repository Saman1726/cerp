package io.cerp.project.repository;

import io.cerp.project.domain.ConstructionSiteIfcFile;
import io.cerp.project.domain.ConstructionSiteIfcFile.IfcFileStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConstructionSiteIfcFileRepository extends JpaRepository<ConstructionSiteIfcFile, UUID> {

    List<ConstructionSiteIfcFile> findByConstructionSiteId(UUID constructionSiteId);

    List<ConstructionSiteIfcFile> findByConstructionSiteIdAndStatus(UUID constructionSiteId, IfcFileStatus status);

    Optional<ConstructionSiteIfcFile> findByConstructionSiteIdAndIfcFileId(UUID constructionSiteId, String ifcFileId);
}