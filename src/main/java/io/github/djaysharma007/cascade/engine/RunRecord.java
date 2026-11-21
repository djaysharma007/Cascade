package io.github.djaysharma007.cascade.engine;

import io.github.djaysharma007.cascade.core.RunId;
import io.github.djaysharma007.cascade.core.WorkflowId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable historical record of a workflow run.
 *
 * @param id unique run id, never null
 * @param workflowId workflow id, never null
 * @param workflowName workflow name, never null
 * @param startedAt start timestamp, never null
 * @param completedAt completion timestamp, or null if still running
 * @param outcome run outcome status, never null
 * @param triggerSummary summary of trigger, never null
 * @param steps immutable list of executed and skipped step records, never null
 */
public record RunRecord(
        RunId id,
        WorkflowId workflowId,
        String workflowName,
        Instant startedAt,
        Instant completedAt,
        RunOutcome outcome,
        String triggerSummary,
        List<StepRecord> steps
) {
    public RunRecord {
        Objects.requireNonNull(id, "RunId must not be null");
        Objects.requireNonNull(workflowId, "WorkflowId must not be null");
        Objects.requireNonNull(workflowName, "workflowName must not be null");
        Objects.requireNonNull(startedAt, "startedAt must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        triggerSummary = triggerSummary == null ? "Manual" : triggerSummary;
        steps = steps == null ? List.of() : List.copyOf(steps);
    }

    public Optional<Instant> completedTimestamp() {
        return Optional.ofNullable(completedAt);
    }

    public long totalDurationMs() {
        if (completedAt == null) {
            return 0;
        }
        return Math.max(0, completedAt.toEpochMilli() - startedAt.toEpochMilli());
    }
}
