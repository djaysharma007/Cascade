package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.PluginSpec;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.WorkflowEngine;
import io.github.djaysharma007.cascade.plugins.BuiltinPlugins;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
import io.github.djaysharma007.cascade.storage.JsonWorkflowRepository;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardViewTest {

    private static boolean platformStarted = false;

    @BeforeAll
    static synchronized void initPlatform() {
        if (!platformStarted) {
            try {
                Platform.startup(() -> {});
                platformStarted = true;
            } catch (IllegalStateException ignored) {
                platformStarted = true;
            }
        }
    }

    @Test
    void testDashboardInitializationAndRefresh(@TempDir Path tempDir) {
        PluginRegistry registry = new PluginRegistry();
        BuiltinPlugins.registerAll(registry);
        WorkflowRepository repo = new JsonWorkflowRepository(tempDir);
        WorkflowEngine engine = new WorkflowEngine(registry);
        EngineFacade facade = new EngineFacade(engine);

        Workflow wf = Workflow.builder()
                .name("Auto Print")
                .enabled(true)
                .trigger(PluginSpec.of("manual", Map.of()))
                .addAction(PluginSpec.of("print-message", Map.of("text", "Hello Dashboard")))
                .build();
        repo.save(wf);
        facade.registerWorkflow(wf);

        AtomicBoolean actNavigated = new AtomicBoolean(false);
        AtomicBoolean wfNavigated = new AtomicBoolean(false);
        AtomicBoolean newWfNavigated = new AtomicBoolean(false);

        DashboardView dashboard = new DashboardView(
                facade,
                repo,
                () -> actNavigated.set(true),
                () -> wfNavigated.set(true),
                () -> newWfNavigated.set(true)
        );

        assertThat(dashboard.getNode()).isNotNull();

        // Execute a run
        facade.executeWorkflowManually(wf.id(), Event.empty());
        dashboard.refresh();

        assertThat(facade.getRuns()).hasSize(1);
    }
}
