package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.ConditionSpec;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.PluginCategory;
import io.github.djaysharma007.cascade.core.PluginId;
import io.github.djaysharma007.cascade.core.PluginSpec;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.RunRecord;
import io.github.djaysharma007.cascade.registry.PluginDescriptor;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
import io.github.djaysharma007.cascade.ui.components.CascadeIcons;
import io.github.djaysharma007.cascade.ui.components.DynamicFormBuilder;
import io.github.djaysharma007.cascade.ui.components.PluginDisplayNameResolver;
import io.github.djaysharma007.cascade.ui.components.UiMotion;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Visual workflow editor generating forms from plugin ParamSpecs.
 */
public final class WorkflowEditorView {

    private final EngineFacade facade;
    private final WorkflowRepository repository;
    private final PluginRegistry registry;
    private final Workflow existingWorkflow;
    private final Runnable onComplete;

    private final VBox root = new VBox(16);
    private final TextField nameField = new TextField();
    private final Label nameErrorLabel = new Label();
    private final CheckBox enabledCheckBox = new CheckBox("Enabled");

    private ComboBox<String> triggerSelector;
    private final VBox triggerFormContainer = new VBox(8);
    private DynamicFormBuilder triggerFormBuilder;

    private boolean matchAll = true;
    private final VBox conditionsContainer = new VBox(8);
    private final List<ConditionRow> conditionRows = new ArrayList<>();

    private final VBox actionsContainer = new VBox(8);
    private final List<ActionRow> actionRows = new ArrayList<>();

    private record ConditionRow(ComboBox<String> selector, VBox formBox, DynamicFormBuilder[] builderRef, HBox rowNode) {}
    private record ActionRow(ComboBox<String> selector, VBox formBox, DynamicFormBuilder[] builderRef, HBox rowNode) {}

    public WorkflowEditorView(
            EngineFacade facade,
            WorkflowRepository repository,
            PluginRegistry registry,
            Workflow existingWorkflow,
            Runnable onComplete
    ) {
        this.facade = facade;
        this.repository = repository;
        this.registry = registry;
        this.existingWorkflow = existingWorkflow;
        this.onComplete = onComplete;

        buildEditor();
        populateFields();
    }

    private void buildEditor() {
        root.setPadding(new Insets(24));
        root.getStyleClass().add("surface-panel");

        Label title = new Label(existingWorkflow == null ? "New workflow" : "Edit workflow");
        title.getStyleClass().add("page-title");

        VBox formContent = new VBox(20);

        // Name and Enabled card
        VBox metaCard = new VBox(10);
        metaCard.getStyleClass().add("card");
        metaCard.setPadding(new Insets(16));

        Label nameLabel = new Label("Workflow name *");
        nameLabel.getStyleClass().add("secondary-text");

        nameField.getStyleClass().add("text-input");
        nameField.setPromptText("Enter workflow name");
        nameErrorLabel.getStyleClass().add("field-error-label");
        nameErrorLabel.setVisible(false);
        nameErrorLabel.setManaged(false);

        metaCard.getChildren().addAll(nameLabel, nameField, nameErrorLabel, enabledCheckBox);

        // Trigger card
        VBox triggerCard = new VBox(12);
        triggerCard.getStyleClass().add("card");
        triggerCard.setPadding(new Insets(16));

        Label triggerTitle = new Label("Trigger");
        triggerTitle.getStyleClass().add("section-title");

        triggerSelector = new ComboBox<>();
        triggerSelector.getStyleClass().add("combo-box");
        configureComboBoxFriendlyNames(triggerSelector);
        for (PluginDescriptor<?> d : registry.findByCategory(PluginCategory.TRIGGER)) {
            triggerSelector.getItems().add(d.id().value());
        }
        triggerSelector.setOnAction(e -> onTriggerChanged());

        triggerCard.getChildren().addAll(triggerTitle, triggerSelector, triggerFormContainer);

        // Conditions card
        VBox conditionsCard = new VBox(12);
        conditionsCard.getStyleClass().add("card");
        conditionsCard.setPadding(new Insets(16));

        HBox conditionsHeader = new HBox(12);
        conditionsHeader.setAlignment(Pos.CENTER_LEFT);
        Label conditionsTitle = new Label("Conditions");
        conditionsTitle.getStyleClass().add("section-title");

        Button matchAllBtn = new Button("Match all");
        matchAllBtn.getStyleClass().addAll("segment-button", "segment-button-active");
        Button matchAnyBtn = new Button("Match any");
        matchAnyBtn.getStyleClass().addAll("segment-button");

        matchAllBtn.setOnAction(e -> {
            matchAll = true;
            matchAllBtn.getStyleClass().add("segment-button-active");
            matchAnyBtn.getStyleClass().remove("segment-button-active");
        });
        matchAnyBtn.setOnAction(e -> {
            matchAll = false;
            matchAnyBtn.getStyleClass().add("segment-button-active");
            matchAllBtn.getStyleClass().remove("segment-button-active");
        });

        HBox matchSegment = new HBox(matchAllBtn, matchAnyBtn);
        matchSegment.getStyleClass().add("segmented-control");

        Region condSpacer = new Region();
        HBox.setHgrow(condSpacer, Priority.ALWAYS);

        Button addCondBtn = new Button("Add condition");
        addCondBtn.setGraphic(CascadeIcons.icon("plus", 0.8, "icon-secondary"));
        addCondBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(addCondBtn);
        addCondBtn.setOnAction(e -> addConditionRow(null, null));

        conditionsHeader.getChildren().addAll(conditionsTitle, matchSegment, condSpacer, addCondBtn);
        conditionsCard.getChildren().addAll(conditionsHeader, conditionsContainer);

        // Actions card
        VBox actionsCard = new VBox(12);
        actionsCard.getStyleClass().add("card");
        actionsCard.setPadding(new Insets(16));

        HBox actionsHeader = new HBox(12);
        actionsHeader.setAlignment(Pos.CENTER_LEFT);
        Label actionsTitle = new Label("Actions");
        actionsTitle.getStyleClass().add("section-title");

        Region actSpacer = new Region();
        HBox.setHgrow(actSpacer, Priority.ALWAYS);

        Button addActBtn = new Button("Add action");
        addActBtn.setGraphic(CascadeIcons.icon("plus", 0.8, "icon-secondary"));
        addActBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(addActBtn);
        addActBtn.setOnAction(e -> addActionRow(null, null));

        actionsHeader.getChildren().addAll(actionsTitle, actSpacer, addActBtn);
        actionsCard.getChildren().addAll(actionsHeader, actionsContainer);

        formContent.getChildren().addAll(metaCard, triggerCard, conditionsCard, actionsCard);

        ScrollPane scroll = new ScrollPane(formContent);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Footer
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 0, 0, 0));

        Button cancelBtn = new Button("Discard changes");
        cancelBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(cancelBtn);
        cancelBtn.setOnAction(e -> onComplete.run());

        Button testRunBtn = new Button("Test run");
        testRunBtn.setGraphic(CascadeIcons.icon("play", 0.8, "icon-secondary"));
        testRunBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(testRunBtn);
        testRunBtn.setOnAction(e -> runTestExecution());

        Button saveBtn = new Button("Save");
        saveBtn.setGraphic(CascadeIcons.icon("check", 0.85, "icon-primary"));
        saveBtn.getStyleClass().add("button-primary");
        UiMotion.animateHover(saveBtn);
        saveBtn.setOnAction(e -> saveWorkflow());

        footer.getChildren().addAll(testRunBtn, cancelBtn, saveBtn);

        root.getChildren().addAll(title, scroll, footer);
    }

    private void configureComboBoxFriendlyNames(ComboBox<String> cb) {
        cb.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : PluginDisplayNameResolver.resolve(item));
            }
        });
        cb.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : PluginDisplayNameResolver.resolve(item));
            }
        });
    }

    private void populateFields() {
        if (existingWorkflow != null) {
            nameField.setText(existingWorkflow.name());
            enabledCheckBox.setSelected(existingWorkflow.enabled());

            String trigId = existingWorkflow.trigger().pluginId().value();
            triggerSelector.setValue(trigId);
            rebuildTriggerForm(trigId, existingWorkflow.trigger().params());

            existingWorkflow.conditionSpec().ifPresent(spec -> {
                if (ConditionSpec.TYPE_ALL_OF.equals(spec.type())) {
                    matchAll = true;
                    for (ConditionSpec child : spec.children()) {
                        addConditionFromSpec(child);
                    }
                } else if (ConditionSpec.TYPE_ANY_OF.equals(spec.type())) {
                    matchAll = false;
                    for (ConditionSpec child : spec.children()) {
                        addConditionFromSpec(child);
                    }
                } else {
                    addConditionFromSpec(spec);
                }
            });

            for (PluginSpec action : existingWorkflow.actions()) {
                addActionRow(action.pluginId().value(), action.params());
            }
        } else {
            enabledCheckBox.setSelected(true);
            if (!triggerSelector.getItems().isEmpty()) {
                triggerSelector.setValue(triggerSelector.getItems().get(0));
                onTriggerChanged();
            }
            addActionRow("print-message", Map.of("text", "Processed file"));
        }
    }

    private void onTriggerChanged() {
        String trigId = triggerSelector.getValue();
        if (trigId != null) {
            rebuildTriggerForm(trigId, Map.of());
        }
    }

    private void rebuildTriggerForm(String trigId, Map<String, Object> initialParams) {
        triggerFormContainer.getChildren().clear();
        registry.findDescriptor(trigId).ifPresent(desc -> {
            triggerFormBuilder = new DynamicFormBuilder(desc.parameters(), initialParams);
            triggerFormContainer.getChildren().add(triggerFormBuilder.getNode());
        });
    }

    private void addConditionFromSpec(ConditionSpec spec) {
        if (!spec.isComposite()) {
            addConditionRow(spec.type(), spec.params());
        }
    }

    private void addConditionRow(String conditionId, Map<String, Object> initialParams) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.getStyleClass().addAll("card", "list-row");

        ComboBox<String> selector = new ComboBox<>();
        selector.getStyleClass().add("combo-box");
        configureComboBoxFriendlyNames(selector);
        for (PluginDescriptor<?> d : registry.findByCategory(PluginCategory.CONDITION)) {
            if (!d.id().value().equals("all-of") && !d.id().value().equals("any-of") && !d.id().value().equals("not")) {
                selector.getItems().add(d.id().value());
            }
        }

        String initial = conditionId != null ? conditionId : (!selector.getItems().isEmpty() ? selector.getItems().get(0) : "");
        selector.setValue(initial);

        VBox formBox = new VBox(4);
        HBox.setHgrow(formBox, Priority.ALWAYS);
        DynamicFormBuilder[] builderRef = new DynamicFormBuilder[1];

        Runnable updateForm = () -> {
            formBox.getChildren().clear();
            String sel = selector.getValue();
            if (sel != null) {
                registry.findDescriptor(sel).ifPresent(desc -> {
                    builderRef[0] = new DynamicFormBuilder(desc.parameters(), initialParams != null ? initialParams : Map.of());
                    formBox.getChildren().add(builderRef[0].getNode());
                });
            }
        };
        selector.setOnAction(e -> updateForm.run());
        updateForm.run();

        Button removeBtn = new Button("Remove");
        removeBtn.setGraphic(CascadeIcons.icon("trash", 0.75, "icon-error"));
        removeBtn.getStyleClass().add("button-danger");
        UiMotion.animateHover(removeBtn);

        ConditionRow condRow = new ConditionRow(selector, formBox, builderRef, row);
        conditionRows.add(condRow);

        removeBtn.setOnAction(e -> {
            conditionRows.remove(condRow);
            conditionsContainer.getChildren().remove(row);
        });

        row.getChildren().addAll(selector, formBox, removeBtn);
        conditionsContainer.getChildren().add(row);
    }

    private void addActionRow(String actionId, Map<String, Object> initialParams) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.getStyleClass().addAll("card", "list-row");

        ComboBox<String> selector = new ComboBox<>();
        selector.getStyleClass().add("combo-box");
        configureComboBoxFriendlyNames(selector);
        for (PluginDescriptor<?> d : registry.findByCategory(PluginCategory.ACTION)) {
            selector.getItems().add(d.id().value());
        }

        String initial = actionId != null ? actionId : (!selector.getItems().isEmpty() ? selector.getItems().get(0) : "");
        selector.setValue(initial);

        VBox formBox = new VBox(4);
        HBox.setHgrow(formBox, Priority.ALWAYS);
        DynamicFormBuilder[] builderRef = new DynamicFormBuilder[1];

        Runnable updateForm = () -> {
            formBox.getChildren().clear();
            String sel = selector.getValue();
            if (sel != null) {
                registry.findDescriptor(sel).ifPresent(desc -> {
                    builderRef[0] = new DynamicFormBuilder(desc.parameters(), initialParams != null ? initialParams : Map.of());
                    formBox.getChildren().add(builderRef[0].getNode());
                });
            }
        };
        selector.setOnAction(e -> updateForm.run());
        updateForm.run();

        Button removeBtn = new Button("Remove");
        removeBtn.setGraphic(CascadeIcons.icon("trash", 0.75, "icon-error"));
        removeBtn.getStyleClass().add("button-danger");
        UiMotion.animateHover(removeBtn);

        ActionRow actRow = new ActionRow(selector, formBox, builderRef, row);
        actionRows.add(actRow);

        removeBtn.setOnAction(e -> {
            actionRows.remove(actRow);
            actionsContainer.getChildren().remove(row);
        });

        row.getChildren().addAll(selector, formBox, removeBtn);
        actionsContainer.getChildren().add(row);
    }

    private boolean validate() {
        boolean valid = true;
        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            nameErrorLabel.setText("Workflow name must not be blank.");
            nameErrorLabel.setVisible(true);
            nameErrorLabel.setManaged(true);
            valid = false;
        } else {
            nameErrorLabel.setVisible(false);
            nameErrorLabel.setManaged(false);
        }

        if (triggerFormBuilder != null && !triggerFormBuilder.validate()) {
            valid = false;
        }

        for (ConditionRow cr : conditionRows) {
            if (cr.builderRef()[0] != null && !cr.builderRef()[0].validate()) {
                valid = false;
            }
        }

        for (ActionRow ar : actionRows) {
            if (ar.builderRef()[0] != null && !ar.builderRef()[0].validate()) {
                valid = false;
            }
        }

        return valid;
    }

    private Workflow buildWorkflowObject() {
        WorkflowId id = existingWorkflow != null ? existingWorkflow.id() : WorkflowId.random();
        String name = nameField.getText().trim();
        boolean enabled = enabledCheckBox.isSelected();

        String trigId = triggerSelector.getValue();
        Map<String, Object> trigParams = triggerFormBuilder != null ? triggerFormBuilder.getValues() : Map.of();
        PluginSpec triggerSpec = PluginSpec.of(trigId, trigParams);

        ConditionSpec conditionSpec = null;
        if (!conditionRows.isEmpty()) {
            List<ConditionSpec> leaves = new ArrayList<>();
            for (ConditionRow cr : conditionRows) {
                String cId = cr.selector().getValue();
                Map<String, Object> cParams = cr.builderRef()[0] != null ? cr.builderRef()[0].getValues() : Map.of();
                leaves.add(ConditionSpec.leaf(cId, cParams));
            }
            if (leaves.size() == 1) {
                conditionSpec = leaves.get(0);
            } else {
                conditionSpec = matchAll ? ConditionSpec.allOf(leaves) : ConditionSpec.anyOf(leaves);
            }
        }

        List<PluginSpec> actionSpecs = new ArrayList<>();
        for (ActionRow ar : actionRows) {
            String aId = ar.selector().getValue();
            Map<String, Object> aParams = ar.builderRef()[0] != null ? ar.builderRef()[0].getValues() : Map.of();
            actionSpecs.add(PluginSpec.of(aId, aParams));
        }

        return Workflow.builder()
                .id(id)
                .name(name)
                .enabled(enabled)
                .trigger(triggerSpec)
                .conditions(conditionSpec)
                .actions(actionSpecs)
                .build();
    }

    private void saveWorkflow() {
        if (!validate()) {
            return;
        }

        Workflow wf = buildWorkflowObject();
        repository.save(wf);
        facade.registerWorkflow(wf);
        onComplete.run();
    }

    private void runTestExecution() {
        if (!validate()) {
            return;
        }

        Workflow wf = buildWorkflowObject();
        facade.registerWorkflow(wf);
        RunRecord record = facade.executeWorkflowManually(wf.id(), Event.empty());

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Test run complete");
        alert.setHeaderText("Outcome: " + record.outcome());
        alert.setContentText("Ran " + record.steps().size() + " step(s) in " + record.totalDurationMs() + " ms");
        alert.showAndWait();
    }

    public Node getNode() {
        return root;
    }
}
