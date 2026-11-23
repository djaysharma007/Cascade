package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.ConditionSpec;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowEditorViewTest {

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
    void testNewWorkflowDefaultsAndReordering(@TempDir Path tempDir) {
        PluginRegistry registry = new PluginRegistry();
        BuiltinPlugins.registerAll(registry);
        WorkflowRepository repo = new JsonWorkflowRepository(tempDir);
        WorkflowEngine engine = new WorkflowEngine(registry);
        EngineFacade facade = new EngineFacade(engine);

        AtomicBoolean completed = new AtomicBoolean(false);

        WorkflowEditorView editor = new WorkflowEditorView(
                facade,
                repo,
                registry,
                null,
                () -> completed.set(true)
        );

        assertThat(editor.getNode()).isNotNull();
        assertThat(editor.getActionCount()).isEqualTo(1);
        assertThat(editor.getActionPluginIds()).containsExactly("print-message");

        // Add a second action
        editor.addAction("move-file", Map.of("destination", tempDir.toString()));
        assertThat(editor.getActionCount()).isEqualTo(2);
        assertThat(editor.getActionPluginIds()).containsExactly("print-message", "move-file");

        // Reorder: move 2nd action up
        editor.moveActionUp(1);
        assertThat(editor.getActionPluginIds()).containsExactly("move-file", "print-message");

        // Move 1st action down
        editor.moveActionDown(0);
        assertThat(editor.getActionPluginIds()).containsExactly("print-message", "move-file");

        // Remove 1st action
        editor.removeAction(0);
        assertThat(editor.getActionCount()).isEqualTo(1);
        assertThat(editor.getActionPluginIds()).containsExactly("move-file");
    }

    @Test
    void testExistingWorkflowPopulationAndSave(@TempDir Path tempDir) {
        PluginRegistry registry = new PluginRegistry();
        BuiltinPlugins.registerAll(registry);
        WorkflowRepository repo = new JsonWorkflowRepository(tempDir);
        WorkflowEngine engine = new WorkflowEngine(registry);
        EngineFacade facade = new EngineFacade(engine);

        Workflow existing = Workflow.builder()
                .name("Test Workflow")
                .enabled(true)
                .trigger(PluginSpec.of("manual", Map.of()))
                .conditions(ConditionSpec.leaf("extension-is", Map.of("extension", "pdf")))
                .addAction(PluginSpec.of("print-message", Map.of("text", "First")))
                .addAction(PluginSpec.of("print-message", Map.of("text", "Second")))
                .build();

        AtomicBoolean completed = new AtomicBoolean(false);

        WorkflowEditorView editor = new WorkflowEditorView(
                facade,
                repo,
                registry,
                existing,
                () -> completed.set(true)
        );

        assertThat(editor.getName()).isEqualTo("Test Workflow");
        assertThat(editor.getActionCount()).isEqualTo(2);
        assertThat(editor.getActionPluginIds()).containsExactly("print-message", "print-message");

        // Validation test: empty name fails
        editor.setName("");
        assertThat(editor.validateEditor()).isFalse();

        // Valid name succeeds and saves
        editor.setName("Updated Workflow");
        assertThat(editor.validateEditor()).isTrue();

        editor.triggerSave();
        assertThat(completed.get()).isTrue();
        assertThat(repo.findAll()).hasSize(1);
        assertThat(repo.findById(existing.id())).isPresent();
        assertThat(repo.findById(existing.id()).get().name()).isEqualTo("Updated Workflow");
    }
}
