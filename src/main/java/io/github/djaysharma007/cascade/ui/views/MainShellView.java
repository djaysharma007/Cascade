package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.core.WorkflowState;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.EngineListener;
import io.github.djaysharma007.cascade.engine.RunRecord;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
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
 * Main application shell containing top bar, navigation sidebar, and views.
 */
public final class MainShellView implements EngineListener {

    private final EngineFacade facade;
    private final WorkflowRepository repository;
    private final PluginRegistry registry;

    private final BorderPane root = new BorderPane();
    private final StackPane contentArea = new StackPane();

    private final Label viewTitleLabel = new Label("Workflows");
    private final Button engineToggleBtn = new Button("Start engine");
    private final Label engineChip = new Label("Stopped");

    private final Label navWorkflows = new Label("Workflows");
    private final Label navActivity = new Label("Activity");

    private final Label statusWorkflowCount = new Label();
    private final Label statusEngineState = new Label();

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

        // Center view area
        contentArea.getChildren().add(workflowsView.getNode());
        root.setCenter(contentArea);

        // Status bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);
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

        engineToggleBtn.getStyleClass().add("button-primary");
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
        newWfBtn.getStyleClass().add("button-primary");
        newWfBtn.setMaxWidth(Double.MAX_VALUE);
        newWfBtn.setOnAction(e -> openNewWorkflowEditor());

        Region sep = new Region();
        sep.setMinHeight(16);

        navWorkflows.getStyleClass().addAll("nav-item", "nav-item-active", "body-text");
        navWorkflows.setOnMouseClicked(e -> showWorkflowsView());

        navActivity.getStyleClass().addAll("nav-item", "body-text");
        navActivity.setOnMouseClicked(e -> showActivityView());

        sidebar.getChildren().addAll(newWfBtn, sep, navWorkflows, navActivity);
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

    public void showWorkflowsView() {
        viewTitleLabel.setText("Workflows");
        navWorkflows.getStyleClass().add("nav-item-active");
        navActivity.getStyleClass().remove("nav-item-active");
        workflowsView.refresh();
        contentArea.getChildren().setAll(workflowsView.getNode());
        updateStatusCounts();
    }

    public void showActivityView() {
        viewTitleLabel.setText("Activity");
        navActivity.getStyleClass().add("nav-item-active");
        navWorkflows.getStyleClass().remove("nav-item-active");
        activityView.refresh();
        contentArea.getChildren().setAll(activityView.getNode());
    }

    public void openNewWorkflowEditor() {
        viewTitleLabel.setText("New workflow");
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
    }

    public void openEditWorkflowEditor(Workflow workflow) {
        viewTitleLabel.setText("Edit workflow");
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
    }

    private void updateEngineStatus(boolean running) {
        if (running) {
            engineChip.setText("Running");
            engineChip.getStyleClass().setAll("chip", "chip-running");
            engineToggleBtn.setText("Stop engine");
            statusEngineState.setText("Engine running");
        } else {
            engineChip.setText("Stopped");
            engineChip.getStyleClass().setAll("chip", "chip-stopped");
            engineToggleBtn.setText("Start engine");
            statusEngineState.setText("Engine stopped");
        }
    }

    private void updateStatusCounts() {
        long enabledCount = facade.allWorkflows().stream().filter(Workflow::enabled).count();
        statusWorkflowCount.setText(enabledCount + " workflow" + (enabledCount == 1 ? "" : "s") + " enabled");
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
            activityView.refresh();
            workflowsView.refresh();
        });
    }

    public Node getNode() {
        return root;
    }

    public WorkflowsView getWorkflowsView() {
        return workflowsView;
    }

    public ActivityView getActivityView() {
        return activityView;
    }
}
