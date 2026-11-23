package io.github.djaysharma007.cascade.engine;

import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.RunId;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.core.WorkflowState;
import io.github.djaysharma007.cascade.engine.undo.UndoService;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Public facade exposing engine operations to UI view-models and controllers.
 */
public final class EngineFacade {

    private final WorkflowEngine engine;
    private final UndoService undoService = new UndoService();

    public EngineFacade(WorkflowEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    public void start() {
        engine.start();
    }

    public void stop() {
        engine.stop();
    }

    public boolean isRunning() {
        return engine.isRunning();
    }

    public void registerWorkflow(Workflow workflow) {
        engine.registerWorkflow(workflow);
    }

    public void unregisterWorkflow(WorkflowId id) {
        engine.unregisterWorkflow(id);
    }

    public void enableWorkflow(WorkflowId id) {
        engine.enableWorkflow(id);
    }

    public void disableWorkflow(WorkflowId id) {
        engine.disableWorkflow(id);
    }

    public WorkflowState getWorkflowState(WorkflowId id) {
        return engine.getWorkflowState(id);
    }

    public List<Workflow> allWorkflows() {
        return engine.allWorkflows();
    }

    public Optional<Workflow> getWorkflow(WorkflowId id) {
        return engine.getWorkflow(id);
    }

    public RunRecord executeWorkflowManually(WorkflowId id, Event event) {
        return engine.executeWorkflow(id, event);
    }

    public List<RunRecord> getRuns() {
        return engine.getRuns();
    }

    public void addListener(EngineListener listener) {
        engine.addListener(listener);
    }

    public void removeListener(EngineListener listener) {
        engine.removeListener(listener);
    }

    public boolean canUndo(RunRecord run) {
        return undoService.canUndo(run);
    }

    public boolean isRunUndone(RunId runId) {
        return undoService.isUndone(runId);
    }

    public void undoRun(RunRecord run) throws IOException {
        undoService.undoRun(run);
    }

    public UndoService getUndoService() {
        return undoService;
    }
}
