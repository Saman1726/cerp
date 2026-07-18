package io.cerp.project.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import io.cerp.project.domain.Building;
import io.cerp.project.domain.Floor;
import io.cerp.project.domain.Unit;
import io.cerp.project.dto.BuildingCreateRequest;
import io.cerp.project.dto.BuildingResponse;
import io.cerp.project.dto.FloorResponse;
import io.cerp.project.dto.UnitResponse;
import io.cerp.project.repository.BuildingRepository;
import io.cerp.project.repository.FloorRepository;
import io.cerp.project.repository.UnitRepository;

/**
 * Service for building CRUD operations within a construction site.
 */
@Service
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final UnitRepository unitRepository;

    public BuildingService(BuildingRepository buildingRepository,
            FloorRepository floorRepository,
            UnitRepository unitRepository) {
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
        this.unitRepository = unitRepository;
    }

    public BuildingResponse toResponse(Building building) {
        List<Floor> floors = floorRepository.findByBuildingId(building.getId());
        List<FloorResponse> floorResponses = floors.stream()
            .map(floor -> {
                List<Unit> units = unitRepository.findByFloorId(floor.getId());
                List<UnitResponse> unitResponses = units.stream()
                    .map(unit -> new UnitResponse(
                        unit.getId(),
                        unit.getLabel(),
                        unit.getUsableArea(),
                        unit.getBedrooms(),
                        unit.getBathrooms(),
                        unit.getBalconies(),
                        unit.getToilets(),
                        unit.getCreatedAt(),
                        unit.getUpdatedAt()))
                    .toList();

                return new FloorResponse(
                    floor.getId(),
                    floor.getType(),
                    floor.getUsableArea(),
                    floor.getTotalArea(),
                    unitResponses,
                    floor.getCreatedAt(),
                    floor.getUpdatedAt());
            })
            .toList();

        return new BuildingResponse(
            building.getId(),
            building.getName(),
            building.getUsableArea(),
            building.getTotalArea(),
            floorResponses,
            building.getCreatedAt(),
            building.getUpdatedAt());
    }

    public List<BuildingResponse> findAllResponses(List<Building> buildings) {
        return buildings.stream()
            .map(this::toResponse)
            .toList();
    }
}
