package io.cerp.project.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a plan quota could not be verified (identity-service unreachable/erroring). Fail-closed by design: quota checks gate paid-tier limits, and rows created
 * during an identity-service outage would not un-create themselves once it recovers - unlike a permission check, "allow by default" here is not an acceptable failure
 * mode.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class QuotaCheckUnavailableException extends RuntimeException {

    public QuotaCheckUnavailableException(String message) {
        super(message);
    }
}
