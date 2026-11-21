package io.github.djaysharma007.cascade.engine;

import io.github.djaysharma007.cascade.core.ConditionSpec;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.ExecutionResult;
import io.github.djaysharma007.cascade.core.PluginSpec;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.core.WorkflowState;
import io.github.djaysharma007.cascade.plugins.BuiltinPlugins;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowEngineTest {

    private PluginRegistry registry;
    private WorkflowEngine engine;
    private EngineFacade facade;

    @BeforeEach
    void setUp() {
        registry = new PluginRegistry();
        BuiltinPlugins.registerAll(registry);
        engine = new WorkflowEngine(registry);
        facade = new EngineFacade(engine);
    }

    @Test
    void startAndStopEngineTogglesRunningState() {
        AtomicBoolean engineRunning = new AtomicBoolean(false);
        facade.addListener(new EngineListener() {
            @Override
            public void onEngineStateChanged(boolean running) {
                engineRunning.set(running);
            }

            @Override
            public void onWorkflowStateChanged(WorkflowId workflowId, WorkflowState state) {}

            @Override
            public void onRunUpdated(RunRecord record) {}
        });

        assertThat(facade.isRunning()).isFalse();
        facade.start();
        assertThat(facade.isRunning()).isTrue();
        assertThat(engineRunning.get()).isTrue();

        facade.stop();
        assertThat(facade.isRunning()).isFalse();
        assertThat(engineRunning.get()).isFalse();
    }

    @Test
    void enableAndDisableWorkflowsUpdatesState() {
        Workflow wf = Workflow.builder()
                .name("Sample")
                .enabled(false)
                .trigger(PluginSpec.of("manual", Map.of()))
                .addAction(PluginSpec.of("print-message", Map.of("text", "Hello")))
                .build();

        facade.registerWorkflow(wf);
        assertThat(facade.getWorkflowState(wf.id())).isEqualTo(WorkflowState.DISABLED);

        facade.enableWorkflow(wf.id());
        assertThat(facade.getWorkflowState(wf.id())).isEqualTo(WorkflowState.ENABLED);

        facade.disableWorkflow(wf.id());
        assertThat(facade.getWorkflowState(wf.id())).isEqualTo(WorkflowState.DISABLED);
    }

    @Test
    void executeWorkflowSuccessRunsActionsInOrder() {
        Workflow wf = Workflow.builder()
                .name("Order Execution")
                .enabled(true)
                .trigger(PluginSpec.of("manual", Map.of()))
                .conditions(ConditionSpec.leaf("extension-is", Map.of("value", ".pdf")))
                .addAction(PluginSpec.of("print-message", Map.of("text", "Step 1: {fileName}")))
                .addAction(PluginSpec.of("print-message", Map.of("text", "Step 2: Done")))
                .build();

        facade.registerWorkflow(wf);
        Event event = Event.of(Map.of("fileName", "doc.pdf"));

        RunRecord record = facade.executeWorkflowManually(wf.id(), event);
        assertThat(record.outcome()).isEqualTo(RunOutcome.SUCCEEDED);
        assertThat(record.steps()).hasSize(2);
        assertThat(record.steps().get(0).result()).isInstanceOf(ExecutionResult.Success.class);
        assertThat(record.steps().get(1).result()).isInstanceOf(ExecutionResult.Success.class);
    }

    @Test
    void executeWorkflowConditionMismatchSkipsRun() {
        Workflow wf = Workflow.builder()
                .name("Condition Skip")
                .enabled(true)
                .trigger(PluginSpec.of("manual", Map.of()))
                .conditions(ConditionSpec.leaf("extension-is", Map.of("value", ".pdf")))
                .addAction(PluginSpec.of("print-message", Map.of("text", "Should not run")))
                .build();

        facade.registerWorkflow(wf);
        Event event = Event.of(Map.of("fileName", "image.png"));

        RunRecord record = facade.executeWorkflowManually(wf.id(), event);
        assertThat(record.outcome()).isEqualTo(RunOutcome.SKIPPED);
        assertThat(record.steps()).isEmpty();
    }

    @Test
    void failedStepStopsRunAndRemainingStepsAreMarkedNotRun() {
        Workflow wf = Workflow.builder()
                .name("Failure Halt")
                .enabled(true)
                .trigger(PluginSpec.of("manual", Map.of()))
                .addAction(PluginSpec.of("move-file", Map.of("destinationFolder", "C:/non_existent_path", "onConflict", "skip")))
                .addAction(PluginSpec.of("print-message", Map.of("text", "Step 2")))
                .build();

        facade.registerWorkflow(wf);
        // Provide non-existent file
        Event event = Event.of(Map.of("filePath", "C:/phantom_file.pdf"));

        RunRecord record = facade.executeWorkflowManually(wf.id(), event);
        assertThat(record.outcome()).isEqualTo(RunOutcome.FAILED);
        assertThat(record.steps()).hasSize(2);
        assertThat(record.steps().get(0).result()).isInstanceOf(ExecutionResult.Failed.class);
        // Step 2 must be marked as not run
        assertThat(record.steps().get(1).result()).isInstanceOf(ExecutionResult.Skipped.class);
        ExecutionResult.Skipped skipped = (ExecutionResult.Skipped) record.steps().get(1).result();
        assertThat(skipped.reason()).contains("Not run");
    }
}
