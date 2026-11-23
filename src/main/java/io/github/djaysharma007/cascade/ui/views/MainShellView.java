package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.core.WorkflowState;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.EngineListener;
import io.github.djaysharma007.cascade.engine.RunRecord;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
import io.github.djaysharma007.cascade.settings.SettingsService;
import io.github.djaysharma007.cascade.ui.components.CascadeIcons;
import io.github.djaysharma007.cascade.ui.components.CommandPaletteDialog;
import io.github.djaysharma007.cascade.ui.components.ToastOverlay;
import io.github.djaysharma007.cascade.ui.components.UiMotion;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.nio.file.Path;

/**
 * Main application shell containing top bar, navigation sidebar, views, and toast overlay.
 */
public final class MainShellView implements EngineListener {

    private final EngineFacade facade;
    private final WorkflowRepository repository;
    private final PluginRegistry registry;
    private final SettingsService settingsService;

    private final StackPane shellContainer = new StackPane();
    private final BorderPane root = new BorderPane();
    private final StackPane contentArea = new StackPane();
    private final ToastOverlay toastOverlay = new ToastOverlay();
    private CommandPaletteDialog commandPalette;

    private final Label viewTitleLabel = new Label("Dashboard");
    private final Button engineToggleBtn = new Button("Start engine");
    private final Label engineChip = new Label("Stopped");
    private Timeline pulseTimeline;

    private final HBox navDashboard = new HBox(8);
    private final HBox navWorkflows = new HBox(8);
    private final HBox navTemplates = new HBox(8);
    private final HBox navActivity = new HBox(8);
    private final HBox navSettings = new HBox(8);

    private final Label statusWorkflowCount = new Label();
    private final Label statusEngineState = new Label();

    private DashboardView dashboardView;
    private WorkflowsView workflowsView;
    private TemplateGalleryView templateGalleryView;
    private ActivityView activityView;
    private WorkflowEditorView editorView;
    private SettingsView settingsView;

    public MainShellView(EngineFacade facade, WorkflowRepository repository, PluginRegistry registry) {
        this(facade, repository, registry, new SettingsService(Path.of(System.getProperty("user.home"), ".cascade", "settings.json")));
    }

    public MainShellView(EngineFacade facade, WorkflowRepository repository, PluginRegistry registry, SettingsService settingsService) {
        this.facade = facade;
        this.repository = repository;
        this.registry = registry;
        this.settingsService = settingsService;

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
        templateGalleryView = new TemplateGalleryView(this::openWorkflowEditorFromTemplate);
        activityView = new ActivityView(facade, this::showToast);
        settingsView = new SettingsView(settingsService, repository, facade, this::showToast);
    }

    private void buildShell() {
        root.getStyleClass().add("root");

        // Top bar
        HBox topBar = createTopBar();
        root.setTop(topBar);

        // Sidebar
        VBox sidebar = createSidebar();
        root.setLeft(sidebar);

        // Center content area
        root.setCenter(contentArea);

        // Default to Dashboard
        showDashboardView();

        // Status bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);

        // Command palette modal overlay
        commandPalette = new CommandPaletteDialog(() -> {});

        // Shortcut Ctrl+K
        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.K) {
                toggleCommandPalette();
                event.consume();
            }
        });

        shellContainer.getChildren().addAll(root, toastOverlay, commandPalette);
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

        // Quick search button for command palette
        Button quickSearchBtn = new Button();
        quickSearchBtn.getStyleClass().add("button-secondary");
        Label qsLabel = new Label("Search or commands...");
        qsLabel.getStyleClass().add("caption-text");
        Label kbdBadge = new Label("Ctrl+K");
        kbdBadge.getStyleClass().add("template-category-chip");
        HBox qsBox = new HBox(8, CascadeIcons.icon("search", 0.8, "icon-secondary"), qsLabel, kbdBadge);
        qsBox.setAlignment(Pos.CENTER_LEFT);
        quickSearchBtn.setGraphic(qsBox);
        UiMotion.animateHover(quickSearchBtn);
        quickSearchBtn.setOnAction(e -> toggleCommandPalette());

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

        topBar.getChildren().addAll(brandBox, quickSearchBtn, viewTitleLabel, spacer, engineChip, engineToggleBtn);
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

        // Nav Templates
        navTemplates.setAlignment(Pos.CENTER_LEFT);
        navTemplates.getStyleClass().addAll("nav-item");
        Label tplLabel = new Label("Templates");
        tplLabel.getStyleClass().add("body-text");
        navTemplates.getChildren().addAll(CascadeIcons.icon("templates", 0.9, "icon-secondary"), tplLabel);
        navTemplates.setOnMouseClicked(e -> showTemplateGalleryView());

        // Nav Activity
        navActivity.setAlignment(Pos.CENTER_LEFT);
        navActivity.getStyleClass().addAll("nav-item");
        Label actLabel = new Label("Activity");
        actLabel.getStyleClass().add("body-text");
        navActivity.getChildren().addAll(CascadeIcons.icon("activity", 0.9, "icon-secondary"), actLabel);
        navActivity.setOnMouseClicked(e -> showActivityView());

        // Nav Settings
        navSettings.setAlignment(Pos.CENTER_LEFT);
        navSettings.getStyleClass().addAll("nav-item");
        Label setLabel = new Label("Settings");
        setLabel.getStyleClass().add("body-text");
        navSettings.getChildren().addAll(CascadeIcons.icon("settings", 0.9, "icon-secondary"), setLabel);
        navSettings.setOnMouseClicked(e -> showSettingsView());

        sidebar.getChildren().addAll(newWfBtn, sep, navDashboard, navWorkflows, navTemplates, navActivity, navSettings);
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
        navTemplates.getStyleClass().remove("nav-item-active");
        navActivity.getStyleClass().remove("nav-item-active");
        navSettings.getStyleClass().remove("nav-item-active");
        dashboardView.refresh();
        contentArea.getChildren().setAll(dashboardView.getNode());
        UiMotion.pageTransition(dashboardView.getNode());
        updateStatusCounts();
    }

    public void showWorkflowsView() {
        viewTitleLabel.setText("Workflows");
        navWorkflows.getStyleClass().add("nav-item-active");
        navDashboard.getStyleClass().remove("nav-item-active");
        navTemplates.getStyleClass().remove("nav-item-active");
        navActivity.getStyleClass().remove("nav-item-active");
        navSettings.getStyleClass().remove("nav-item-active");
        workflowsView.refresh();
        contentArea.getChildren().setAll(workflowsView.getNode());
        UiMotion.pageTransition(workflowsView.getNode());
        updateStatusCounts();
    }

    public void showTemplateGalleryView() {
        viewTitleLabel.setText("Templates");
        navTemplates.getStyleClass().add("nav-item-active");
        navDashboard.getStyleClass().remove("nav-item-active");
        navWorkflows.getStyleClass().remove("nav-item-active");
        navActivity.getStyleClass().remove("nav-item-active");
        navSettings.getStyleClass().remove("nav-item-active");
        contentArea.getChildren().setAll(templateGalleryView.getNode());
        UiMotion.pageTransition(templateGalleryView.getNode());
    }

    public void showActivityView() {
        viewTitleLabel.setText("Activity");
        navActivity.getStyleClass().add("nav-item-active");
        navDashboard.getStyleClass().remove("nav-item-active");
        navWorkflows.getStyleClass().remove("nav-item-active");
        navTemplates.getStyleClass().remove("nav-item-active");
        navSettings.getStyleClass().remove("nav-item-active");
        activityView.refresh();
        contentArea.getChildren().setAll(activityView.getNode());
        UiMotion.pageTransition(activityView.getNode());
    }

    public void showSettingsView() {
        viewTitleLabel.setText("Settings");
        navSettings.getStyleClass().add("nav-item-active");
        navDashboard.getStyleClass().remove("nav-item-active");
        navWorkflows.getStyleClass().remove("nav-item-active");
        navTemplates.getStyleClass().remove("nav-item-active");
        navActivity.getStyleClass().remove("nav-item-active");
        contentArea.getChildren().setAll(settingsView.getNode());
        UiMotion.pageTransition(settingsView.getNode());
    }

    public void openNewWorkflowEditor() {
        viewTitleLabel.setText("New workflow");
        navDashboard.getStyleClass().remove("nav-item-active");
        navWorkflows.getStyleClass().remove("nav-item-active");
        navTemplates.getStyleClass().remove("nav-item-active");
        navActivity.getStyleClass().remove("nav-item-active");
        navSettings.getStyleClass().remove("nav-item-active");

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

    public void openWorkflowEditorFromTemplate(Workflow draftWorkflow) {
        viewTitleLabel.setText("New workflow from template");
        navDashboard.getStyleClass().remove("nav-item-active");
        navWorkflows.getStyleClass().remove("nav-item-active");
        navTemplates.getStyleClass().remove("nav-item-active");
        navActivity.getStyleClass().remove("nav-item-active");
        navSettings.getStyleClass().remove("nav-item-active");

        editorView = new WorkflowEditorView(
                facade,
                repository,
                registry,
                draftWorkflow,
                () -> {
                    showWorkflowsView();
                    toastOverlay.showToast("Workflow created from template", ToastOverlay.Type.SUCCESS);
                }
        );
        contentArea.getChildren().setAll(editorView.getNode());
        UiMotion.pageTransition(editorView.getNode());
    }

    public void openEditWorkflowEditor(Workflow workflow) {
        viewTitleLabel.setText("Edit workflow");
        navDashboard.getStyleClass().remove("nav-item-active");
        navWorkflows.getStyleClass().remove("nav-item-active");
        navTemplates.getStyleClass().remove("nav-item-active");
        navActivity.getStyleClass().remove("nav-item-active");
        navSettings.getStyleClass().remove("nav-item-active");

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

    public void toggleCommandPalette() {
        if (commandPalette.isOpen()) {
            commandPalette.close();
        } else {
            commandPalette.open(
                    facade,
                    this::showDashboardView,
                    this::showWorkflowsView,
                    this::showTemplateGalleryView,
                    this::showActivityView,
                    this::showSettingsView,
                    this::openNewWorkflowEditor,
                    this::openEditWorkflowEditor,
                    this::openWorkflowEditorFromTemplate,
                    templateGalleryView::getTemplates
            );
        }
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

    public TemplateGalleryView getTemplateGalleryView() {
        return templateGalleryView;
    }

    public ActivityView getActivityView() {
        return activityView;
    }

    public SettingsView getSettingsView() {
        return settingsView;
    }

    public CommandPaletteDialog getCommandPalette() {
        return commandPalette;
    }

    public ToastOverlay getToastOverlay() {
        return toastOverlay;
    }
}

