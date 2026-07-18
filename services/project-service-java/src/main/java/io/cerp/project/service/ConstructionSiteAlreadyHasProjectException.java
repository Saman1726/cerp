package io.cerp.project.service;

/**
 * Exception thrown when attempting to assign a construction site to a project but another active project is already linked to that construction site.
 */
public class ConstructionSiteAlreadyHasProjectException extends RuntimeException {

    public ConstructionSiteAlreadyHasProjectException(String constructionSiteId, String existingProjectName, String existingProjectCode) {
        super(String.format(
            "Construction site '%s' is already linked to project '%s' (code: %s). Each construction site can only have one active project.",
            constructionSiteId, existingProjectName, existingProjectCode));
    }
}
