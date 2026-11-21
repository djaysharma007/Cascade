package io.github.djaysharma007.cascade.engine;

import io.github.djaysharma007.cascade.core.ExecutionResult;
import io.github.djaysharma007.cascade.core.PluginId;

import java.util.Objects;

/**
 * Record of a single action step execution within a run.
 *
 * @param stepIndex 0-based index of the action step
 * @param actionId id of the action plugin
 * @param actionName display name of the action
 * @param result outcome result of execution
 * @param durationMs duration in milliseconds
 */
public record StepRecord(
        int stepIndex,
        PluginId actionId,
        String actionName,
        ExecutionResult result,
        long durationMs
) {
    public StepRecord {
        Objects.requireNonNull(actionId, "actionId must not be null");
        Objects.requireNonNull(actionName, "actionName must not be null");
        Objects.requireNonNull(result, "result must not be null");
    }

    public static StepRecord notRun(int stepIndex, PluginId actionId, String actionName) {
        return new StepRecord(
                stepIndex,
                actionId,
                actionName,
                new ExecutionResult.Skipped("Not run due to preceding failure"),
                0
        );
    }
}
