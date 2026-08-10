package io.cerp.project.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a Project deletion's cascade to archive its Workflows or Bidding/Procurement sagas
 * in workflow-service could not complete. Fail-closed by design, mirroring
 * PermissionSyncUnavailableException: a Project that "deletes" successfully while its Workflows
 * and sagas stay live and visible elsewhere would be a worse outcome than failing the delete and
 * letting the caller retry.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class CascadeArchivalException extends RuntimeException {

    public CascadeArchivalException(String message, Throwable cause) {
        super(message, cause);
    }
}
