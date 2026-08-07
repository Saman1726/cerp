package io.cerp.project.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a creation/assignment would exceed the creator's plan quota - a definite, known rejection.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(String message) {
        super(message);
    }
}
