package io.github.djaysharma007.cascade.engine;

import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.core.WorkflowState;

/**
 * Listener interface for engine state changes and run progress.
 */
public interface EngineListener {

    void onEngineStateChanged(boolean running);

    void onWorkflowStateChanged(WorkflowId workflowId, WorkflowState state);

    void onRunUpdated(RunRecord record);
}
