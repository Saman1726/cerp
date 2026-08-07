package io.cerp.project.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to link a SubProject to a Building that already has one. Mirrors ConstructionSiteAlreadyHasProjectException for the analogous
 * Building<->SubProject 1:1 relationship.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class BuildingAlreadyHasSubProjectException extends RuntimeException {

    public BuildingAlreadyHasSubProjectException(UUID buildingId) {
        super(String.format(
            "Building '%s' is already linked to a SubProject. Each building can only have one SubProject.", buildingId));
    }
}
