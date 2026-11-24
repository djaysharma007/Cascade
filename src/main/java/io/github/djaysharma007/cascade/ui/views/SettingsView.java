package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.settings.Settings;
import io.github.djaysharma007.cascade.settings.SettingsService;
import io.github.djaysharma007.cascade.settings.StartupManager;
import io.github.djaysharma007.cascade.storage.WorkflowPortabilityService;
import io.github.djaysharma007.cascade.ui.components.ToastOverlay;
import io.github.djaysharma007.cascade.ui.components.ToggleSwitch;
import io.github.djaysharma007.cascade.ui.components.UiMotion;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Settings view for configuring tray, startup, motion, execution limits, and workflow portability.
 */
public final class SettingsView {

    private final SettingsService settingsService;
    private final WorkflowRepository repository;
    private final EngineFacade facade;
    private final BiConsumer<String, ToastOverlay.Type> toastConsumer;

    private final VBox root = new VBox(20);
    private final ToggleSwitch traySwitch = new ToggleSwitch(true);
    private final ToggleSwitch startupSwitch = new ToggleSwitch(false);
    private final ToggleSwitch motionSwitch = new ToggleSwitch(false);
    private final TextField timeoutField = new TextField();
    private final TextField retriesField = new TextField();

    public SettingsView(
            SettingsService settingsService,
            WorkflowRepository repository,
            EngineFacade facade,
            BiConsumer<String, ToastOverlay.Type> toastConsumer
    ) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.facade = Objects.requireNonNull(facade, "facade must not be null");
        this.toastConsumer = toastConsumer;

        buildUI();
        populateValues();
    }

    private void buildUI() {
        root.setPadding(new Insets(24));
        root.getStyleClass().add("page-view");

        // Header
        VBox headerBox = new VBox(6);
        Label title = new Label("Settings");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Configure app preferences, background execution, and workflow storage.");
        subtitle.getStyleClass().add("caption-text");
        headerBox.getChildren().addAll(title, subtitle);

        VBox contentBox = new VBox(20);

        // Section 1: Background & Startup
        VBox traySection = createCardSection("Background & Startup", "Configure system tray and automatic launch behavior.");
        HBox trayRow = createSettingRow(
                "Keep running in the tray when the window is closed",
                "Cascade stays active in the system tray when you close the main window.",
                traySwitch
        );
        traySwitch.selectedProperty().addListener((obs, oldV, newV) -> {
            Settings updated = settingsService.getSettings().withKeepRunningInTray(newV);
            settingsService.updateSettings(updated);
            showToast("Tray preference updated", ToastOverlay.Type.INFO);
        });

        HBox startupRow = createSettingRow(
                "Start Cascade when Windows starts",
                "Automatically launch Cascade in the background upon system login.",
                startupSwitch
        );
        boolean devMode = StartupManager.isDevMode();
        if (devMode) {
            startupSwitch.setDisable(true);
            Label devNote = new Label("Disabled: Requires packaged application (currently running in development mode)");
            devNote.getStyleClass().add("caption-text");
            devNote.setStyle("-fx-text-fill: -color-warning;");
            traySection.getChildren().addAll(trayRow, startupRow, devNote);
        } else {
            startupSwitch.selectedProperty().addListener((obs, oldV, newV) -> {
                Settings updated = settingsService.getSettings().withStartOnWindowsStartup(newV);
                settingsService.updateSettings(updated);
                showToast("Windows startup preference updated", ToastOverlay.Type.INFO);
            });
            traySection.getChildren().addAll(trayRow, startupRow);
        }

        // Section 2: Motion & Accessibility
        VBox motionSection = createCardSection("Motion & Accessibility", "Adjust UI animation effects for comfort.");
        HBox motionRow = createSettingRow(
                "Reduce motion",
                "Disable slide transitions and animated effects throughout the application.",
                motionSwitch
        );
        motionSwitch.selectedProperty().addListener((obs, oldV, newV) -> {
            Settings updated = settingsService.getSettings().withReduceMotion(newV);
            settingsService.updateSettings(updated);
            showToast(newV ? "Motion effects disabled" : "Motion effects enabled", ToastOverlay.Type.INFO);
        });
        motionSection.getChildren().add(motionRow);

        // Section 3: Engine Execution Limits
        VBox engineSection = createCardSection("Engine Execution", "Configure timeout limits and retry budgets for action steps.");
        timeoutField.setPrefWidth(120);
        HBox timeoutRow = createFieldRow("Action timeout (seconds)", "Maximum execution time per attempt before timing out.", timeoutField);

        retriesField.setPrefWidth(120);
        HBox retriesRow = createFieldRow("Max retry attempts", "Number of automatic retries for transient infrastructure failures.", retriesField);

        Button saveEngineBtn = new Button("Apply execution settings");
        saveEngineBtn.getStyleClass().add("button-primary");
        UiMotion.animateHover(saveEngineBtn);
        saveEngineBtn.setOnAction(e -> applyEngineSettings());

        engineSection.getChildren().addAll(timeoutRow, retriesRow, saveEngineBtn);

        // Section 4: Workflow Portability
        VBox portabilitySection = createCardSection("Workflow Portability", "Export workflows or import previously saved workflows as JSON.");
        HBox portBtnRow = new HBox(12);
        portBtnRow.setAlignment(Pos.CENTER_LEFT);

        Button importBtn = new Button("Import workflow...");
        importBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(importBtn);
        importBtn.setOnAction(e -> chooseAndImportWorkflow());

        Button exportAllBtn = new Button("Export all workflows...");
        exportAllBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(exportAllBtn);
        exportAllBtn.setOnAction(e -> chooseAndExportAllWorkflows());

        portBtnRow.getChildren().addAll(importBtn, exportAllBtn);
        portabilitySection.getChildren().add(portBtnRow);

        contentBox.getChildren().addAll(traySection, motionSection, engineSection, portabilitySection);

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.getChildren().addAll(headerBox, scrollPane);
    }

    private VBox createCardSection(String title, String subtitle) {
        VBox card = new VBox(12);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(16));

        Label t = new Label(title);
        t.getStyleClass().add("section-title");
        Label s = new Label(subtitle);
        s.getStyleClass().add("caption-text");

        card.getChildren().addAll(t, s);
        return card;
    }

    private HBox createSettingRow(String label, String description, ToggleSwitch control) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox textBox = new VBox(2);
        Label l = new Label(label);
        l.getStyleClass().add("body-text");
        l.setStyle("-fx-font-weight: bold;");
        Label d = new Label(description);
        d.getStyleClass().add("caption-text");
        textBox.getChildren().addAll(l, d);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        row.getChildren().addAll(textBox, control);
        return row;
    }

    private HBox createFieldRow(String label, String description, TextField field) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox textBox = new VBox(2);
        Label l = new Label(label);
        l.getStyleClass().add("body-text");
        Label d = new Label(description);
        d.getStyleClass().add("caption-text");
        textBox.getChildren().addAll(l, d);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        row.getChildren().addAll(textBox, field);
        return row;
    }

    private void populateValues() {
        Settings s = settingsService.getSettings();
        traySwitch.setSelected(s.keepRunningInTray());
        startupSwitch.setSelected(s.startOnWindowsStartup());
        motionSwitch.setSelected(s.reduceMotion());
        timeoutField.setText(String.valueOf(s.actionTimeoutSeconds()));
        retriesField.setText(String.valueOf(s.retryAttempts()));
    }

    public void applyEngineSettings() {
        try {
            int to = Integer.parseInt(timeoutField.getText().trim());
            int ret = Integer.parseInt(retriesField.getText().trim());
            if (to <= 0 || ret < 0) {
                showToast("Values must be positive numbers", ToastOverlay.Type.ERROR);
                return;
            }
            Settings updated = settingsService.getSettings()
                    .withActionTimeoutSeconds(to)
                    .withRetryAttempts(ret);
            settingsService.updateSettings(updated);
            showToast("Engine execution limits applied", ToastOverlay.Type.SUCCESS);
        } catch (NumberFormatException e) {
            showToast("Invalid number format entered", ToastOverlay.Type.ERROR);
        }
    }

    private void chooseAndImportWorkflow() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Workflow JSON");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Workflow JSON (*.json)", "*.json"));
        File file = chooser.showOpenDialog(root.getScene() != null ? root.getScene().getWindow() : null);
        if (file != null) {
            try {
                Workflow wf = WorkflowPortabilityService.importWorkflow(file.toPath(), repository, facade);
                showToast("Imported workflow: " + wf.name(), ToastOverlay.Type.SUCCESS);
            } catch (Exception e) {
                showToast("Failed to import workflow: " + e.getMessage(), ToastOverlay.Type.ERROR);
            }
        }
    }

    private void chooseAndExportAllWorkflows() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Export All Workflows");
        File dir = chooser.showDialog(root.getScene() != null ? root.getScene().getWindow() : null);
        if (dir != null) {
            try {
                List<Workflow> all = repository.findAll();
                int count = WorkflowPortabilityService.exportAllWorkflows(all, dir.toPath());
                showToast("Exported " + count + " workflow(s)", ToastOverlay.Type.SUCCESS);
            } catch (Exception e) {
                showToast("Failed to export workflows: " + e.getMessage(), ToastOverlay.Type.ERROR);
            }
        }
    }

    private void showToast(String message, ToastOverlay.Type type) {
        if (toastConsumer != null) {
            toastConsumer.accept(message, type);
        }
    }

    public Node getNode() {
        return root;
    }

    public ToggleSwitch getTraySwitch() {
        return traySwitch;
    }

    public ToggleSwitch getStartupSwitch() {
        return startupSwitch;
    }

    public ToggleSwitch getMotionSwitch() {
        return motionSwitch;
    }

    public TextField getTimeoutField() {
        return timeoutField;
    }

    public TextField getRetriesField() {
        return retriesField;
    }
}
