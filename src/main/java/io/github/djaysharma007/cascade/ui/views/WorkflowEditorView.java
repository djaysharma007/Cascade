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
import io.github.djaysharma007.cascade.ui.components.DynamicFormBuilder;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
    private VBox triggerFormContainer = new VBox(8);
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
        VBox metaCard = new VBox(8);
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
        addCondBtn.getStyleClass().add("button-secondary");
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
        addActBtn.getStyleClass().add("button-secondary");
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
        cancelBtn.setOnAction(e -> onComplete.run());

        Button testRunBtn = new Button("Test run");
        testRunBtn.getStyleClass().add("button-secondary");
        testRunBtn.setOnAction(e -> runTestExecution());

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setOnAction(e -> saveWorkflow());

        footer.getChildren().addAll(testRunBtn, cancelBtn, saveBtn);

        root.getChildren().addAll(title, scroll, footer);
    }

    private void populateFields() {
        if (existingWorkflow != null) {
            nameField.setText(existingWorkflow.name());
            enabledCheckBox.setSelected(existingWorkflow.enabled());

            String trigId = existingWorkflow.trigger().pluginId().value();
            triggerSelector.setValue(trigId);
            rebuildTriggerForm(trigId, existingWorkflow.trigger().params());

            existingWorkflow.conditionSpec().ifPresent(cs -> {
                matchAll = !ConditionSpec.TYPE_ANY_OF.equals(cs.type());
                if (cs.isComposite()) {
                    for (ConditionSpec child : cs.children()) {
                        addConditionRow(child.type(), child.params());
                    }
                } else {
                    addConditionRow(cs.type(), cs.params());
                }
            });

            for (PluginSpec actionSpec : existingWorkflow.actions()) {
                addActionRow(actionSpec.pluginId().value(), actionSpec.params());
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

    private void addConditionRow(String conditionId, Map<String, Object> initialParams) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));
        row.getStyleClass().add("surface-panel");

        ComboBox<String> selector = new ComboBox<>();
        selector.getStyleClass().add("combo-box");
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
        removeBtn.getStyleClass().add("button-danger");

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
        row.setPadding(new Insets(8));
        row.getStyleClass().add("surface-panel");

        ComboBox<String> selector = new ComboBox<>();
        selector.getStyleClass().add("combo-box");
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
        removeBtn.getStyleClass().add("button-danger");

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
        nameErrorLabel.setVisible(false);
        nameErrorLabel.setManaged(false);

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            nameErrorLabel.setText("Workflow name must not be blank.");
            nameErrorLabel.setVisible(true);
            nameErrorLabel.setManaged(true);
            valid = false;
        } else if (name.length() > Workflow.MAX_NAME_LENGTH) {
            nameErrorLabel.setText("Workflow name cannot exceed " + Workflow.MAX_NAME_LENGTH + " characters.");
            nameErrorLabel.setVisible(true);
            nameErrorLabel.setManaged(true);
            valid = false;
        }

        String trigId = triggerSelector.getValue();
        Set<String> providedKeys = trigId != null
                ? registry.findDescriptor(trigId).map(PluginDescriptor::providedKeys).orElse(Set.of())
                : Set.of();

        if (triggerFormBuilder != null && !triggerFormBuilder.validate(providedKeys)) {
            valid = false;
        }

        for (ConditionRow cr : conditionRows) {
            if (cr.builderRef()[0] != null && !cr.builderRef()[0].validate(providedKeys)) {
                valid = false;
            }
        }

        if (actionRows.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation error");
            alert.setHeaderText("No actions configured");
            alert.setContentText("A workflow must have at least one action.");
            alert.showAndWait();
            valid = false;
        }

        for (ActionRow ar : actionRows) {
            if (ar.builderRef()[0] != null && !ar.builderRef()[0].validate(providedKeys)) {
                valid = false;
            }
        }

        return valid;
    }

    private void saveWorkflow() {
        if (!validate()) {
            return;
        }

        WorkflowId wid = existingWorkflow != null ? existingWorkflow.id() : WorkflowId.random();
        String name = nameField.getText().trim();
        boolean enabled = enabledCheckBox.isSelected();

        String trigId = triggerSelector.getValue();
        Map<String, Object> trigParams = triggerFormBuilder != null ? triggerFormBuilder.getValues() : Map.of();
        PluginSpec triggerSpec = PluginSpec.of(trigId, trigParams);

        ConditionSpec conditionSpec = null;
        if (!conditionRows.isEmpty()) {
            List<ConditionSpec> children = new ArrayList<>();
            for (ConditionRow cr : conditionRows) {
                String cId = cr.selector().getValue();
                Map<String, Object> cParams = cr.builderRef()[0] != null ? cr.builderRef()[0].getValues() : Map.of();
                children.add(ConditionSpec.leaf(cId, cParams));
            }
            conditionSpec = matchAll ? ConditionSpec.allOf(children) : ConditionSpec.anyOf(children);
        }

        List<PluginSpec> actionSpecs = new ArrayList<>();
        for (ActionRow ar : actionRows) {
            String aId = ar.selector().getValue();
            Map<String, Object> aParams = ar.builderRef()[0] != null ? ar.builderRef()[0].getValues() : Map.of();
            actionSpecs.add(PluginSpec.of(aId, aParams));
        }

        Workflow workflow = Workflow.builder()
                .id(wid)
                .name(name)
                .enabled(enabled)
                .trigger(triggerSpec)
                .conditions(conditionSpec)
                .actions(actionSpecs)
                .build();

        repository.save(workflow);
        facade.registerWorkflow(workflow);
        onComplete.run();
    }

    private void runTestExecution() {
        if (!validate()) {
            return;
        }
        // Build transient workflow and execute with sample event
        String trigId = triggerSelector.getValue();
        PluginSpec triggerSpec = PluginSpec.of(trigId, triggerFormBuilder != null ? triggerFormBuilder.getValues() : Map.of());

        List<PluginSpec> actionSpecs = new ArrayList<>();
        for (ActionRow ar : actionRows) {
            String aId = ar.selector().getValue();
            Map<String, Object> aParams = ar.builderRef()[0] != null ? ar.builderRef()[0].getValues() : Map.of();
            actionSpecs.add(PluginSpec.of(aId, aParams));
        }

        Workflow tempWf = Workflow.builder()
                .name(nameField.getText().trim())
                .trigger(triggerSpec)
                .actions(actionSpecs)
                .build();

        facade.registerWorkflow(tempWf);
        RunRecord record = facade.executeWorkflowManually(tempWf.id(), Event.empty());
        facade.unregisterWorkflow(tempWf.id());

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Test run complete");
        alert.setHeaderText("Outcome: " + record.outcome());
        alert.setContentText("Executed " + record.steps().size() + " step(s) in " + record.totalDurationMs() + " ms");
        alert.showAndWait();
    }

    public Node getNode() {
        return root;
    }
}
