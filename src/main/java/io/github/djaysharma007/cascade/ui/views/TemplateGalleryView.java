package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.ConditionSpec;
import io.github.djaysharma007.cascade.core.PluginSpec;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.ui.components.CascadeIcons;
import io.github.djaysharma007.cascade.ui.components.PluginDisplayNameResolver;
import io.github.djaysharma007.cascade.ui.components.UiMotion;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Template gallery providing ready-made workflows for quick creation.
 */
public final class TemplateGalleryView {

    private final Consumer<Workflow> onUseTemplate;
    private final List<WorkflowTemplate> allTemplates = new ArrayList<>();

    private final VBox root = new VBox(20);
    private final TextField searchField = new TextField();
    private final FlowPane cardsContainer = new FlowPane();
    private String selectedCategory = "All";

    public TemplateGalleryView(Consumer<Workflow> onUseTemplate) {
        this.onUseTemplate = Objects.requireNonNull(onUseTemplate);
        initTemplates();
        buildUI();
    }

    private void initTemplates() {
        String userHome = System.getProperty("user.home", "C:\\Users\\User");
        String downloadsDir = Path.of(userHome, "Downloads").toString();
        String documentsDir = Path.of(userHome, "Documents").toString();
        String pdfsDir = Path.of(userHome, "Documents", "PDFs").toString();
        String picturesDir = Path.of(userHome, "Pictures").toString();
        String screenshotsDir = Path.of(userHome, "Pictures", "Screenshots").toString();
        String desktopDir = Path.of(userHome, "Desktop").toString();
        String videosArchiveDir = Path.of(userHome, "Videos", "Archive").toString();
        String reportsArchiveDir = Path.of(userHome, "Documents", "Archive").toString();

        // 1. Sort downloads by file type
        Workflow wf1 = Workflow.builder()
                .id(WorkflowId.random())
                .name("Sort downloads by file type")
                .enabled(true)
                .trigger(PluginSpec.of("file-created", Map.of("directory", downloadsDir)))
                .conditions(ConditionSpec.leaf("extension-is", Map.of("extension", "pdf")))
                .addAction(PluginSpec.of("move-file", Map.of("destination", pdfsDir)))
                .build();
        allTemplates.add(new WorkflowTemplate(
                "sort-downloads-by-type",
                "Sort downloads by file type",
                "Automatically route downloaded PDF documents into your Documents/PDFs folder.",
                "File Management",
                "folder",
                wf1
        ));

        // 2. Move screenshots to folder
        Workflow wf2 = Workflow.builder()
                .id(WorkflowId.random())
                .name("Move screenshots to folder")
                .enabled(true)
                .trigger(PluginSpec.of("file-created", Map.of("directory", picturesDir)))
                .conditions(ConditionSpec.leaf("name-contains", Map.of("substring", "Screenshot")))
                .addAction(PluginSpec.of("move-file", Map.of("destination", screenshotsDir)))
                .build();
        allTemplates.add(new WorkflowTemplate(
                "move-screenshots",
                "Move screenshots to folder",
                "Collect desktop and picture screenshots into a dedicated Screenshots directory.",
                "Organization",
                "file",
                wf2
        ));

        // 3. Archive large video files
        Workflow wf3 = Workflow.builder()
                .id(WorkflowId.random())
                .name("Archive large video files")
                .enabled(true)
                .trigger(PluginSpec.of("file-created", Map.of("directory", downloadsDir)))
                .conditions(ConditionSpec.leaf("file-size-over", Map.of("bytes", 524288000L)))
                .addAction(PluginSpec.of("move-file", Map.of("destination", videosArchiveDir)))
                .build();
        allTemplates.add(new WorkflowTemplate(
                "archive-large-videos",
                "Archive large video files",
                "Identify video files over 500 MB in your staging folder and relocate them to archive storage.",
                "Storage",
                "folder",
                wf3
        ));

        // 4. Desktop cleanup notify
        Workflow wf4 = Workflow.builder()
                .id(WorkflowId.random())
                .name("Desktop cleanup notify")
                .enabled(true)
                .trigger(PluginSpec.of("file-created", Map.of("directory", desktopDir)))
                .conditions(ConditionSpec.leaf("extension-is", Map.of("extension", "tmp")))
                .addAction(PluginSpec.of("print-message", Map.of("text", "Temporary desktop file detected: cleaning queue.")))
                .build();
        allTemplates.add(new WorkflowTemplate(
                "desktop-cleanup-notify",
                "Desktop cleanup notify",
                "Notify and log a message when temporary files accumulate on the desktop.",
                "Productivity",
                "clock",
                wf4
        ));

        // 5. Hourly system heartbeat
        Workflow wf5 = Workflow.builder()
                .id(WorkflowId.random())
                .name("Hourly system heartbeat")
                .enabled(true)
                .trigger(PluginSpec.of("timer", Map.of("interval", 3600)))
                .addAction(PluginSpec.of("print-message", Map.of("text", "System heartbeat: Cascade background runner active.")))
                .build();
        allTemplates.add(new WorkflowTemplate(
                "hourly-heartbeat",
                "Hourly system heartbeat",
                "Run an hourly heartbeat check that logs system activity status.",
                "Monitoring",
                "clock",
                wf5
        ));

        // 6. Process incoming reports
        Workflow wf6 = Workflow.builder()
                .id(WorkflowId.random())
                .name("Process incoming reports")
                .enabled(true)
                .trigger(PluginSpec.of("file-created", Map.of("directory", documentsDir)))
                .conditions(ConditionSpec.leaf("name-contains", Map.of("substring", "Report")))
                .addAction(PluginSpec.of("print-message", Map.of("text", "Incoming report detected and queued.")))
                .addAction(PluginSpec.of("move-file", Map.of("destination", reportsArchiveDir)))
                .build();
        allTemplates.add(new WorkflowTemplate(
                "process-incoming-reports",
                "Process incoming reports",
                "Match incoming financial reports and route them to your Records archive.",
                "Organization",
                "file",
                wf6
        ));
    }

    private void buildUI() {
        root.setPadding(new Insets(24));
        root.getStyleClass().add("page-view");

        // Header section
        VBox headerBox = new VBox(6);
        Label titleLabel = new Label("Template Gallery");
        titleLabel.getStyleClass().add("page-title");
        Label subtitleLabel = new Label("Choose a pre-built workflow to kickstart your desktop automation.");
        subtitleLabel.getStyleClass().add("caption-text");
        headerBox.getChildren().addAll(titleLabel, subtitleLabel);

        // Filter / Search bar
        HBox filterBar = new HBox(12);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Search templates...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(260);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> renderCards());

        HBox categoryPills = new HBox(8);
        categoryPills.setAlignment(Pos.CENTER_LEFT);
        String[] categories = {"All", "File Management", "Organization", "Productivity", "Storage", "Monitoring"};
        for (String cat : categories) {
            Button pill = new Button(cat);
            pill.getStyleClass().add("filter-pill");
            if (cat.equals(selectedCategory)) {
                pill.getStyleClass().add("filter-pill-active");
            }
            UiMotion.animateHover(pill);
            pill.setOnAction(e -> {
                selectedCategory = cat;
                categoryPills.getChildren().forEach(n -> n.getStyleClass().remove("filter-pill-active"));
                pill.getStyleClass().add("filter-pill-active");
                renderCards();
            });
            categoryPills.getChildren().add(pill);
        }

        filterBar.getChildren().addAll(searchField, categoryPills);

        // Cards grid
        cardsContainer.setHgap(16);
        cardsContainer.setVgap(16);
        cardsContainer.setPrefWrapLength(920);

        ScrollPane scrollPane = new ScrollPane(cardsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("flow-scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.getChildren().addAll(headerBox, filterBar, scrollPane);

        renderCards();
    }

    private void renderCards() {
        cardsContainer.getChildren().clear();
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        List<WorkflowTemplate> filtered = allTemplates.stream()
                .filter(t -> selectedCategory.equals("All") || t.category().equalsIgnoreCase(selectedCategory))
                .filter(t -> query.isEmpty()
                        || t.title().toLowerCase().contains(query)
                        || t.description().toLowerCase().contains(query)
                        || t.category().toLowerCase().contains(query))
                .toList();

        if (filtered.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(48));
            emptyBox.setPrefWidth(800);
            Label emptyTitle = new Label("No matching templates found");
            emptyTitle.getStyleClass().add("card-title");
            Label emptySub = new Label("Try adjusting your search or category filters.");
            emptySub.getStyleClass().add("caption-text");
            emptyBox.getChildren().addAll(emptyTitle, emptySub);
            cardsContainer.getChildren().add(emptyBox);
            return;
        }

        for (WorkflowTemplate template : filtered) {
            VBox card = createTemplateCard(template);
            cardsContainer.getChildren().add(card);
            UiMotion.pageTransition(card);
        }
    }

    private VBox createTemplateCard(WorkflowTemplate template) {
        VBox card = new VBox(14);
        card.getStyleClass().add("template-card");
        card.setPrefWidth(290);
        card.setMaxWidth(340);
        card.setMinHeight(260);

        // Top row: Icon + Title + Category Chip
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        HBox iconBadge = new HBox(CascadeIcons.icon(template.iconName(), 0.9, "icon-teal"));
        iconBadge.getStyleClass().add("template-icon-badge");
        iconBadge.setAlignment(Pos.CENTER);

        VBox titleBox = new VBox(2);
        Label titleLabel = new Label(template.title());
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setWrapText(true);

        Label catChip = new Label(template.category());
        catChip.getStyleClass().add("template-category-chip");

        titleBox.getChildren().addAll(titleLabel, catChip);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        topRow.getChildren().addAll(iconBadge, titleBox);

        // Description
        Label descLabel = new Label(template.description());
        descLabel.getStyleClass().add("body-text");
        descLabel.setWrapText(true);
        descLabel.setMinHeight(48);

        // Step summary tags
        VBox stepsBox = new VBox(6);
        Workflow wf = template.templateWorkflow();

        // Trigger tag
        String triggerDisplay = PluginDisplayNameResolver.resolve(wf.trigger().pluginId().value());
        Label triggerTag = new Label("Trigger: " + triggerDisplay);
        triggerTag.getStyleClass().addAll("step-badge", "step-badge-trigger");

        stepsBox.getChildren().add(triggerTag);

        // Condition tag if present
        wf.conditionSpec().ifPresent(c -> {
            String cName = PluginDisplayNameResolver.resolve(c.type());
            Label condTag = new Label("Condition: " + cName);
            condTag.getStyleClass().addAll("step-badge", "step-badge-condition");
            stepsBox.getChildren().add(condTag);
        });

        // Action tags
        for (int i = 0; i < wf.actions().size(); i++) {
            String aName = PluginDisplayNameResolver.resolve(wf.actions().get(i).pluginId().value());
            Label actTag = new Label("Action " + (i + 1) + ": " + aName);
            actTag.getStyleClass().addAll("step-badge", "step-badge-action");
            stepsBox.getChildren().add(actTag);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Bottom action button
        Button useBtn = new Button("Use template");
        useBtn.setGraphic(CascadeIcons.icon("plus", 0.8, "icon-primary"));
        useBtn.getStyleClass().addAll("button-primary");
        useBtn.setMaxWidth(Double.MAX_VALUE);
        UiMotion.animateHover(useBtn);

        useBtn.setOnAction(e -> onUseTemplate.accept(template.instantiate()));

        card.getChildren().addAll(topRow, descLabel, stepsBox, spacer, useBtn);
        return card;
    }

    public Node getNode() {
        return root;
    }

    public int getTemplateCount() {
        return allTemplates.size();
    }

    public List<WorkflowTemplate> getTemplates() {
        return List.copyOf(allTemplates);
    }

    public void selectCategory(String category) {
        this.selectedCategory = category;
        renderCards();
    }

    public void search(String query) {
        this.searchField.setText(query);
    }

    public void useTemplate(String templateId) {
        allTemplates.stream()
                .filter(t -> t.id().equals(templateId))
                .findFirst()
                .ifPresent(t -> onUseTemplate.accept(t.instantiate()));
    }
}
