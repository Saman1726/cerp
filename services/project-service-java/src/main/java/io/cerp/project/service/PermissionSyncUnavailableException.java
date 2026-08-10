package io.cerp.project.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a project-level assignment's matching identity-service resource membership (used by workflow-service's write-permission checks) could not be created,
 * updated, or revoked. Fail-closed by design, mirroring QuotaCheckUnavailableException: an assignment that "succeeds" without its permission counterpart would leave
 * the assignee unable to act on the project (or, on removal, leave a stale grant behind) until someone notices and re-triggers the assignment - silently allowing
 * that drift is not an acceptable failure mode.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class PermissionSyncUnavailableException extends RuntimeException {

    public PermissionSyncUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
