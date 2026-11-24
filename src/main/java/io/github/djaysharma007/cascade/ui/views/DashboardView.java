package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.RunOutcome;
import io.github.djaysharma007.cascade.engine.RunRecord;
import io.github.djaysharma007.cascade.ui.components.PluginDisplayNameResolver;
import io.github.djaysharma007.cascade.ui.components.UiMotion;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Overview screen displaying calm KPI cards, a 7-day activity chart (2/3 width),
 * and live recent activity feed (1/3 width).
 */
public final class DashboardView {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("EEE dd");

    private final EngineFacade facade;
    private final WorkflowRepository repository;
    private final Runnable onNavigateActivity;
    private final Runnable onNavigateWorkflows;
    private final Runnable onNewWorkflow;

    private final ScrollPane scrollPane = new ScrollPane();
    private final VBox root = new VBox(24);

    private final Label runsTodayVal = new Label("0");
    private final Label runsTodayContext = new Label("Today's runs");

    private final Label successRateVal = new Label("—");
    private final Label successRateContext = new Label("No runs yet");

    private final Label activeWorkflowsVal = new Label("0");
    private final Label activeWorkflowsContext = new Label("Active and monitoring");

    private final Label failuresVal = new Label("0");
    private final Label failuresContext = new Label("No failures");
    private VBox failuresCard;

    private final CategoryAxis xAxis = new CategoryAxis();
    private final NumberAxis yAxis = new NumberAxis();
    private final BarChart<String, Number> activityChart = new BarChart<>(xAxis, yAxis);

    private final VBox recentFeedContainer = new VBox(8);

    public DashboardView(
            EngineFacade facade,
            WorkflowRepository repository,
            Runnable onNavigateActivity,
            Runnable onNavigateWorkflows,
            Runnable onNewWorkflow
    ) {
        this.facade = Objects.requireNonNull(facade);
        this.repository = Objects.requireNonNull(repository);
        this.onNavigateActivity = onNavigateActivity;
        this.onNavigateWorkflows = onNavigateWorkflows;
        this.onNewWorkflow = onNewWorkflow;

        buildView();
        refresh();
    }

    private void buildView() {
        root.setPadding(new Insets(24));
        root.getStyleClass().add("surface-panel");

        // Header: Plex Serif page title "Overview" and concise subtitle
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Overview");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Execution summary and live workflow activity.");
        subtitle.getStyleClass().add("secondary-text");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button viewActBtn = new Button("View activity");
        viewActBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(viewActBtn);
        if (onNavigateActivity != null) {
            viewActBtn.setOnAction(e -> onNavigateActivity.run());
        }

        Button newWfBtn = new Button("New workflow");
        newWfBtn.getStyleClass().add("button-primary");
        UiMotion.animateHover(newWfBtn);
        if (onNewWorkflow != null) {
            newWfBtn.setOnAction(e -> onNewWorkflow.run());
        }

        header.getChildren().addAll(titleBox, spacer, viewActBtn, newWfBtn);

        // 4 Stat Cards Row (no icons, clean typographic hierarchy)
        HBox statCardsRow = createStatCardsRow();

        // Two-column section: Chart card (~2/3 width) and Recent activity card (~1/3 width)
        HBox twoColRow = new HBox(16);
        VBox chartCard = createChartCard();
        VBox recentCard = createRecentActivityCard();

        HBox.setHgrow(chartCard, Priority.ALWAYS);
        HBox.setHgrow(recentCard, Priority.ALWAYS);
        chartCard.prefWidthProperty().bind(twoColRow.widthProperty().multiply(0.64));
        recentCard.prefWidthProperty().bind(twoColRow.widthProperty().multiply(0.36));

        twoColRow.getChildren().addAll(chartCard, recentCard);

        root.getChildren().addAll(header, statCardsRow, twoColRow);

        scrollPane.setContent(root);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
    }

    private HBox createStatCardsRow() {
        HBox row = new HBox(16);

        VBox card1 = createStatCard("Runs today", runsTodayVal, runsTodayContext);
        VBox card2 = createStatCard("Success rate", successRateVal, successRateContext);
        VBox card3 = createStatCard("Active workflows", activeWorkflowsVal, activeWorkflowsContext);
        this.failuresCard = createStatCard("Failures", failuresVal, failuresContext);

        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(failuresCard, Priority.ALWAYS);

        row.getChildren().addAll(card1, card2, card3, failuresCard);
        return row;
    }

    private VBox createStatCard(String labelText, Label valLabel, Label contextLabel) {
        VBox card = new VBox(6);
        card.getStyleClass().addAll("card", "stat-card");

        Label label = new Label(labelText);
        label.getStyleClass().add("stat-label");

        valLabel.getStyleClass().add("stat-value");
        contextLabel.getStyleClass().add("stat-context");

        card.getChildren().addAll(label, valLabel, contextLabel);
        return card;
    }

    private VBox createChartCard() {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("card");
        card.setPadding(new Insets(20));

        Label chartTitle = new Label("Runs over last 7 days");
        chartTitle.getStyleClass().add("section-title");

        xAxis.setAnimated(false);
        yAxis.setAnimated(false);
        yAxis.setMinorTickVisible(false);
        yAxis.setTickUnit(1);

        activityChart.setLegendVisible(false);
        activityChart.setAnimated(!UiMotion.isReduceMotion());
        activityChart.setPrefHeight(240);
        activityChart.setMinHeight(220);

        card.getChildren().addAll(chartTitle, activityChart);
        return card;
    }

    private VBox createRecentActivityCard() {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("card");
        card.setPadding(new Insets(20));

        HBox head = new HBox(12);
        head.setAlignment(Pos.CENTER_LEFT);
        Label headTitle = new Label("Live recent activity");
        headTitle.getStyleClass().add("section-title");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button allBtn = new Button("View all");
        allBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(allBtn);
        if (onNavigateActivity != null) {
            allBtn.setOnAction(e -> onNavigateActivity.run());
        }

        head.getChildren().addAll(headTitle, sp, allBtn);
        card.getChildren().addAll(head, recentFeedContainer);
        return card;
    }

    public void refresh() {
        List<RunRecord> allRuns = facade.getRuns();
        List<Workflow> allWfs = facade.allWorkflows();

        LocalDate today = LocalDate.now();
        long runsToday = allRuns.stream()
                .filter(r -> r.startedAt().atZone(ZoneId.systemDefault()).toLocalDate().equals(today))
                .count();

        long successfulToday = allRuns.stream()
                .filter(r -> r.startedAt().atZone(ZoneId.systemDefault()).toLocalDate().equals(today)
                        && r.outcome() == RunOutcome.SUCCEEDED)
                .count();

        long failuresToday = allRuns.stream()
                .filter(r -> r.startedAt().atZone(ZoneId.systemDefault()).toLocalDate().equals(today)
                        && r.outcome() == RunOutcome.FAILED)
                .count();

        long enabledWorkflows = allWfs.stream().filter(Workflow::enabled).count();

        // Update Runs today
        UiMotion.animateNumber(runsTodayVal, 0, (int) runsToday, "");
        runsTodayContext.setText("Today's runs");

        // Update Success rate: with 0 runs must show dash and "No runs yet", never 100%
        if (runsToday == 0) {
            successRateVal.setText("—");
            successRateContext.setText("No runs yet");
        } else {
            int successRate = (int) Math.round(((double) successfulToday / runsToday) * 100.0);
            successRateVal.setText(successRate + "%");
            successRateContext.setText(successfulToday + " of " + runsToday + " succeeded");
        }

        // Update Active workflows
        UiMotion.animateNumber(activeWorkflowsVal, 0, (int) enabledWorkflows, "");
        activeWorkflowsContext.setText(enabledWorkflows == 1 ? "1 enabled" : enabledWorkflows + " enabled");

        // Update Failures: neutral style at 0, only error style when > 0
        UiMotion.animateNumber(failuresVal, 0, (int) failuresToday, "");
        if (failuresToday == 0) {
            failuresContext.setText("No failures");
            failuresContext.setStyle("-fx-text-fill: -color-text-muted;");
            if (failuresCard != null) {
                failuresCard.getStyleClass().remove("stat-card-error");
            }
        } else {
            failuresContext.setText(failuresToday == 1 ? "1 needs attention" : failuresToday + " need attention");
            failuresContext.setStyle("-fx-text-fill: -color-error; -fx-font-weight: bold;");
            if (failuresCard != null && !failuresCard.getStyleClass().contains("stat-card-error")) {
                failuresCard.getStyleClass().add("stat-card-error");
            }
        }

        updateChart(allRuns);
        updateRecentFeed(allRuns);
    }

    private void updateChart(List<RunRecord> runs) {
        activityChart.getData().clear();

        Map<LocalDate, Integer> dayCounts = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            dayCounts.put(today.minusDays(i), 0);
        }

        for (RunRecord r : runs) {
            LocalDate d = r.startedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            if (dayCounts.containsKey(d)) {
                dayCounts.put(d, dayCounts.get(d) + 1);
            }
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<LocalDate, Integer> entry : dayCounts.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey().format(DAY_FORMAT), entry.getValue()));
        }

        activityChart.getData().add(series);
    }

    private void updateRecentFeed(List<RunRecord> runs) {
        recentFeedContainer.getChildren().clear();

        if (runs.isEmpty()) {
            VBox empty = new VBox(6);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(32, 12, 32, 12));
            Label emptyTitle = new Label("No recent runs");
            emptyTitle.getStyleClass().add("section-title");
            Label emptySub = new Label("Activity appears here live as workflows run.");
            emptySub.getStyleClass().add("secondary-text");
            empty.getChildren().addAll(emptyTitle, emptySub);
            recentFeedContainer.getChildren().add(empty);
            return;
        }

        int max = Math.min(5, runs.size());
        for (int i = 0; i < max; i++) {
            RunRecord r = runs.get(i);
            HBox item = new HBox(12);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setPadding(new Insets(10, 14, 10, 14));
            item.getStyleClass().addAll("card", "list-row");

            VBox info = new VBox(2);
            Label name = new Label(r.workflowName());
            name.getStyleClass().add("body-text");
            name.setStyle("-fx-font-weight: bold;");

            String triggerFriendly = PluginDisplayNameResolver.resolve(r.triggerSummary());
            Label timeStr = new Label(triggerFriendly + " • " + TIME_FORMAT.format(r.startedAt()) + " (" + r.totalDurationMs() + " ms)");
            timeStr.getStyleClass().add("caption-text");
            info.getChildren().addAll(name, timeStr);

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label chip = new Label(r.outcome().name());
            chip.getStyleClass().addAll("chip", switch (r.outcome()) {
                case SUCCEEDED -> "chip-succeeded";
                case FAILED -> "chip-failed";
                case RUNNING -> "chip-running";
                case SKIPPED -> "chip-warning";
                default -> "chip-disabled";
            });

            // Text only: NO icon
            item.getChildren().addAll(info, sp, chip);

            if (onNavigateActivity != null) {
                item.setCursor(javafx.scene.Cursor.HAND);
                item.setOnMouseClicked(e -> onNavigateActivity.run());
            }

            recentFeedContainer.getChildren().add(item);
        }

        UiMotion.staggerFadeIn(recentFeedContainer.getChildren());
    }

    public Node getNode() {
        return scrollPane;
    }

    public Label getRunsTodayVal() {
        return runsTodayVal;
    }

    public Label getSuccessRateVal() {
        return successRateVal;
    }

    public Label getSuccessRateContext() {
        return successRateContext;
    }

    public Label getActiveWorkflowsVal() {
        return activeWorkflowsVal;
    }

    public Label getFailuresVal() {
        return failuresVal;
    }

    public Label getFailuresContext() {
        return failuresContext;
    }
}
