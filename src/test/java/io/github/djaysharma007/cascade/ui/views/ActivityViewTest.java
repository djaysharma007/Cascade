package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.PluginSpec;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.RunRecord;
import io.github.djaysharma007.cascade.engine.WorkflowEngine;
import io.github.djaysharma007.cascade.plugins.BuiltinPlugins;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
import io.github.djaysharma007.cascade.ui.components.ToastOverlay;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityViewTest {

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
    void testActivityViewDisplaysRunsAndHandlesUndo(@TempDir Path tempDir) throws IOException {
        PluginRegistry registry = new PluginRegistry();
        BuiltinPlugins.registerAll(registry);
        WorkflowEngine engine = new WorkflowEngine(registry);
        EngineFacade facade = new EngineFacade(engine);

        Path sourceFile = tempDir.resolve("input.txt");
        Files.writeString(sourceFile, "Input file text");
        Path destDir = tempDir.resolve("target");

        Workflow wf = Workflow.builder()
                .name("Move Workflow")
                .enabled(true)
                .trigger(PluginSpec.of("manual", Map.of()))
                .addAction(PluginSpec.of("move-file", Map.of("destinationFolder", destDir.toString(), "onConflict", "rename")))
                .build();
        facade.registerWorkflow(wf);

        RunRecord run = facade.executeWorkflowManually(wf.id(), Event.of(Map.of("filePath", sourceFile.toAbsolutePath().toString())));
        assertThat(run).isNotNull();
        assertThat(facade.canUndo(run)).isTrue();
        assertThat(destDir.resolve("input.txt")).exists();
        assertThat(sourceFile).doesNotExist();

        AtomicReference<String> toastMessage = new AtomicReference<>();
        AtomicReference<ToastOverlay.Type> toastType = new AtomicReference<>();

        ActivityView view = new ActivityView(facade, (msg, type) -> {
            toastMessage.set(msg);
            toastType.set(type);
        });

        assertThat(view.getNode()).isNotNull();
        assertThat(view.getTableView().getItems()).isNotEmpty();

        // Perform undo
        view.performUndo(run);

        assertThat(toastMessage.get()).isEqualTo("Run undone successfully");
        assertThat(toastType.get()).isEqualTo(ToastOverlay.Type.SUCCESS);
        assertThat(facade.isRunUndone(run.id())).isTrue();
        assertThat(sourceFile).exists();
        assertThat(destDir.resolve("input.txt")).doesNotExist();
    }
}
