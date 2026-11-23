package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.RunOutcome;
import io.github.djaysharma007.cascade.engine.RunRecord;
import io.github.djaysharma007.cascade.ui.components.CascadeIcons;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Dashboard home screen displaying KPI stat cards, a 7-day activity chart, and a live recent activity feed.
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
    private final VBox root = new VBox(20);

    private final Label runsTodayVal = new Label("0");
    private final Label successRateVal = new Label("100%");
    private final Label activeWorkflowsVal = new Label("0");
    private final Label failuresVal = new Label("0");

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

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label("Dashboard");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("System overview, execution analytics, and live activity.");
        subtitle.getStyleClass().add("secondary-text");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newWfBtn = new Button("New workflow");
        newWfBtn.setGraphic(CascadeIcons.icon("plus", 0.85, "icon-primary"));
        newWfBtn.getStyleClass().add("button-primary");
        UiMotion.animateHover(newWfBtn);
        if (onNewWorkflow != null) {
            newWfBtn.setOnAction(e -> onNewWorkflow.run());
        }

        Button viewActBtn = new Button("View activity");
        viewActBtn.setGraphic(CascadeIcons.icon("activity", 0.85, "icon-secondary"));
        viewActBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(viewActBtn);
        if (onNavigateActivity != null) {
            viewActBtn.setOnAction(e -> onNavigateActivity.run());
        }

        header.getChildren().addAll(titleBox, spacer, viewActBtn, newWfBtn);

        // Stat Cards Row
        HBox statCardsRow = createStatCardsRow();

        // Chart & System Card Row
        HBox middleRow = new HBox(16);
        VBox chartCard = createChartCard();
        HBox.setHgrow(chartCard, Priority.ALWAYS);
        middleRow.getChildren().add(chartCard);

        // Recent Activity Card
        VBox recentCard = createRecentActivityCard();

        root.getChildren().addAll(header, statCardsRow, middleRow, recentCard);

        scrollPane.setContent(root);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
    }

    private HBox createStatCardsRow() {
        HBox row = new HBox(16);

        VBox card1 = createStatCard("Runs today", runsTodayVal, "Total executions today", "play", "icon-teal");
        VBox card2 = createStatCard("Success rate", successRateVal, "Successful completion", "check", "icon-success");
        VBox card3 = createStatCard("Active workflows", activeWorkflowsVal, "Enabled and monitoring", "workflows", "icon-teal");
        VBox card4 = createStatCard("Failures", failuresVal, "Needs attention", "close", "icon-error");

        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);
        HBox.setHgrow(card4, Priority.ALWAYS);

        row.getChildren().addAll(card1, card2, card3, card4);
        return row;
    }

    private VBox createStatCard(String title, Label valueLabel, String subtitle, String iconName, String iconClass) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "stat-card");

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-label");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        var icon = CascadeIcons.icon(iconName, 1.0, iconClass);
        top.getChildren().addAll(titleLabel, sp, icon);

        valueLabel.getStyleClass().add("stat-value");

        Label subLabel = new Label(subtitle);
        subLabel.getStyleClass().add("caption-text");

        card.getChildren().addAll(top, valueLabel, subLabel);
        return card;
    }

    private VBox createChartCard() {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("card");
        card.setPadding(new Insets(16));

        Label chartTitle = new Label("Runs over last 7 days");
        chartTitle.getStyleClass().add("section-title");

        xAxis.setAnimated(false);
        yAxis.setAnimated(false);
        yAxis.setMinorTickVisible(false);
        yAxis.setTickUnit(1);

        activityChart.setLegendVisible(false);
        activityChart.setAnimated(!UiMotion.isReduceMotion());
        activityChart.setPrefHeight(220);
        activityChart.setMinHeight(200);

        card.getChildren().addAll(chartTitle, activityChart);
        return card;
    }

    private VBox createRecentActivityCard() {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("card");
        card.setPadding(new Insets(16));

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

        int successRate = runsToday > 0 ? (int) Math.round(((double) successfulToday / runsToday) * 100.0) : 100;

        UiMotion.animateNumber(runsTodayVal, 0, (int) runsToday, "");
        successRateVal.setText(successRate + "%");
        UiMotion.animateNumber(activeWorkflowsVal, 0, (int) enabledWorkflows, "");
        UiMotion.animateNumber(failuresVal, 0, (int) failuresToday, "");

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
            VBox empty = new VBox(8);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(24));
            Label emptyTitle = new Label("No recent workflow runs");
            emptyTitle.getStyleClass().add("section-title");
            Label emptySub = new Label("Runs appear here live when triggers execute.");
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

            var icon = CascadeIcons.icon("activity", 0.85, "icon-teal");

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

            item.getChildren().addAll(icon, info, sp, chip);

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
}
