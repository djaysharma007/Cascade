package io.github.djaysharma007.cascade.engine;

import io.github.djaysharma007.cascade.core.Action;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.ExecutionContext;
import io.github.djaysharma007.cascade.core.ExecutionResult;
import io.github.djaysharma007.cascade.core.PluginSpec;
import io.github.djaysharma007.cascade.core.RunId;
import io.github.djaysharma007.cascade.core.ThreadSafety;
import io.github.djaysharma007.cascade.core.Trigger;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.core.WorkflowState;
import io.github.djaysharma007.cascade.registry.PluginDescriptor;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Execution engine managing workflow lifecycles and event dispatching.
 */
public final class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);
    private static final int MAX_RUN_HISTORY = 500;

    @ThreadSafety(value = ThreadSafety.Category.GUARDED, guard = "this")
    private final AtomicBoolean running = new AtomicBoolean(false);

    @ThreadSafety(value = ThreadSafety.Category.GUARDED, guard = "workflows")
    private final Map<WorkflowId, Workflow> workflows = new ConcurrentHashMap<>();

    @ThreadSafety(value = ThreadSafety.Category.GUARDED, guard = "activeTriggers")
    private final Map<WorkflowId, Trigger> activeTriggers = new ConcurrentHashMap<>();

    @ThreadSafety(value = ThreadSafety.Category.GUARDED, guard = "workflowStates")
    private final Map<WorkflowId, WorkflowState> workflowStates = new ConcurrentHashMap<>();

    @ThreadSafety(ThreadSafety.Category.CONFINED)
    private final List<RunRecord> runHistory = new CopyOnWriteArrayList<>();

    @ThreadSafety(ThreadSafety.Category.CONFINED)
    private final List<EngineListener> listeners = new CopyOnWriteArrayList<>();

    private final PluginRegistry registry;
    private final ConditionEvaluator conditionEvaluator;
    private final ExecutorService executor;

    public WorkflowEngine(PluginRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.conditionEvaluator = new ConditionEvaluator(registry);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public synchronized void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        log.info("WorkflowEngine starting");
        for (Workflow workflow : workflows.values()) {
            if (workflow.enabled()) {
                startWorkflowTrigger(workflow);
            } else {
                setWorkflowState(workflow.id(), WorkflowState.DISABLED);
            }
        }
        notifyEngineStateChanged(true);
        log.info("WorkflowEngine started");
    }

    public synchronized void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        log.info("WorkflowEngine stopping");
        for (WorkflowId wid : new ArrayList<>(activeTriggers.keySet())) {
            stopWorkflowTrigger(wid);
        }
        notifyEngineStateChanged(false);
        log.info("WorkflowEngine stopped");
    }

    public boolean isRunning() {
        return running.get();
    }

    public synchronized void registerWorkflow(Workflow workflow) {
        Objects.requireNonNull(workflow, "workflow must not be null");
        workflows.put(workflow.id(), workflow);
        if (running.get() && workflow.enabled()) {
            startWorkflowTrigger(workflow);
        } else {
            setWorkflowState(workflow.id(), workflow.enabled() ? WorkflowState.ENABLED : WorkflowState.DISABLED);
        }
    }

    public synchronized void unregisterWorkflow(WorkflowId id) {
        Objects.requireNonNull(id, "WorkflowId must not be null");
        stopWorkflowTrigger(id);
        workflows.remove(id);
        workflowStates.remove(id);
    }

    public synchronized void enableWorkflow(WorkflowId id) {
        Workflow wf = workflows.get(id);
        if (wf == null) {
            return;
        }
        Workflow updated = Workflow.builder()
                .id(wf.id())
                .name(wf.name())
                .enabled(true)
                .trigger(wf.trigger())
                .conditions(wf.conditionSpec().orElse(null))
                .actions(wf.actions())
                .build();
        workflows.put(id, updated);
        if (running.get()) {
            startWorkflowTrigger(updated);
        } else {
            setWorkflowState(id, WorkflowState.ENABLED);
        }
    }

    public synchronized void disableWorkflow(WorkflowId id) {
        Workflow wf = workflows.get(id);
        if (wf == null) {
            return;
        }
        stopWorkflowTrigger(id);
        Workflow updated = Workflow.builder()
                .id(wf.id())
                .name(wf.name())
                .enabled(false)
                .trigger(wf.trigger())
                .conditions(wf.conditionSpec().orElse(null))
                .actions(wf.actions())
                .build();
        workflows.put(id, updated);
        setWorkflowState(id, WorkflowState.DISABLED);
    }

    private void startWorkflowTrigger(Workflow wf) {
        stopWorkflowTrigger(wf.id());
        setWorkflowState(wf.id(), WorkflowState.STARTING);
        try {
            Trigger trigger = registry.createTrigger(wf.trigger().pluginId(), wf.trigger().params());
            activeTriggers.put(wf.id(), trigger);
            trigger.start(event -> onEventFired(wf.id(), event));
            setWorkflowState(wf.id(), WorkflowState.ENABLED);
        } catch (Exception e) {
            log.error("Failed to start trigger for workflow {}", wf.name(), e);
            setWorkflowState(wf.id(), WorkflowState.FAULTED);
        }
    }

    private void stopWorkflowTrigger(WorkflowId id) {
        Trigger trigger = activeTriggers.remove(id);
        if (trigger != null) {
            try {
                trigger.stop();
            } catch (Exception e) {
                log.warn("Error stopping trigger for workflow {}", id, e);
            }
        }
        setWorkflowState(id, WorkflowState.DISABLED);
    }

    private void setWorkflowState(WorkflowId id, WorkflowState state) {
        workflowStates.put(id, state);
        for (EngineListener l : listeners) {
            l.onWorkflowStateChanged(id, state);
        }
    }

    public WorkflowState getWorkflowState(WorkflowId id) {
        return workflowStates.getOrDefault(id, WorkflowState.DISABLED);
    }

    public void onEventFired(WorkflowId workflowId, Event event) {
        executor.submit(() -> executeWorkflow(workflowId, event));
    }

    public RunRecord executeWorkflow(WorkflowId workflowId, Event event) {
        Workflow workflow = workflows.get(workflowId);
        if (workflow == null) {
            throw new IllegalArgumentException("Workflow not found: " + workflowId);
        }
        RunId runId = RunId.random();
        Instant start = Instant.now();
        String triggerSummary = workflow.trigger().pluginId().value();

        RunRecord initial = new RunRecord(
                runId, workflowId, workflow.name(), start, null,
                RunOutcome.RUNNING, triggerSummary, List.of()
        );
        recordRun(initial);

        // Evaluate conditions
        boolean conditionsMet = workflow.conditionSpec()
                .map(spec -> conditionEvaluator.evaluate(spec, event))
                .orElse(true);

        if (!conditionsMet) {
            RunRecord skipped = new RunRecord(
                    runId, workflowId, workflow.name(), start, Instant.now(),
                    RunOutcome.SKIPPED, triggerSummary, List.of()
            );
            recordRun(skipped);
            return skipped;
        }

        // Run actions in order
        List<StepRecord> stepRecords = new ArrayList<>();
        RunOutcome outcome = RunOutcome.SUCCEEDED;

        List<PluginSpec> actionSpecs = workflow.actions();
        for (int i = 0; i < actionSpecs.size(); i++) {
            PluginSpec spec = actionSpecs.get(i);
            String actionName = registry.findDescriptor(spec.pluginId())
                    .map(PluginDescriptor::displayName)
                    .orElse(spec.pluginId().value());

            if (outcome == RunOutcome.FAILED) {
                stepRecords.add(StepRecord.notRun(i, spec.pluginId(), actionName));
                continue;
            }

            long stepStart = System.currentTimeMillis();
            try {
                Action action = registry.createAction(spec.pluginId(), spec.params());
                ExecutionContext context = new ExecutionContext(workflowId, runId, i);
                ExecutionResult res = action.execute(event, context);
                long duration = System.currentTimeMillis() - stepStart;
                stepRecords.add(new StepRecord(i, spec.pluginId(), actionName, res, duration));

                if (res instanceof ExecutionResult.Failed) {
                    outcome = RunOutcome.FAILED;
                }
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - stepStart;
                ExecutionResult failRes = new ExecutionResult.Failed(
                        new io.github.djaysharma007.cascade.core.Failure.Defect(e, e.getMessage() != null ? e.getMessage() : "Error")
                );
                stepRecords.add(new StepRecord(i, spec.pluginId(), actionName, failRes, duration));
                outcome = RunOutcome.FAILED;
            }
        }

        RunRecord completed = new RunRecord(
                runId, workflowId, workflow.name(), start, Instant.now(),
                outcome, triggerSummary, stepRecords
        );
        recordRun(completed);
        return completed;
    }

    private void recordRun(RunRecord record) {
        runHistory.add(0, record);
        if (runHistory.size() > MAX_RUN_HISTORY) {
            runHistory.remove(runHistory.size() - 1);
        }
        for (EngineListener l : listeners) {
            l.onRunUpdated(record);
        }
    }

    public List<RunRecord> getRuns() {
        return Collections.unmodifiableList(runHistory);
    }

    public Optional<Workflow> getWorkflow(WorkflowId id) {
        return Optional.ofNullable(workflows.get(id));
    }

    public List<Workflow> allWorkflows() {
        return List.copyOf(workflows.values());
    }

    public void addListener(EngineListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public void removeListener(EngineListener listener) {
        listeners.remove(listener);
    }

    private void notifyEngineStateChanged(boolean running) {
        for (EngineListener l : listeners) {
            l.onEngineStateChanged(running);
        }
    }
}
