package io.cerp.workflow.saga;

import java.util.Map;

/**
 * Exception thrown when a saga step fails.
 */
public class SagaException extends Exception {
    private final String errorCode;
    private final Map<String, Object> details;

    public SagaException(String errorCode, String message, Map<String, Object> details) {
        super(errorCode + ": " + message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public SagaException(String errorCode, String message, Throwable cause) {
        super(errorCode + ": " + message, cause);
        this.errorCode = errorCode;
        this.details = Map.of();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
