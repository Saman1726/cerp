package io.cerp.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.cerp.project.domain.Floor;

@Repository
public interface FloorRepository extends JpaRepository<Floor, UUID> {

    List<Floor> findByBuildingId(UUID buildingId);
}
