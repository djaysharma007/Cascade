package io.github.djaysharma007.cascade.ui.views;

import io.github.djaysharma007.cascade.core.ConditionSpec;
import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.PluginCategory;
import io.github.djaysharma007.cascade.core.PluginSpec;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.core.WorkflowId;
import io.github.djaysharma007.cascade.core.WorkflowRepository;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.engine.RunRecord;
import io.github.djaysharma007.cascade.registry.PluginDescriptor;
import io.github.djaysharma007.cascade.registry.PluginRegistry;
import io.github.djaysharma007.cascade.ui.components.DynamicFormBuilder;
import io.github.djaysharma007.cascade.ui.components.PluginDisplayNameResolver;
import io.github.djaysharma007.cascade.ui.components.ToggleSwitch;
import io.github.djaysharma007.cascade.ui.components.UiMotion;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Visual flow editor displaying triggers, conditions, and actions as connected step cards
 * with visual connector lines, drag-to-reorder, and embedded dynamic parameter forms.
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
    private final ToggleSwitch enabledSwitch = new ToggleSwitch(true);

    // Trigger
    private ComboBox<String> triggerSelector;
    private final VBox triggerCard = new VBox(10);
    private final VBox triggerFormContainer = new VBox(8);
    private DynamicFormBuilder triggerFormBuilder;

    // Conditions
    private boolean matchAll = true;
    private final VBox conditionsCard = new VBox(10);
    private final VBox conditionsContainer = new VBox(10);
    private final List<ConditionEntry> conditionEntries = new ArrayList<>();

    // Actions
    private final VBox actionsFlowContainer = new VBox(0);
    private final List<ActionEntry> actionEntries = new ArrayList<>();

    private static final class ConditionEntry {
        final ComboBox<String> selector;
        final VBox cardNode;
        final VBox formContainer;
        DynamicFormBuilder formBuilder;

        ConditionEntry(ComboBox<String> selector, VBox cardNode, VBox formContainer) {
            this.selector = selector;
            this.cardNode = cardNode;
            this.formContainer = formContainer;
        }
    }

    private static final class ActionEntry {
        final ComboBox<String> selector;
        final VBox cardNode;
        final VBox formContainer;
        final Label stepBadge;
        final VBox wrapperNode;
        DynamicFormBuilder formBuilder;

        ActionEntry(ComboBox<String> selector, VBox cardNode, VBox formContainer, Label stepBadge, VBox wrapperNode) {
            this.selector = selector;
            this.cardNode = cardNode;
            this.formContainer = formContainer;
            this.stepBadge = stepBadge;
            this.wrapperNode = wrapperNode;
        }
    }

    public WorkflowEditorView(
            EngineFacade facade,
            WorkflowRepository repository,
            PluginRegistry registry,
            Workflow existingWorkflow,
            Runnable onComplete
    ) {
        this.facade = Objects.requireNonNull(facade);
        this.repository = Objects.requireNonNull(repository);
        this.registry = Objects.requireNonNull(registry);
        this.existingWorkflow = existingWorkflow;
        this.onComplete = onComplete;

        buildEditor();
        populateFields();
    }

    private void buildEditor() {
        root.setPadding(new Insets(24));
        root.getStyleClass().add("surface-panel");

        // Header Title
        HBox topTitleRow = new HBox(12);
        topTitleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(existingWorkflow == null ? "New workflow" : "Edit workflow");
        title.getStyleClass().add("page-title");
        topTitleRow.getChildren().add(title);

        VBox flowContent = new VBox(0);
        flowContent.setAlignment(Pos.TOP_CENTER);
        flowContent.setMaxWidth(760);

        // 1. Workflow Metadata Card
        VBox metaCard = createMetadataCard();

        // 2. Connector
        Node conn1 = createConnector();

        // 3. Trigger Step Card
        buildTriggerCard();

        // 4. Connector
        Node conn2 = createConnector();

        // 5. Conditions Step Card
        buildConditionsCard();

        // 6. Connector
        Node conn3 = createConnector();

        // 7. Actions Container
        buildActionsFlow();

        flowContent.getChildren().addAll(
                metaCard,
                conn1,
                triggerCard,
                conn2,
                conditionsCard,
                conn3,
                actionsFlowContainer
        );

        ScrollPane scroll = new ScrollPane(flowContent);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Sticky Footer
        HBox footer = createFooter();

        root.getChildren().addAll(topTitleRow, scroll, footer);
    }

    private VBox createMetadataCard() {
        VBox metaCard = new VBox(10);
        metaCard.getStyleClass().addAll("card", "flow-step-card");
        metaCard.setPadding(new Insets(16));

        HBox metaHeader = new HBox(8);
        metaHeader.setAlignment(Pos.CENTER_LEFT);
        Label metaTitle = new Label("Workflow Configuration");
        metaTitle.getStyleClass().add("section-title");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label enabledLabel = new Label("Enabled");
        enabledLabel.getStyleClass().add("body-text");
        metaHeader.getChildren().addAll(metaTitle, sp, enabledLabel, enabledSwitch);

        Label nameLabel = new Label("Name *");
        nameLabel.getStyleClass().add("secondary-text");

        nameField.getStyleClass().add("text-input");
        nameField.setPromptText("E.g. Sort downloaded PDF documents");
        nameErrorLabel.getStyleClass().add("field-error-label");
        nameErrorLabel.setVisible(false);
        nameErrorLabel.setManaged(false);

        metaCard.getChildren().addAll(metaHeader, nameLabel, nameField, nameErrorLabel);
        return metaCard;
    }

    private void buildTriggerCard() {
        triggerCard.getStyleClass().addAll("card", "flow-step-card", "flow-step-card-selected");
        triggerCard.setPadding(new Insets(16));

        HBox head = new HBox(12);
        head.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label("TRIGGER");
        badge.getStyleClass().addAll("step-badge", "step-badge-trigger");

        triggerSelector = new ComboBox<>();
        triggerSelector.getStyleClass().add("combo-box");
        configureComboBoxFriendlyNames(triggerSelector);
        for (PluginDescriptor<?> d : registry.findByCategory(PluginCategory.TRIGGER)) {
            triggerSelector.getItems().add(d.id().value());
        }
        triggerSelector.setOnAction(e -> onTriggerChanged());

        head.getChildren().addAll(badge, triggerSelector);

        triggerCard.getChildren().addAll(head, triggerFormContainer);
    }

    private void buildConditionsCard() {
        conditionsCard.getStyleClass().addAll("card", "flow-step-card");
        conditionsCard.setPadding(new Insets(16));

        HBox head = new HBox(12);
        head.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label("CONDITIONS");
        badge.getStyleClass().addAll("step-badge", "step-badge-condition");

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

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button addCondBtn = new Button("Add condition");
        addCondBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(addCondBtn);
        addCondBtn.setOnAction(e -> addConditionEntry(null, null));

        head.getChildren().addAll(badge, matchSegment, sp, addCondBtn);

        conditionsCard.getChildren().addAll(head, conditionsContainer);
    }

    private void buildActionsFlow() {
        Button addActBtn = new Button("Add action");
        addActBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(addActBtn);
        addActBtn.setOnAction(e -> addActionEntry("print-message", Map.of("text", "Processed file")));

        HBox addActRow = new HBox(addActBtn);
        addActRow.setAlignment(Pos.CENTER);
        addActRow.setPadding(new Insets(12, 0, 12, 0));

        actionsFlowContainer.getChildren().add(addActRow);
    }

    private Node createConnector() {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(2, 0, 2, 0));

        Rectangle line = new Rectangle(1.5, 20);
        line.getStyleClass().add("flow-connector-line");

        box.getChildren().add(line);
        return box;
    }

    private HBox createFooter() {
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 0, 0, 0));

        Button cancelBtn = new Button("Discard changes");
        cancelBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(cancelBtn);
        cancelBtn.setOnAction(e -> onComplete.run());

        Button testRunBtn = new Button("Test run");
        testRunBtn.getStyleClass().add("button-secondary");
        UiMotion.animateHover(testRunBtn);
        testRunBtn.setOnAction(e -> runTestExecution());

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("button-primary");
        UiMotion.animateHover(saveBtn);
        saveBtn.setOnAction(e -> saveWorkflow());

        footer.getChildren().addAll(testRunBtn, cancelBtn, saveBtn);
        return footer;
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

    private void addConditionEntry(String conditionId, Map<String, Object> initialParams) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "flow-step-card");
        card.setPadding(new Insets(12));

        HBox head = new HBox(10);
        head.setAlignment(Pos.CENTER_LEFT);

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

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button removeBtn = new Button("Delete");
        removeBtn.getStyleClass().add("button-danger");
        UiMotion.animateHover(removeBtn);

        head.getChildren().addAll(selector, sp, removeBtn);

        VBox formBox = new VBox(4);
        ConditionEntry entry = new ConditionEntry(selector, card, formBox);

        Runnable updateForm = () -> {
            formBox.getChildren().clear();
            String sel = selector.getValue();
            if (sel != null) {
                registry.findDescriptor(sel).ifPresent(desc -> {
                    entry.formBuilder = new DynamicFormBuilder(desc.parameters(), initialParams != null ? initialParams : Map.of());
                    formBox.getChildren().add(entry.formBuilder.getNode());
                });
            }
        };
        selector.setOnAction(e -> updateForm.run());
        updateForm.run();

        removeBtn.setOnAction(e -> {
            conditionEntries.remove(entry);
            conditionsContainer.getChildren().remove(card);
        });

        card.getChildren().addAll(head, formBox);
        conditionEntries.add(entry);
        conditionsContainer.getChildren().add(card);
        UiMotion.pageTransition(card);
    }

    public void addActionEntry(String actionId, Map<String, Object> initialParams) {
        int index = actionEntries.size();

        VBox wrapper = new VBox(0);
        wrapper.setAlignment(Pos.TOP_CENTER);

        Node conn = createConnector();

        VBox card = new VBox(8);
        card.getStyleClass().addAll("card", "flow-step-card");
        card.setPadding(new Insets(12));

        HBox head = new HBox(8);
        head.setAlignment(Pos.CENTER_LEFT);

        // Move Up & Move Down buttons for accessible reordering
        Button moveUpBtn = new Button("▲");
        moveUpBtn.getStyleClass().add("button-ghost");
        moveUpBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2px 6px;");

        Button moveDownBtn = new Button("▼");
        moveDownBtn.getStyleClass().add("button-ghost");
        moveDownBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2px 6px;");

        Label stepBadge = new Label("ACTION " + (index + 1));
        stepBadge.getStyleClass().addAll("step-badge", "step-badge-action");

        ComboBox<String> selector = new ComboBox<>();
        selector.getStyleClass().add("combo-box");
        configureComboBoxFriendlyNames(selector);
        for (PluginDescriptor<?> d : registry.findByCategory(PluginCategory.ACTION)) {
            selector.getItems().add(d.id().value());
        }
        String initial = actionId != null ? actionId : (!selector.getItems().isEmpty() ? selector.getItems().get(0) : "");
        selector.setValue(initial);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button removeBtn = new Button("Delete");
        removeBtn.getStyleClass().add("button-danger");
        UiMotion.animateHover(removeBtn);

        head.getChildren().addAll(moveUpBtn, moveDownBtn, stepBadge, selector, sp, removeBtn);

        VBox formBox = new VBox(4);
        ActionEntry entry = new ActionEntry(selector, card, formBox, stepBadge, wrapper);

        Runnable updateForm = () -> {
            formBox.getChildren().clear();
            String sel = selector.getValue();
            if (sel != null) {
                registry.findDescriptor(sel).ifPresent(desc -> {
                    entry.formBuilder = new DynamicFormBuilder(desc.parameters(), initialParams != null ? initialParams : Map.of());
                    formBox.getChildren().add(entry.formBuilder.getNode());
                });
            }
        };
        selector.setOnAction(e -> updateForm.run());
        updateForm.run();

        // Reordering logic
        moveUpBtn.setOnAction(e -> moveActionUp(entry));
        moveDownBtn.setOnAction(e -> moveActionDown(entry));

        // Drag and Drop reordering support
        card.setOnDragDetected(event -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(actionEntries.indexOf(entry)));
            db.setContent(content);
            event.consume();
        });

        card.setOnDragOver(event -> {
            if (event.getGestureSource() != card && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        card.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                try {
                    int fromIndex = Integer.parseInt(db.getString());
                    int toIndex = actionEntries.indexOf(entry);
                    if (fromIndex >= 0 && fromIndex < actionEntries.size() && fromIndex != toIndex) {
                        ActionEntry moved = actionEntries.remove(fromIndex);
                        actionEntries.add(toIndex, moved);
                        rebuildActionListUI();
                        success = true;
                    }
                } catch (NumberFormatException ignored) {}
            }
            event.setDropCompleted(success);
            event.consume();
        });

        removeBtn.setOnAction(e -> {
            actionEntries.remove(entry);
            rebuildActionListUI();
        });

        card.getChildren().addAll(head, formBox);
        wrapper.getChildren().addAll(conn, card);

        actionEntries.add(entry);
        rebuildActionListUI();
        UiMotion.pageTransition(card);
    }

    private void moveActionUp(ActionEntry entry) {
        int idx = actionEntries.indexOf(entry);
        if (idx > 0) {
            actionEntries.remove(idx);
            actionEntries.add(idx - 1, entry);
            rebuildActionListUI();
        }
    }

    private void moveActionDown(ActionEntry entry) {
        int idx = actionEntries.indexOf(entry);
        if (idx >= 0 && idx < actionEntries.size() - 1) {
            actionEntries.remove(idx);
            actionEntries.add(idx + 1, entry);
            rebuildActionListUI();
        }
    }

    private void rebuildActionListUI() {
        // Keep the "+ Add action" button at the end
        Node addBtnNode = actionsFlowContainer.getChildren().get(actionsFlowContainer.getChildren().size() - 1);
        actionsFlowContainer.getChildren().clear();

        for (int i = 0; i < actionEntries.size(); i++) {
            ActionEntry entry = actionEntries.get(i);
            entry.stepBadge.setText("ACTION " + (i + 1));
            actionsFlowContainer.getChildren().add(entry.wrapperNode);
        }

        actionsFlowContainer.getChildren().add(addBtnNode);
    }

    private void populateFields() {
        if (existingWorkflow != null) {
            nameField.setText(existingWorkflow.name());
            enabledSwitch.setSelected(existingWorkflow.enabled());

            String trigId = existingWorkflow.trigger().pluginId().value();
            triggerSelector.setValue(trigId);
            rebuildTriggerForm(trigId, existingWorkflow.trigger().params());

            existingWorkflow.conditionSpec().ifPresent(spec -> {
                if (ConditionSpec.TYPE_ALL_OF.equals(spec.type())) {
                    matchAll = true;
                    for (ConditionSpec child : spec.children()) {
                        if (!child.isComposite()) {
                            addConditionEntry(child.type(), child.params());
                        }
                    }
                } else if (ConditionSpec.TYPE_ANY_OF.equals(spec.type())) {
                    matchAll = false;
                    for (ConditionSpec child : spec.children()) {
                        if (!child.isComposite()) {
                            addConditionEntry(child.type(), child.params());
                        }
                    }
                } else if (!spec.isComposite()) {
                    addConditionEntry(spec.type(), spec.params());
                }
            });

            for (PluginSpec action : existingWorkflow.actions()) {
                addActionEntry(action.pluginId().value(), action.params());
            }
        } else {
            enabledSwitch.setSelected(true);
            if (!triggerSelector.getItems().isEmpty()) {
                triggerSelector.setValue(triggerSelector.getItems().get(0));
                onTriggerChanged();
            }
            addActionEntry("print-message", Map.of("text", "Processed file"));
        }
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

        for (ConditionEntry ce : conditionEntries) {
            if (ce.formBuilder != null && !ce.formBuilder.validate()) {
                valid = false;
            }
        }

        for (ActionEntry ae : actionEntries) {
            if (ae.formBuilder != null && !ae.formBuilder.validate()) {
                valid = false;
            }
        }

        return valid;
    }

    private Workflow buildWorkflowObject() {
        WorkflowId id = existingWorkflow != null ? existingWorkflow.id() : WorkflowId.random();
        String name = nameField.getText().trim();
        boolean enabled = enabledSwitch.isSelected();

        String trigId = triggerSelector.getValue();
        Map<String, Object> trigParams = triggerFormBuilder != null ? triggerFormBuilder.getValues() : Map.of();
        PluginSpec triggerSpec = PluginSpec.of(trigId, trigParams);

        ConditionSpec conditionSpec = null;
        if (!conditionEntries.isEmpty()) {
            List<ConditionSpec> leaves = new ArrayList<>();
            for (ConditionEntry ce : conditionEntries) {
                String cId = ce.selector.getValue();
                Map<String, Object> cParams = ce.formBuilder != null ? ce.formBuilder.getValues() : Map.of();
                leaves.add(ConditionSpec.leaf(cId, cParams));
            }
            if (leaves.size() == 1) {
                conditionSpec = leaves.get(0);
            } else {
                conditionSpec = matchAll ? ConditionSpec.allOf(leaves) : ConditionSpec.anyOf(leaves);
            }
        }

        List<PluginSpec> actionSpecs = new ArrayList<>();
        for (ActionEntry ae : actionEntries) {
            String aId = ae.selector.getValue();
            Map<String, Object> aParams = ae.formBuilder != null ? ae.formBuilder.getValues() : Map.of();
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

    public int getActionCount() {
        return actionEntries.size();
    }

    public void addAction(String pluginId, Map<String, Object> params) {
        addActionEntry(pluginId, params);
    }

    public void moveActionUp(int index) {
        if (index > 0 && index < actionEntries.size()) {
            moveActionUp(actionEntries.get(index));
        }
    }

    public void moveActionDown(int index) {
        if (index >= 0 && index < actionEntries.size() - 1) {
            moveActionDown(actionEntries.get(index));
        }
    }

    public void removeAction(int index) {
        if (index >= 0 && index < actionEntries.size()) {
            actionEntries.remove(index);
            rebuildActionListUI();
        }
    }

    public void setName(String name) {
        nameField.setText(name);
    }

    public String getName() {
        return nameField.getText();
    }

    public boolean validateEditor() {
        return validate();
    }

    public Workflow buildWorkflow() {
        return buildWorkflowObject();
    }

    public void triggerSave() {
        saveWorkflow();
    }

    public List<String> getActionPluginIds() {
        List<String> list = new ArrayList<>();
        for (ActionEntry ae : actionEntries) {
            list.add(ae.selector.getValue());
        }
        return list;
    }
}
