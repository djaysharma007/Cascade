package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.RunRecord;
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

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Workflows listing view showing all configured automations and status.
 */
public final class WorkflowsView {

    private final EngineFacade facade;
    private final WorkflowRepository repository;
    private final Runnable onNewWorkflow;
    private final Consumer<Workflow> onEditWorkflow;

    private final VBox root = new VBox(16);
    private final VBox listContainer = new VBox(8);
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

        Button newBtn = new Button("New workflow");
        newBtn.getStyleClass().add("button-primary");
        newBtn.setOnAction(e -> onNewWorkflow.run());

        header.getChildren().addAll(title, spacer, newBtn);

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
                listContainer.getChildren().add(createWorkflowRow(wf));
            }
        }
    }

    private Node createEmptyState() {
        VBox empty = new VBox(16);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(64, 24, 64, 24));

        Label header = new Label("Workflows run automatically when triggered");
        header.getStyleClass().add("section-title");

        Label sub = new Label("Select New workflow to create your first automation.");
        sub.getStyleClass().add("secondary-text");

        Button createBtn = new Button("New workflow");
        createBtn.getStyleClass().add("button-primary");
        createBtn.setOnAction(e -> onNewWorkflow.run());

        empty.getChildren().addAll(header, sub, createBtn);
        return empty;
    }

    private Node createWorkflowRow(Workflow wf) {
        HBox row = new HBox(16);
        row.getStyleClass().add("list-row");
        row.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label name = new Label(wf.name());
        name.getStyleClass().add("body-text");
        name.setStyle("-fx-font-weight: bold;");

        Label triggerDesc = new Label("Trigger: " + wf.trigger().pluginId().value());
        triggerDesc.getStyleClass().add("caption-text");
        titleBox.getChildren().addAll(name, triggerDesc);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // State chip
        Label chip = new Label(wf.enabled() ? "Enabled" : "Disabled");
        chip.getStyleClass().addAll("chip", wf.enabled() ? "chip-enabled" : "chip-disabled");

        // Toggle button
        Button toggleBtn = new Button(wf.enabled() ? "Disable" : "Enable");
        toggleBtn.getStyleClass().add("button-secondary");
        toggleBtn.setOnAction(e -> {
            if (wf.enabled()) {
                facade.disableWorkflow(wf.id());
                repository.save(Workflow.builder()
                        .id(wf.id())
                        .name(wf.name())
                        .enabled(false)
                        .trigger(wf.trigger())
                        .conditions(wf.conditionSpec().orElse(null))
                        .actions(wf.actions())
                        .build());
            } else {
                facade.enableWorkflow(wf.id());
                repository.save(Workflow.builder()
                        .id(wf.id())
                        .name(wf.name())
                        .enabled(true)
                        .trigger(wf.trigger())
                        .conditions(wf.conditionSpec().orElse(null))
                        .actions(wf.actions())
                        .build());
            }
            refresh();
        });

        // Run test button
        Button runBtn = new Button("Run test");
        runBtn.getStyleClass().add("button-secondary");
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
        editBtn.getStyleClass().add("button-secondary");
        editBtn.setOnAction(e -> onEditWorkflow.accept(wf));

        // Delete button
        Button delBtn = new Button("Delete");
        delBtn.getStyleClass().add("button-danger");
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

        row.getChildren().addAll(titleBox, spacer, chip, toggleBtn, runBtn, editBtn, delBtn);
        return row;
    }

    public Node getNode() {
        return root;
    }
}
