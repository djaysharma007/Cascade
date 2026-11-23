package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.ExecutionResult;
import io.github.djaysharma007.cascade.core.RunId;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.RunOutcome;
import io.github.djaysharma007.cascade.engine.RunRecord;
import io.github.djaysharma007.cascade.engine.StepRecord;
import io.github.djaysharma007.cascade.ui.components.CascadeIcons;
import io.github.djaysharma007.cascade.ui.components.PluginDisplayNameResolver;
import io.github.djaysharma007.cascade.ui.components.ToastOverlay;
import io.github.djaysharma007.cascade.ui.components.UiMotion;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Activity log view with run table and step timeline detail panel.
 */
public final class ActivityView {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final EngineFacade facade;
    private final BiConsumer<String, ToastOverlay.Type> toastConsumer;
    private final BorderPane root = new BorderPane();
    private final TableView<RunRecord> tableView = new TableView<>();
    private final VBox emptyState = new VBox(16);
    private final VBox detailPanel = new VBox(16);

    public ActivityView(EngineFacade facade) {
        this(facade, null);
    }

    public ActivityView(EngineFacade facade, BiConsumer<String, ToastOverlay.Type> toastConsumer) {
        this.facade = facade;
        this.toastConsumer = toastConsumer;
        buildView();
        refresh();
    }

    private void buildView() {
        root.setPadding(new Insets(24));
        root.getStyleClass().add("surface-panel");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Activity");
        title.getStyleClass().add("page-title");
        header.getChildren().add(title);
        header.setPadding(new Insets(0, 0, 16, 0));
        root.setTop(header);

        // Table setup
        tableView.getStyleClass().add("table-view");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<RunRecord, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(TIME_FORMAT.format(data.getValue().startedAt())));
        timeCol.setPrefWidth(160);

        TableColumn<RunRecord, String> wfCol = new TableColumn<>("Workflow");
        wfCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().workflowName()));
        wfCol.setPrefWidth(200);

        TableColumn<RunRecord, String> triggerCol = new TableColumn<>("Trigger");
        triggerCol.setCellValueFactory(data -> new SimpleStringProperty(
                PluginDisplayNameResolver.resolve(data.getValue().triggerSummary())
        ));
        triggerCol.setPrefWidth(140);

        TableColumn<RunRecord, String> outcomeCol = new TableColumn<>("Outcome");
        outcomeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().outcome().name()));
        outcomeCol.setPrefWidth(120);

        TableColumn<RunRecord, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().totalDurationMs() + " ms"));
        durationCol.setPrefWidth(100);

        TableColumn<RunRecord, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setPrefWidth(100);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button undoBtn = new Button("Undo");
            {
                undoBtn.getStyleClass().add("button-secondary");
                undoBtn.setStyle("-fx-padding: 2px 8px; -fx-font-size: 11px;");
                undoBtn.setGraphic(CascadeIcons.icon("undo", 0.7, "icon-secondary"));
                UiMotion.animateHover(undoBtn);
                undoBtn.setOnAction(e -> {
                    RunRecord run = getTableRow() != null ? getTableRow().getItem() : null;
                    if (run != null) {
                        performUndo(run);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    RunRecord run = getTableRow().getItem();
                    if (facade.isRunUndone(run.id())) {
                        undoBtn.setText("Undone");
                        undoBtn.setDisable(true);
                        setGraphic(undoBtn);
                    } else if (facade.canUndo(run)) {
                        undoBtn.setText("Undo");
                        undoBtn.setDisable(false);
                        setGraphic(undoBtn);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        tableView.getColumns().addAll(timeCol, wfCol, triggerCol, outcomeCol, durationCol, actionCol);

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected != null) {
                showDetailPanel(selected);
            } else {
                root.setRight(null);
            }
        });

        // Empty state
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(64, 24, 64, 24));
        emptyState.getStyleClass().add("card");
        var actIcon = CascadeIcons.icon("activity", 2.0, "icon-muted");
        Label emptyTitle = new Label("No workflow runs recorded yet");
        emptyTitle.getStyleClass().add("section-title");
        Label emptySub = new Label("Runs appear here automatically when triggers fire.");
        emptySub.getStyleClass().add("secondary-text");
        emptyState.getChildren().addAll(actIcon, emptyTitle, emptySub);

        // Detail panel setup
        detailPanel.getStyleClass().add("detail-panel");

        root.setCenter(tableView);
    }

    public void refresh() {
        RunRecord selected = tableView.getSelectionModel().getSelectedItem();
        RunId selectedId = selected != null ? selected.id() : null;

        List<RunRecord> runs = facade.getRuns();
        tableView.getItems().setAll(runs);

        if (runs.isEmpty()) {
            root.setCenter(emptyState);
            root.setRight(null);
        } else {
            root.setCenter(tableView);
            if (selectedId != null) {
                for (RunRecord r : runs) {
                    if (r.id().equals(selectedId)) {
                        tableView.getSelectionModel().select(r);
                        showDetailPanel(r);
                        break;
                    }
                }
            }
        }
    }

    private void showDetailPanel(RunRecord run) {
        detailPanel.getChildren().clear();

        HBox detailHeader = new HBox(8);
        detailHeader.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Run details");
        title.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button undoBtn = new Button("Undo run");
        undoBtn.setGraphic(CascadeIcons.icon("undo", 0.75, "icon-secondary"));
        undoBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(undoBtn);

        if (facade.isRunUndone(run.id())) {
            undoBtn.setText("Undone");
            undoBtn.setDisable(true);
        } else if (facade.canUndo(run)) {
            undoBtn.setOnAction(e -> performUndo(run));
        } else {
            undoBtn.setVisible(false);
            undoBtn.setManaged(false);
        }

        Button closeBtn = new Button("Close");
        closeBtn.setGraphic(CascadeIcons.icon("close", 0.75, "icon-secondary"));
        closeBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(closeBtn);
        closeBtn.setOnAction(e -> {
            tableView.getSelectionModel().clearSelection();
            root.setRight(null);
        });

        detailHeader.getChildren().addAll(title, spacer, undoBtn, closeBtn);

        // Metadata box
        VBox metaBox = new VBox(6);
        metaBox.getStyleClass().add("card");
        metaBox.setPadding(new Insets(12));

        Label wfName = new Label(run.workflowName());
        wfName.getStyleClass().add("body-text");
        wfName.setStyle("-fx-font-weight: bold;");

        Label runId = new Label("ID: " + run.id().value());
        runId.getStyleClass().add("code-text");

        String friendlyTrigger = PluginDisplayNameResolver.resolve(run.triggerSummary());
        Label triggerLabel = new Label("Trigger: " + friendlyTrigger);
        triggerLabel.getStyleClass().add("secondary-text");

        Label outcomeLabel = new Label("Outcome: " + run.outcome());
        outcomeLabel.getStyleClass().addAll("chip", getChipStyle(run.outcome().name()));

        Label durLabel = new Label("Total duration: " + run.totalDurationMs() + " ms");
        durLabel.getStyleClass().add("secondary-text");

        metaBox.getChildren().addAll(wfName, runId, triggerLabel, outcomeLabel, durLabel);

        // If run failed, display a prominent failure cause banner
        if (run.outcome() == RunOutcome.FAILED) {
            VBox failureCard = createFailureBanner(run);
            metaBox.getChildren().add(failureCard);
        }

        // Timeline of steps
        Label stepsTitle = new Label("Step execution timeline");
        stepsTitle.getStyleClass().add("section-title");

        VBox stepsBox = new VBox(8);
        for (StepRecord step : run.steps()) {
            VBox stepCard = new VBox(6);
            stepCard.getStyleClass().add("step-timeline-item");

            boolean notRun = step.result() instanceof ExecutionResult.Skipped sk
                    && sk.reason().contains("Not run");
            if (notRun) {
                stepCard.getStyleClass().add("step-timeline-notrun");
            }

            String friendlyAction = PluginDisplayNameResolver.resolve(step.actionName());
            Label stepHeader = new Label((step.stepIndex() + 1) + ". " + friendlyAction);
            stepHeader.getStyleClass().add("body-text");
            stepHeader.setStyle("-fx-font-weight: bold;");

            HBox badgeRow = new HBox(8);
            badgeRow.setAlignment(Pos.CENTER_LEFT);

            String resText = step.result() instanceof ExecutionResult.Success ? "Succeeded"
                    : (step.result() instanceof ExecutionResult.Failed ? "Failed" : "Not run");
            Label stepRes = new Label(resText);
            stepRes.getStyleClass().addAll("chip", getChipStyle(resText));

            Label stepDur = new Label(step.durationMs() + " ms");
            stepDur.getStyleClass().add("caption-text");

            if (step.attempts() > 0) {
                String attemptStr = step.attempts() == 1 ? "1 attempt" : step.attempts() + " attempts";
                Label attemptLabel = new Label(attemptStr);
                attemptLabel.getStyleClass().add("caption-text");
                badgeRow.getChildren().addAll(stepRes, stepDur, attemptLabel);
            } else {
                badgeRow.getChildren().addAll(stepRes, stepDur);
            }

            stepCard.getChildren().addAll(stepHeader, badgeRow);

            if (step.result() instanceof ExecutionResult.Failed f) {
                Label failReason = new Label(f.failure().message());
                failReason.getStyleClass().add("field-error-label");
                failReason.setWrapText(true);
                stepCard.getChildren().add(failReason);

                f.failure().findCause().ifPresent(cause -> {
                    Label causeLabel = new Label("Cause: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
                    causeLabel.getStyleClass().add("caption-text");
                    causeLabel.setStyle("-fx-text-fill: -color-error;");
                    causeLabel.setWrapText(true);
                    stepCard.getChildren().add(causeLabel);
                });
            } else if (step.result() instanceof ExecutionResult.Skipped sk) {
                Label skipReason = new Label(sk.reason());
                skipReason.getStyleClass().add("secondary-text");
                skipReason.setWrapText(true);
                stepCard.getChildren().add(skipReason);
            } else if (step.result() instanceof ExecutionResult.Success succ && !succ.output().isEmpty()) {
                Label outputLabel = new Label("Output: " + succ.output());
                outputLabel.getStyleClass().add("caption-text");
                outputLabel.setWrapText(true);
                stepCard.getChildren().add(outputLabel);
            }

            stepsBox.getChildren().add(stepCard);
        }

        ScrollPane stepsScroll = new ScrollPane(stepsBox);
        stepsScroll.setFitToWidth(true);
        stepsScroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(stepsScroll, Priority.ALWAYS);

        detailPanel.getChildren().addAll(detailHeader, metaBox, stepsTitle, stepsScroll);
        root.setRight(detailPanel);
    }

    private VBox createFailureBanner(RunRecord run) {
        VBox failureCard = new VBox(4);
        failureCard.getStyleClass().addAll("card", "error-card");
        failureCard.setPadding(new Insets(8));

        Label failHeader = new Label("Failure reason");
        failHeader.getStyleClass().add("body-text");
        failHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-error;");

        StepRecord failedStep = run.steps().stream()
                .filter(s -> s.result() instanceof ExecutionResult.Failed)
                .findFirst()
                .orElse(null);

        if (failedStep != null && failedStep.result() instanceof ExecutionResult.Failed f) {
            String friendlyAction = PluginDisplayNameResolver.resolve(failedStep.actionName());
            Label msgLabel = new Label(friendlyAction + ": " + f.failure().message());
            msgLabel.getStyleClass().add("body-text");
            msgLabel.setStyle("-fx-text-fill: -color-error;");
            msgLabel.setWrapText(true);
            failureCard.getChildren().addAll(failHeader, msgLabel);

            f.failure().findCause().ifPresent(cause -> {
                Label causeLabel = new Label("Cause: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
                causeLabel.getStyleClass().add("caption-text");
                causeLabel.setStyle("-fx-text-fill: -color-error;");
                causeLabel.setWrapText(true);
                failureCard.getChildren().add(causeLabel);
            });
        } else {
            Label fallback = new Label("Workflow run failed without recorded step failure.");
            fallback.getStyleClass().add("body-text");
            fallback.setStyle("-fx-text-fill: -color-error;");
            failureCard.getChildren().addAll(failHeader, fallback);
        }

        return failureCard;
    }

    public void performUndo(RunRecord run) {
        try {
            facade.undoRun(run);
            if (toastConsumer != null) {
                toastConsumer.accept("Run undone successfully", ToastOverlay.Type.SUCCESS);
            }
            refresh();
            showDetailPanel(run);
        } catch (Exception e) {
            if (toastConsumer != null) {
                toastConsumer.accept("Undo failed: " + e.getMessage(), ToastOverlay.Type.ERROR);
            }
        }
    }

    private String getChipStyle(String status) {
        return switch (status.toUpperCase()) {
            case "SUCCEEDED" -> "chip-succeeded";
            case "FAILED" -> "chip-failed";
            case "RUNNING" -> "chip-running";
            case "SKIPPED" -> "chip-warning";
            default -> "chip-disabled";
        };
    }

    public Node getNode() {
        return root;
    }

    public TableView<RunRecord> getTableView() {
        return tableView;
    }
}
