package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.WorkflowEngine;
import io.github.djaysharma007.cascade.plugins.BuiltinPlugins;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
import io.github.djaysharma007.cascade.settings.Settings;
import io.github.djaysharma007.cascade.settings.SettingsService;
import io.github.djaysharma007.cascade.storage.JsonWorkflowRepository;
import io.github.djaysharma007.cascade.ui.components.ToastOverlay;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SettingsViewTest {

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
    void testSettingsViewTogglesAndLimits(@TempDir Path tempDir) {
        Path settingsFile = tempDir.resolve("settings.json");
        SettingsService settingsService = new SettingsService(settingsFile);

        PluginRegistry registry = new PluginRegistry();
        BuiltinPlugins.registerAll(registry);
        WorkflowRepository repo = new JsonWorkflowRepository(tempDir.resolve("workflows"));
        WorkflowEngine engine = new WorkflowEngine(registry);
        EngineFacade facade = new EngineFacade(engine);

        AtomicReference<String> lastToast = new AtomicReference<>();
        SettingsView view = new SettingsView(
                settingsService,
                repo,
                facade,
                (msg, type) -> lastToast.set(msg)
        );

        assertThat(view.getNode()).isNotNull();

        // Check initial values
        Settings initial = settingsService.getSettings();
        assertThat(view.getTraySwitch().isSelected()).isEqualTo(initial.keepRunningInTray());
        assertThat(view.getMotionSwitch().isSelected()).isEqualTo(initial.reduceMotion());
        assertThat(view.getTimeoutField().getText()).isEqualTo(String.valueOf(initial.actionTimeoutSeconds()));
        assertThat(view.getRetriesField().getText()).isEqualTo(String.valueOf(initial.retryAttempts()));

        // Toggle tray
        view.getTraySwitch().toggle();
        assertThat(settingsService.getSettings().keepRunningInTray()).isNotEqualTo(initial.keepRunningInTray());
        assertThat(lastToast.get()).contains("Tray preference updated");

        // Toggle motion
        view.getMotionSwitch().toggle();
        assertThat(settingsService.getSettings().reduceMotion()).isNotEqualTo(initial.reduceMotion());
        assertThat(lastToast.get()).contains("Motion effects");

        // Update execution limits
        view.getTimeoutField().setText("45");
        view.getRetriesField().setText("5");
        view.applyEngineSettings();

        assertThat(settingsService.getSettings().actionTimeoutSeconds()).isEqualTo(45);
        assertThat(settingsService.getSettings().retryAttempts()).isEqualTo(5);
        assertThat(lastToast.get()).contains("applied");

        // Test invalid execution limits
        view.getTimeoutField().setText("-10");
        view.applyEngineSettings();
        assertThat(lastToast.get()).contains("must be positive");
    }
}
