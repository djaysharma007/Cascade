package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.RunRecord;
import io.github.djaysharma007.cascade.ui.components.CascadeIcons;
import io.github.djaysharma007.cascade.ui.components.PluginDisplayNameResolver;
import io.github.djaysharma007.cascade.ui.components.ToggleSwitch;
import io.github.djaysharma007.cascade.ui.components.UiMotion;
import io.github.djaysharma007.cascade.storage.WorkflowPortabilityService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Workflows listing view showing all configured automations with smooth interaction.
 */
public final class WorkflowsView {

    private final EngineFacade facade;
    private final WorkflowRepository repository;
    private final Runnable onNewWorkflow;
    private final Consumer<Workflow> onEditWorkflow;

    private final VBox root = new VBox(16);
    private final VBox listContainer = new VBox(12);
    private final VBox alertContainer = new VBox(8);

    public WorkflowsView(
            EngineFacade facade,
            WorkflowRepository repository,
            Runnable onNewWorkflow,
            Consumer<Workflow> onEditWorkflow
    ) {
        this.facade = facade;
        this.repository = repository;
        this.onNewWorkflow = onNewWorkflow;
        this.onEditWorkflow = onEditWorkflow;

        buildView();
        refresh();
    }

    private void buildView() {
        root.setPadding(new Insets(24));
        root.getStyleClass().add("surface-panel");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Workflows");
        title.getStyleClass().add("page-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button importBtn = new Button("Import");
        importBtn.setGraphic(CascadeIcons.icon("folder", 0.85, "icon-secondary"));
        importBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(importBtn);
        importBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Import Workflow JSON");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Workflow JSON (*.json)", "*.json"));
            File file = chooser.showOpenDialog(root.getScene() != null ? root.getScene().getWindow() : null);
            if (file != null) {
                try {
                    WorkflowPortabilityService.importWorkflow(file.toPath(), repository, facade);
                    refresh();
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Import Failed");
                    alert.setHeaderText("Failed to import workflow");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                }
            }
        });

        Button newBtn = new Button("New workflow");
        newBtn.setGraphic(CascadeIcons.icon("plus", 0.9, "icon-primary"));
        newBtn.getStyleClass().add("button-primary");
        UiMotion.animateHover(newBtn);
        newBtn.setOnAction(e -> onNewWorkflow.run());

        header.getChildren().addAll(title, spacer, importBtn, newBtn);

        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.getChildren().addAll(header, alertContainer, scrollPane);
    }

    public void refresh() {
        alertContainer.getChildren().clear();
        listContainer.getChildren().clear();

        // Check for corrupt files (FR-20)
        List<WorkflowRepository.CorruptWorkflowFile> corrupt = repository.getCorruptFiles();
        if (!corrupt.isEmpty()) {
            VBox box = new VBox(4);
            box.getStyleClass().add("needs-attention-box");
            Label alertTitle = new Label("Needs attention");
            alertTitle.getStyleClass().add("section-title");
            box.getChildren().add(alertTitle);
            for (var cf : corrupt) {
                Label reasonLabel = new Label("File " + cf.fileName() + ": " + cf.reason());
                reasonLabel.getStyleClass().add("body-text");
                box.getChildren().add(reasonLabel);
            }
            alertContainer.getChildren().add(box);
        }

        List<Workflow> workflows = repository.findAll();
        if (workflows.isEmpty()) {
            listContainer.getChildren().add(createEmptyState());
        } else {
            for (Workflow wf : workflows) {
                listContainer.getChildren().add(createWorkflowCard(wf));
            }
            UiMotion.staggerFadeIn(listContainer.getChildren());
        }
    }

    private Node createEmptyState() {
        VBox empty = new VBox(16);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(64, 24, 64, 24));
        empty.getStyleClass().add("card");

        var emptyIcon = CascadeIcons.icon("workflows", 2.0, "icon-muted");

        Label header = new Label("Workflows run automatically when triggered");
        header.getStyleClass().add("section-title");

        Label sub = new Label("Select New workflow to create your first automation.");
        sub.getStyleClass().add("secondary-text");

        Button createBtn = new Button("New workflow");
        createBtn.setGraphic(CascadeIcons.icon("plus", 0.9, "icon-primary"));
        createBtn.getStyleClass().add("button-primary");
        UiMotion.animateHover(createBtn);
        createBtn.setOnAction(e -> onNewWorkflow.run());

        empty.getChildren().addAll(emptyIcon, header, sub, createBtn);
        return empty;
    }

    private Node createWorkflowCard(Workflow wf) {
        HBox row = new HBox(16);
        row.getStyleClass().addAll("card", "list-row");
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        var wfIcon = CascadeIcons.icon("workflows", 1.0, "icon-teal");

        VBox titleBox = new VBox(2);
        Label name = new Label(wf.name());
        name.getStyleClass().add("body-text");
        name.setStyle("-fx-font-weight: bold;");

        String friendlyTrigger = PluginDisplayNameResolver.resolve(wf.trigger().pluginId().value());
        Label triggerDesc = new Label("Trigger: " + friendlyTrigger);
        triggerDesc.getStyleClass().add("caption-text");
        titleBox.getChildren().addAll(name, triggerDesc);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // State chip
        Label chip = new Label(wf.enabled() ? "Enabled" : "Disabled");
        chip.getStyleClass().addAll("chip", wf.enabled() ? "chip-enabled" : "chip-disabled");

        // Toggle switch
        ToggleSwitch toggle = new ToggleSwitch(wf.enabled());
        toggle.selectedProperty().addListener((obs, oldV, isSel) -> {
            chip.setText(isSel ? "Enabled" : "Disabled");
            chip.getStyleClass().setAll("chip", isSel ? "chip-enabled" : "chip-disabled");
            if (isSel) {
                facade.enableWorkflow(wf.id());
                repository.save(Workflow.builder()
                        .id(wf.id())
                        .name(wf.name())
                        .enabled(true)
                        .trigger(wf.trigger())
                        .conditions(wf.conditionSpec().orElse(null))
                        .actions(wf.actions())
                        .build());
            } else {
                facade.disableWorkflow(wf.id());
                repository.save(Workflow.builder()
                        .id(wf.id())
                        .name(wf.name())
                        .enabled(false)
                        .trigger(wf.trigger())
                        .conditions(wf.conditionSpec().orElse(null))
                        .actions(wf.actions())
                        .build());
            }
        });

        // Run test button
        Button runBtn = new Button("Run");
        runBtn.setGraphic(CascadeIcons.icon("play", 0.8, "icon-secondary"));
        runBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(runBtn);
        runBtn.setOnAction(e -> {
            RunRecord record = facade.executeWorkflowManually(wf.id(), Event.empty());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Test run complete");
            alert.setHeaderText("Outcome: " + record.outcome());
            alert.setContentText("Executed " + record.steps().size() + " step(s) in " + record.totalDurationMs() + " ms");
            alert.showAndWait();
        });

        // Edit button
        Button editBtn = new Button("Edit");
        editBtn.setGraphic(CascadeIcons.icon("editor", 0.8, "icon-secondary"));
        editBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(editBtn);
        editBtn.setOnAction(e -> onEditWorkflow.accept(wf));

        // Export button
        Button exportBtn = new Button("Export");
        exportBtn.setGraphic(CascadeIcons.icon("file", 0.8, "icon-secondary"));
        exportBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(exportBtn);
        exportBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export Workflow JSON");
            chooser.setInitialFileName(wf.name().replaceAll("[^a-zA-Z0-9-_\\.]", "_") + ".json");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Workflow JSON (*.json)", "*.json"));
            File target = chooser.showSaveDialog(root.getScene() != null ? root.getScene().getWindow() : null);
            if (target != null) {
                try {
                    WorkflowPortabilityService.exportWorkflow(wf, target.toPath());
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Export Successful");
                    alert.setHeaderText("Workflow exported successfully");
                    alert.setContentText("Saved to " + target.getAbsolutePath());
                    alert.showAndWait();
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Export Failed");
                    alert.setHeaderText("Failed to export workflow");
                    alert.setContentText(ex.getMessage());
                    alert.showAndWait();
                }
            }
        });

        // Delete button
        Button delBtn = new Button("Delete");
        delBtn.setGraphic(CascadeIcons.icon("trash", 0.8, "icon-error"));
        delBtn.getStyleClass().add("button-danger");
        UiMotion.animateHover(delBtn);
        delBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm delete");
            confirm.setHeaderText("Delete workflow: " + wf.name() + "?");
            confirm.setContentText("This action cannot be undone.");
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                facade.unregisterWorkflow(wf.id());
                repository.delete(wf.id());
                refresh();
            }
        });

        row.getChildren().addAll(wfIcon, titleBox, spacer, chip, toggle, runBtn, editBtn, exportBtn, delBtn);
        return row;
    }

    public Node getNode() {
        return root;
    }
}
