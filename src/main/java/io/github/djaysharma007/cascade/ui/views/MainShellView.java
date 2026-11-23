package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.core.WorkflowState;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.EngineListener;
import io.github.djaysharma007.cascade.engine.RunRecord;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
import io.github.djaysharma007.cascade.ui.components.CascadeIcons;
import io.github.djaysharma007.cascade.ui.components.ToastOverlay;
import io.github.djaysharma007.cascade.ui.components.UiMotion;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Main application shell containing top bar, navigation sidebar, views, and toast overlay.
 */
public final class MainShellView implements EngineListener {

    private final EngineFacade facade;
    private final WorkflowRepository repository;
    private final PluginRegistry registry;

    private final StackPane shellContainer = new StackPane();
    private final BorderPane root = new BorderPane();
    private final StackPane contentArea = new StackPane();
    private final ToastOverlay toastOverlay = new ToastOverlay();

    private final Label viewTitleLabel = new Label("Dashboard");
    private final Button engineToggleBtn = new Button("Start engine");
    private final Label engineChip = new Label("Stopped");
    private Timeline pulseTimeline;

    private final HBox navDashboard = new HBox(8);
    private final HBox navWorkflows = new HBox(8);
    private final HBox navActivity = new HBox(8);

    private final Label statusWorkflowCount = new Label();
    private final Label statusEngineState = new Label();

    private DashboardView dashboardView;
    private WorkflowsView workflowsView;
    private ActivityView activityView;
    private WorkflowEditorView editorView;

    public MainShellView(EngineFacade facade, WorkflowRepository repository, PluginRegistry registry) {
        this.facade = facade;
        this.repository = repository;
        this.registry = registry;

        facade.addListener(this);
        initViews();
        buildShell();
        updateEngineStatus(facade.isRunning());
        updateStatusCounts();
    }

    private void initViews() {
        dashboardView = new DashboardView(
                facade,
                repository,
                this::showActivityView,
                this::showWorkflowsView,
                this::openNewWorkflowEditor
        );
        workflowsView = new WorkflowsView(
                facade,
                repository,
                this::openNewWorkflowEditor,
                this::openEditWorkflowEditor
        );
        activityView = new ActivityView(facade);
    }

    private void buildShell() {
        root.getStyleClass().add("root");

        // Top bar
        HBox topBar = createTopBar();
        root.setTop(topBar);

        // Sidebar
        VBox sidebar = createSidebar();
        root.setLeft(sidebar);

        // Default to Dashboard
        showDashboardView();

        // Status bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);

        shellContainer.getChildren().addAll(root, toastOverlay);
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(16);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);

        // App Mark: 3 descending rounded bars in accent-bright
        HBox appMark = new HBox(3);
        appMark.setAlignment(Pos.CENTER);
        for (int h : new int[]{18, 14, 10}) {
            Rectangle bar = new Rectangle(3, h);
            bar.setArcWidth(2);
            bar.setArcHeight(2);
            bar.setFill(Color.web("#64E9EE"));
            appMark.getChildren().add(bar);
        }

        Label brandLabel = new Label("Cascade");
        brandLabel.getStyleClass().add("section-title");

        HBox brandBox = new HBox(8, appMark, brandLabel);
        brandBox.setAlignment(Pos.CENTER_LEFT);

        viewTitleLabel.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        engineChip.getStyleClass().addAll("chip", "chip-stopped");

        engineToggleBtn.setGraphic(CascadeIcons.icon("play", 0.85, "icon-primary"));
        engineToggleBtn.getStyleClass().add("button-primary");
        UiMotion.animateHover(engineToggleBtn);
        engineToggleBtn.setOnAction(e -> {
            if (facade.isRunning()) {
                facade.stop();
            } else {
                facade.start();
            }
        });

        topBar.getChildren().addAll(brandBox, viewTitleLabel, spacer, engineChip, engineToggleBtn);
        return topBar;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.getStyleClass().add("sidebar");

        Button newWfBtn = new Button("New workflow");
        newWfBtn.setGraphic(CascadeIcons.icon("plus", 0.9, "icon-primary"));
        newWfBtn.getStyleClass().add("button-primary");
        newWfBtn.setMaxWidth(Double.MAX_VALUE);
        UiMotion.animateHover(newWfBtn);
        newWfBtn.setOnAction(e -> openNewWorkflowEditor());

        Region sep = new Region();
        sep.setMinHeight(16);

        // Nav Dashboard
        navDashboard.setAlignment(Pos.CENTER_LEFT);
        navDashboard.getStyleClass().addAll("nav-item", "nav-item-active");
        Label dashLabel = new Label("Dashboard");
        dashLabel.getStyleClass().add("body-text");
        navDashboard.getChildren().addAll(CascadeIcons.icon("dashboard", 0.9, "icon-secondary"), dashLabel);
        navDashboard.setOnMouseClicked(e -> showDashboardView());

        // Nav Workflows
        navWorkflows.setAlignment(Pos.CENTER_LEFT);
        navWorkflows.getStyleClass().addAll("nav-item");
        Label wfLabel = new Label("Workflows");
        wfLabel.getStyleClass().add("body-text");
        navWorkflows.getChildren().addAll(CascadeIcons.icon("workflows", 0.9, "icon-secondary"), wfLabel);
        navWorkflows.setOnMouseClicked(e -> showWorkflowsView());

        // Nav Activity
        navActivity.setAlignment(Pos.CENTER_LEFT);
        navActivity.getStyleClass().addAll("nav-item");
        Label actLabel = new Label("Activity");
        actLabel.getStyleClass().add("body-text");
        navActivity.getChildren().addAll(CascadeIcons.icon("activity", 0.9, "icon-secondary"), actLabel);
        navActivity.setOnMouseClicked(e -> showActivityView());

        sidebar.getChildren().addAll(newWfBtn, sep, navDashboard, navWorkflows, navActivity);
        return sidebar;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(16);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        statusWorkflowCount.getStyleClass().add("caption-text");
        statusEngineState.getStyleClass().add("caption-text");

        statusBar.getChildren().addAll(statusWorkflowCount, statusEngineState);
        return statusBar;
    }

    public void showDashboardView() {
        viewTitleLabel.setText("Dashboard");
        if (!navDashboard.getStyleClass().contains("nav-item-active")) {
            navDashboard.getStyleClass().add("nav-item-active");
        }
        navWorkflows.getStyleClass().remove("nav-item-active");
        navActivity.getStyleClass().remove("nav-item-active");
        dashboardView.refresh();
        contentArea.getChildren().setAll(dashboardView.getNode());
        UiMotion.pageTransition(dashboardView.getNode());
        updateStatusCounts();
    }

    public void showWorkflowsView() {
        viewTitleLabel.setText("Workflows");
        navWorkflows.getStyleClass().add("nav-item-active");
        navDashboard.getStyleClass().remove("nav-item-active");
        navActivity.getStyleClass().remove("nav-item-active");
        workflowsView.refresh();
        contentArea.getChildren().setAll(workflowsView.getNode());
        UiMotion.pageTransition(workflowsView.getNode());
        updateStatusCounts();
    }

    public void showActivityView() {
        viewTitleLabel.setText("Activity");
        navActivity.getStyleClass().add("nav-item-active");
        navDashboard.getStyleClass().remove("nav-item-active");
        navWorkflows.getStyleClass().remove("nav-item-active");
        activityView.refresh();
        contentArea.getChildren().setAll(activityView.getNode());
        UiMotion.pageTransition(activityView.getNode());
    }

    public void openNewWorkflowEditor() {
        viewTitleLabel.setText("New workflow");
        navDashboard.getStyleClass().remove("nav-item-active");
        navWorkflows.getStyleClass().remove("nav-item-active");
        navActivity.getStyleClass().remove("nav-item-active");

        editorView = new WorkflowEditorView(
                facade,
                repository,
                registry,
                null,
                this::showWorkflowsView
        );
        contentArea.getChildren().setAll(editorView.getNode());
        UiMotion.pageTransition(editorView.getNode());
    }

    public void openEditWorkflowEditor(Workflow workflow) {
        viewTitleLabel.setText("Edit workflow");
        navDashboard.getStyleClass().remove("nav-item-active");
        navWorkflows.getStyleClass().remove("nav-item-active");
        navActivity.getStyleClass().remove("nav-item-active");

        editorView = new WorkflowEditorView(
                facade,
                repository,
                registry,
                workflow,
                this::showWorkflowsView
        );
        contentArea.getChildren().setAll(editorView.getNode());
        UiMotion.pageTransition(editorView.getNode());
    }

    private void updateEngineStatus(boolean running) {
        if (pulseTimeline != null) {
            pulseTimeline.stop();
            pulseTimeline = null;
        }

        if (running) {
            engineChip.setText("Running");
            engineChip.getStyleClass().setAll("chip", "chip-running");
            engineToggleBtn.setText("Stop engine");
            engineToggleBtn.setGraphic(CascadeIcons.icon("stop", 0.85, "icon-primary"));
            statusEngineState.setText("Engine running");
            pulseTimeline = UiMotion.pulse(engineChip);
        } else {
            engineChip.setText("Stopped");
            engineChip.getStyleClass().setAll("chip", "chip-stopped");
            engineChip.setOpacity(1.0);
            engineToggleBtn.setText("Start engine");
            engineToggleBtn.setGraphic(CascadeIcons.icon("play", 0.85, "icon-primary"));
            statusEngineState.setText("Engine stopped");
        }
    }

    private void updateStatusCounts() {
        long enabledCount = facade.allWorkflows().stream().filter(Workflow::enabled).count();
        statusWorkflowCount.setText(enabledCount + " workflow" + (enabledCount == 1 ? "" : "s") + " enabled");
    }

    public void showToast(String message, ToastOverlay.Type type) {
        toastOverlay.showToast(message, type);
    }

    @Override
    public void onEngineStateChanged(boolean running) {
        Platform.runLater(() -> updateEngineStatus(running));
    }

    @Override
    public void onWorkflowStateChanged(WorkflowId workflowId, WorkflowState state) {
        Platform.runLater(this::updateStatusCounts);
    }

    @Override
    public void onRunUpdated(RunRecord record) {
        Platform.runLater(() -> {
            dashboardView.refresh();
            activityView.refresh();
            workflowsView.refresh();
        });
    }

    public Node getNode() {
        return shellContainer;
    }

    public DashboardView getDashboardView() {
        return dashboardView;
    }

    public WorkflowsView getWorkflowsView() {
        return workflowsView;
    }

    public ActivityView getActivityView() {
        return activityView;
    }

    public ToastOverlay getToastOverlay() {
        return toastOverlay;
    }
}
