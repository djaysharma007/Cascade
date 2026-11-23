package io.github.djaysharma007.cascade.ui.components;

import io.github.djaysharma007.cascade.core.PluginSpec;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.WorkflowEngine;
import io.github.djaysharma007.cascade.plugins.BuiltinPlugins;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
import io.github.djaysharma007.cascade.ui.views.WorkflowTemplate;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class CommandPaletteDialogTest {

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
    void testCommandPaletteLifecycleAndSearch() {
        AtomicBoolean closed = new AtomicBoolean(false);
        CommandPaletteDialog dialog = new CommandPaletteDialog(() -> closed.set(true));

        assertThat(dialog.isOpen()).isFalse();

        PluginRegistry registry = new PluginRegistry();
        BuiltinPlugins.registerAll(registry);
        WorkflowEngine engine = new WorkflowEngine(registry);
        EngineFacade facade = new EngineFacade(engine);

        Workflow wf = Workflow.builder()
                .name("Daily Backup")
                .trigger(PluginSpec.of("manual", Map.of()))
                .addAction(PluginSpec.of("print-message", Map.of("text", "backing up")))
                .build();
        facade.registerWorkflow(wf);

        AtomicBoolean navDashboardCalled = new AtomicBoolean(false);
        AtomicBoolean navWorkflowsCalled = new AtomicBoolean(false);
        AtomicBoolean navSettingsCalled = new AtomicBoolean(false);
        AtomicBoolean newWfCalled = new AtomicBoolean(false);

        WorkflowTemplate template = new WorkflowTemplate(
                "clean-desktop",
                "Clean Desktop",
                "Archive desktop files",
                "Organization",
                "folder",
                wf
        );

        dialog.open(
                facade,
                () -> navDashboardCalled.set(true),
                () -> navWorkflowsCalled.set(true),
                () -> {},
                () -> {},
                () -> navSettingsCalled.set(true),
                () -> newWfCalled.set(true),
                w -> {},
                w -> {},
                () -> List.of(template)
        );

        assertThat(dialog.isOpen()).isTrue();
        assertThat(dialog.getItemCount()).isGreaterThan(5);

        // Test search filtering for settings
        dialog.setSearchText("settings");
        assertThat(dialog.getItemCount()).isGreaterThan(0);
        assertThat(dialog.getSelectedItem().title()).containsIgnoringCase("settings");

        // Execute selected item
        dialog.executeSelected();
        assertThat(dialog.isOpen()).isFalse();
        assertThat(navSettingsCalled.get()).isTrue();
        assertThat(closed.get()).isTrue();

        // Reopen and search for workflow
        closed.set(false);
        dialog.open(
                facade,
                () -> navDashboardCalled.set(true),
                () -> navWorkflowsCalled.set(true),
                () -> {},
                () -> {},
                () -> navSettingsCalled.set(true),
                () -> newWfCalled.set(true),
                w -> {},
                w -> {},
                () -> List.of(template)
        );

        dialog.setSearchText("daily backup");
        assertThat(dialog.getItemCount()).isGreaterThan(0);
        assertThat(dialog.getSelectedItem().title()).contains("Daily Backup");

        // Close directly
        dialog.close();
        assertThat(dialog.isOpen()).isFalse();
    }
}
