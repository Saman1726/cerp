package io.cerp.project.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.cerp.project.domain.Unit;

@Repository
public interface UnitRepository extends JpaRepository<Unit, UUID> {

    List<Unit> findByFloorId(UUID floorId);
}
