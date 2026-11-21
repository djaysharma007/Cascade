package io.github.djaysharma007.cascade.engine;

/**
 * High-level outcome of a workflow execution run.
 */
public enum RunOutcome {
    RUNNING,
    SUCCEEDED,
    SKIPPED,
    FAILED,
    UNKNOWN
}
