package io.github.djaysharma007.cascade.core;

/**
 * Workflow lifecycle states with defined transitions.
 */
public enum WorkflowState {
    DISABLED,
    STARTING,
    ENABLED,
    STOPPING,
    FAULTED
}
