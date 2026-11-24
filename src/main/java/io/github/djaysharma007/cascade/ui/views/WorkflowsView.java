package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.RunRecord;
import io.github.djaysharma007.cascade.storage.WorkflowPortabilityService;
import io.github.djaysharma007.cascade.ui.components.PluginDisplayNameResolver;
import io.github.djaysharma007.cascade.ui.components.ToggleSwitch;
import io.github.djaysharma007.cascade.ui.components.UiMotion;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Calm, hand-designed Workflows view displaying workflows with one-line summaries,
 * state toggle switch, Edit button, and a More overflow menu.
 */
public final class WorkflowsView {

    private final EngineFacade facade;
    private final WorkflowRepository repository;
    private final Runnable onNewWorkflow;
    private final Consumer<Workflow> onEditWorkflow;

    private final VBox root = new VBox(20);
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

        VBox titleBox = new VBox(4);
        Label title = new Label("Workflows");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Manage your automated file operations and scheduled tasks.");
        subtitle.getStyleClass().add("secondary-text");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Text-only buttons (no decorative icons)
        Button importBtn = new Button("Import");
        importBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(importBtn);
        importBtn.setOnAction(e -> chooseAndImportWorkflow());

        Button newBtn = new Button("New workflow");
        newBtn.getStyleClass().add("button-primary");
        UiMotion.animateHover(newBtn);
        newBtn.setOnAction(e -> onNewWorkflow.run());

        header.getChildren().addAll(titleBox, spacer, importBtn, newBtn);

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
        VBox empty = new VBox(12);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(56, 24, 56, 24));
        empty.getStyleClass().add("card");

        Label header = new Label("No workflows configured");
        header.getStyleClass().add("section-title");

        Label sub = new Label("Workflows run automatically when triggered. Select New workflow to create one.");
        sub.getStyleClass().add("secondary-text");

        Button createBtn = new Button("New workflow");
        createBtn.getStyleClass().add("button-primary");
        UiMotion.animateHover(createBtn);
        createBtn.setOnAction(e -> onNewWorkflow.run());

        empty.getChildren().addAll(header, sub, createBtn);
        return empty;
    }

    private Node createWorkflowCard(Workflow wf) {
        HBox row = new HBox(16);
        row.getStyleClass().addAll("card", "list-row");
        row.setPadding(new Insets(14, 20, 14, 20));
        row.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(3);
        Label name = new Label(wf.name());
        name.getStyleClass().add("body-text");
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");

        Label summaryLabel = new Label(formatWorkflowSummary(wf));
        summaryLabel.getStyleClass().add("secondary-text");
        titleBox.getChildren().addAll(name, summaryLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // State Toggle switch
        ToggleSwitch toggle = new ToggleSwitch(wf.enabled());
        toggle.selectedProperty().addListener((obs, oldV, isSel) -> {
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

        // Exactly one text button "Edit"
        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(editBtn);
        editBtn.setOnAction(e -> onEditWorkflow.accept(wf));

        // "More" overflow menu (Run test, Duplicate, Export, Delete)
        MenuButton moreBtn = new MenuButton("More");
        moreBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(moreBtn);

        MenuItem runItem = new MenuItem("Run test");
        runItem.setOnAction(e -> {
            RunRecord record = facade.executeWorkflowManually(wf.id(), Event.empty());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Test run complete");
            alert.setHeaderText("Outcome: " + record.outcome());
            alert.setContentText("Executed " + record.steps().size() + " step(s) in " + record.totalDurationMs() + " ms");
            alert.showAndWait();
        });

        MenuItem duplicateItem = new MenuItem("Duplicate");
        duplicateItem.setOnAction(e -> {
            Workflow duplicate = Workflow.builder()
                    .name(wf.name() + " (Copy)")
                    .enabled(false)
                    .trigger(wf.trigger())
                    .conditions(wf.conditionSpec().orElse(null))
                    .actions(wf.actions())
                    .build();
            repository.save(duplicate);
            facade.registerWorkflow(duplicate);
            refresh();
        });

        MenuItem exportItem = new MenuItem("Export");
        exportItem.setOnAction(e -> exportWorkflowToFile(wf));

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> confirmAndDelete(wf));

        moreBtn.getItems().addAll(runItem, duplicateItem, exportItem, deleteItem);

        row.getChildren().addAll(titleBox, spacer, toggle, editBtn, moreBtn);
        return row;
    }

    private static String formatWorkflowSummary(Workflow wf) {
        String trigId = wf.trigger().pluginId().value();
        String trigName = PluginDisplayNameResolver.resolve(trigId);
        Map<String, Object> params = wf.trigger().params();

        StringBuilder sb = new StringBuilder();
        if ("file-created".equals(trigId)) {
            String dir = (String) params.getOrDefault("directory", params.get("folder"));
            sb.append("When a file is created");
            if (dir != null && !dir.isBlank()) {
                sb.append(" in ").append(dir);
            }
        } else if ("file-modified".equals(trigId)) {
            String dir = (String) params.getOrDefault("directory", params.get("folder"));
            sb.append("When a file is modified");
            if (dir != null && !dir.isBlank()) {
                sb.append(" in ").append(dir);
            }
        } else if ("schedule".equals(trigId)) {
            String time = (String) params.getOrDefault("time", params.get("timeOfDay"));
            sb.append("On schedule");
            if (time != null && !time.isBlank()) {
                sb.append(" at ").append(time);
            }
        } else if ("manual".equals(trigId)) {
            sb.append("When triggered manually");
        } else {
            sb.append("When ").append(trigName.toLowerCase());
        }

        if (!wf.actions().isEmpty()) {
            String firstAction = PluginDisplayNameResolver.resolve(wf.actions().get(0).pluginId().value());
            sb.append(" → ").append(firstAction);
            if (wf.actions().size() > 1) {
                sb.append(" (+").append(wf.actions().size() - 1).append(" more)");
            }
        }
        return sb.toString();
    }

    private void chooseAndImportWorkflow() {
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
    }

    private void exportWorkflowToFile(Workflow wf) {
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
    }

    private void confirmAndDelete(Workflow wf) {
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
    }

    public Node getNode() {
        return root;
    }
}
