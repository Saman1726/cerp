package io.cerp.workflow.saga;

import java.time.Instant;

/**
 * Represents a single step in a saga execution.
 */
public class SagaStep {
    private final String stepCode;
    private final String stepName;
    private final String status;
    private final Instant startedAt;
    private final Instant completedAt;
    private final SagaError error;

    public SagaStep(String stepCode, String stepName, String status,
            Instant startedAt, Instant completedAt, SagaError error) {
        this.stepCode = stepCode;
        this.stepName = stepName;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.error = error;
    }

    public String getStepCode() {
        return stepCode;
    }

    public String getStepName() {
        return stepName;
    }

    public String getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public SagaError getError() {
        return error;
    }
}
