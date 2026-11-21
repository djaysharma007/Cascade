package io.github.djaysharma007.cascade.core;

import java.util.Objects;

/**
 * Execution context providing workflow and run metadata to actions.
 *
 * @param workflowId current workflow id, never null
 * @param runId current run id, never null
 * @param stepIndex 0-based index of current action step
 */
public record ExecutionContext(WorkflowId workflowId, RunId runId, int stepIndex) {

    public ExecutionContext {
        Objects.requireNonNull(workflowId, "workflowId must not be null");
        Objects.requireNonNull(runId, "runId must not be null");
        if (stepIndex < 0) {
            throw new IllegalArgumentException("stepIndex must not be negative");
        }
    }

    public String idempotencyKey() {
        return runId.value() + ":" + stepIndex;
    }
}
