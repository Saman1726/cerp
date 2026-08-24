package io.cerp.project.service;

/**
 * Unchecked exception thrown when IFC file parsing fails during BIM ingestion.
 *
 * <p>Covers malformed IFC schemas, invalid entity references, and stream corruption.
 * Because it is unchecked, Spring's transactional infrastructure will automatically
 * rollback the surrounding {@code @Transactional} method when this is thrown.
 */
public final class BimIngestionException extends RuntimeException {

    private final String context;

    /**
     * Creates a new instance with a descriptive context and root cause.
     *
     * @param context a short description of where the failure occurred
     *                (e.g. "parsing IfcBuilding", "stream offset 4096")
     * @param cause   the underlying exception
     */
    public BimIngestionException(String context, Throwable cause) {
        super(String.format("BIM ingestion failed at %s", context), cause);
        this.context = context;
    }

    /**
     * Creates a new instance with a descriptive context and detail message.
     *
     * @param context a short description of where the failure occurred
     * @param message a human-readable detail message
     */
    public BimIngestionException(String context, String message) {
        super(String.format("BIM ingestion failed at %s: %s", context, message));
        this.context = context;
    }

    /**
     * Returns the contextual location string that describes where the failure occurred.
     */
    public String getContext() {
        return context;
    }
}