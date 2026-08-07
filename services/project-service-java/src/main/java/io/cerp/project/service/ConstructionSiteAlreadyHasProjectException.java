package io.cerp.project.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when attempting to assign a construction site to a project but another active project is already linked to that construction site.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConstructionSiteAlreadyHasProjectException extends RuntimeException {

    public ConstructionSiteAlreadyHasProjectException(String constructionSiteId, String existingProjectName, String existingProjectCode) {
        super(String.format(
            "Construction site '%s' is already linked to project '%s' (code: %s). Each construction site can only have one active project.",
            constructionSiteId, existingProjectName, existingProjectCode));
    }

    public ConstructionSiteAlreadyHasProjectException(String constructionSiteId) {
        super(String.format(
            "Construction site '%s' is already linked to another active project. Each construction site can only have one active project.",
            constructionSiteId));
    }
}
