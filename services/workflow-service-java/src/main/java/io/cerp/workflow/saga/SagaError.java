package io.cerp.workflow.saga;

import java.time.Instant;
import java.util.Map;

/**
 * Error representation for saga failures.
 */
public class SagaError {
    private final String errorCode;
    private final String message;
    private final Map<String, Object> details;
    private final Instant occurredAt;

    public SagaError(String errorCode, String message, Map<String, Object> details) {
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
        this.occurredAt = Instant.now();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
