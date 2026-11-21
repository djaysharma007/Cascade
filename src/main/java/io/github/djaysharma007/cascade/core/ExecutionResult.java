package io.github.djaysharma007.cascade.core;

import java.util.Map;
import java.util.Objects;

/**
 * Sealed result of an action execution: Success, Skipped, or Failed.
 */
public sealed interface ExecutionResult {

    /**
     * Execution completed successfully with optional output payload.
     */
    record Success(Map<String, Object> output) implements ExecutionResult {
        public Success {
            output = output == null ? Map.of() : Map.copyOf(output);
        }

        public static Success empty() {
            return new Success(Map.of());
        }
    }

    /**
     * Execution was skipped deliberately.
     */
    record Skipped(String reason) implements ExecutionResult {
        public Skipped {
            Objects.requireNonNull(reason, "reason must not be null");
        }
    }

    /**
     * Execution failed with a classified failure species.
     */
    record Failed(Failure failure) implements ExecutionResult {
        public Failed {
            Objects.requireNonNull(failure, "failure must not be null");
        }
    }
}
