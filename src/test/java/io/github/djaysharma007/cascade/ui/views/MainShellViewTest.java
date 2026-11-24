package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.WorkflowEngine;
import io.github.djaysharma007.cascade.plugins.BuiltinPlugins;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
import io.github.djaysharma007.cascade.storage.JsonWorkflowRepository;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MainShellViewTest {

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

    private MainShellView createShell(Path tempDir) {
        PluginRegistry registry = new PluginRegistry();
        BuiltinPlugins.registerAll(registry);
        WorkflowRepository repo = new JsonWorkflowRepository(tempDir);
        WorkflowEngine engine = new WorkflowEngine(registry);
        EngineFacade facade = new EngineFacade(engine);

        return new MainShellView(facade, repo, registry);
    }

    private long countActive(HBox navItem) {
        return navItem.getStyleClass().stream().filter("nav-item-active"::equals).count();
    }

    @Test
    void testInitialNavigationState(@TempDir Path tempDir) {
        MainShellView shell = createShell(tempDir);

        assertThat(countActive(shell.getNavDashboard())).isEqualTo(1);
        assertThat(countActive(shell.getNavWorkflows())).isEqualTo(0);
        assertThat(countActive(shell.getNavTemplates())).isEqualTo(0);
        assertThat(countActive(shell.getNavActivity())).isEqualTo(0);
        assertThat(countActive(shell.getNavSettings())).isEqualTo(0);
    }

    @Test
    void testNavigationTransitionsAndHelper(@TempDir Path tempDir) {
        MainShellView shell = createShell(tempDir);

        // Workflows
        shell.showWorkflowsView();
        assertThat(countActive(shell.getNavWorkflows())).isEqualTo(1);
        assertThat(countActive(shell.getNavDashboard())).isEqualTo(0);

        // Templates
        shell.showTemplateGalleryView();
        assertThat(countActive(shell.getNavTemplates())).isEqualTo(1);
        assertThat(countActive(shell.getNavWorkflows())).isEqualTo(0);

        // Activity
        shell.showActivityView();
        assertThat(countActive(shell.getNavActivity())).isEqualTo(1);
        assertThat(countActive(shell.getNavTemplates())).isEqualTo(0);

        // Settings
        shell.showSettingsView();
        assertThat(countActive(shell.getNavSettings())).isEqualTo(1);
        assertThat(countActive(shell.getNavActivity())).isEqualTo(0);

        // Open new workflow editor: no nav items active
        shell.openNewWorkflowEditor();
        assertThat(countActive(shell.getNavDashboard())).isEqualTo(0);
        assertThat(countActive(shell.getNavWorkflows())).isEqualTo(0);
        assertThat(countActive(shell.getNavTemplates())).isEqualTo(0);
        assertThat(countActive(shell.getNavActivity())).isEqualTo(0);
        assertThat(countActive(shell.getNavSettings())).isEqualTo(0);

        // Back to Overview
        shell.showDashboardView();
        assertThat(countActive(shell.getNavDashboard())).isEqualTo(1);

        // Open editor from template: no nav items active
        shell.openWorkflowEditorFromTemplate(null);
        assertThat(countActive(shell.getNavDashboard())).isEqualTo(0);
        assertThat(countActive(shell.getNavWorkflows())).isEqualTo(0);

        // Open edit editor: no nav items active
        shell.openEditWorkflowEditor(null);
        assertThat(countActive(shell.getNavDashboard())).isEqualTo(0);
        assertThat(countActive(shell.getNavWorkflows())).isEqualTo(0);
    }

    @Test
    void testNavItemActiveNeverAddedTwice(@TempDir Path tempDir) {
        MainShellView shell = createShell(tempDir);

        // Call showDashboardView multiple times
        shell.showDashboardView();
        shell.showDashboardView();
        shell.showDashboardView();
        assertThat(countActive(shell.getNavDashboard())).isEqualTo(1);

        // Call showWorkflowsView multiple times
        shell.showWorkflowsView();
        shell.showWorkflowsView();
        shell.showWorkflowsView();
        assertThat(countActive(shell.getNavWorkflows())).isEqualTo(1);
        assertThat(countActive(shell.getNavDashboard())).isEqualTo(0);
    }

    @Test
    void testEngineStateChangeHandledSafely(@TempDir Path tempDir) {
        MainShellView shell = createShell(tempDir);

        // Fire engine state listener callbacks directly
        shell.onEngineStateChanged(true);
        shell.onEngineStateChanged(false);
        shell.onEngineStateChanged(true);
        shell.onEngineStateChanged(false);
    }
}
