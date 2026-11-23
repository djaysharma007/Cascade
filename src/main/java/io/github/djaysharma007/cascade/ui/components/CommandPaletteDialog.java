package io.github.djaysharma007.cascade.ui.components;

import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.Workflow;
import io.github.djaysharma007.cascade.engine.EngineFacade;
import io.github.djaysharma007.cascade.ui.views.WorkflowTemplate;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Quick command launcher and search palette invoked via Ctrl+K.
 */
public final class CommandPaletteDialog extends StackPane {

    public record CommandItem(
            String category,
            String title,
            String hint,
            String iconName,
            Runnable action
    ) {}

    private final TextField searchField = new TextField();
    private final ListView<CommandItem> listView = new ListView<>();
    private final List<CommandItem> allItems = new ArrayList<>();
    private final Runnable onClose;

    public CommandPaletteDialog(Runnable onClose) {
        this.onClose = Objects.requireNonNull(onClose);
        buildUI();
    }

    private void buildUI() {
        setVisible(false);
        setManaged(false);
        getStyleClass().add("command-palette-overlay");
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(80, 24, 24, 24));

        // Click outside to dismiss
        setOnMouseClicked(e -> {
            if (e.getTarget() == this) {
                close();
            }
        });

        VBox card = new VBox(0);
        card.getStyleClass().add("command-palette-card");
        card.setMaxWidth(620);
        card.setPrefWidth(580);

        // Search input
        HBox inputRow = new HBox(12);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        inputRow.setPadding(new Insets(12, 16, 12, 16));
        inputRow.getStyleClass().add("command-palette-input-row");

        var searchIcon = CascadeIcons.icon("search", 0.9, "icon-secondary");
        searchField.setPromptText("Type a command or search workflows... (Esc to close)");
        searchField.getStyleClass().add("command-palette-input");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchField.textProperty().addListener((obs, oldV, newV) -> filterList(newV));
        searchField.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);

        inputRow.getChildren().addAll(searchIcon, searchField);

        // List view
        listView.getStyleClass().add("command-palette-list");
        listView.setPrefHeight(320);
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CommandItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox row = new HBox(12);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(6, 10, 6, 10));

                    var icon = CascadeIcons.icon(item.iconName(), 0.85, "icon-teal");

                    VBox titleBox = new VBox(2);
                    Label titleLabel = new Label(item.title());
                    titleLabel.getStyleClass().add("body-text");

                    Label catBadge = new Label(item.category());
                    catBadge.getStyleClass().add("template-category-chip");

                    titleBox.getChildren().addAll(titleLabel);
                    HBox.setHgrow(titleBox, Priority.ALWAYS);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label hintLabel = new Label(item.hint() != null ? item.hint() : "");
                    hintLabel.getStyleClass().add("caption-text");

                    row.getChildren().addAll(icon, titleBox, catBadge, spacer, hintLabel);
                    setGraphic(row);
                }
            }
        });

        listView.setOnMouseClicked(e -> {
            CommandItem selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                executeItem(selected);
            }
        });

        // Footer hint
        HBox footer = new HBox(16);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(8, 16, 8, 16));
        footer.getStyleClass().add("command-palette-footer");

        Label hintText = new Label("↑ ↓ to navigate   •   ↵ to select   •   Esc to close");
        hintText.getStyleClass().add("caption-text");
        footer.getChildren().add(hintText);

        card.getChildren().addAll(inputRow, listView, footer);
        getChildren().add(card);
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.DOWN) {
            int current = listView.getSelectionModel().getSelectedIndex();
            if (current < listView.getItems().size() - 1) {
                listView.getSelectionModel().select(current + 1);
                listView.scrollTo(current + 1);
            }
            event.consume();
        } else if (event.getCode() == KeyCode.UP) {
            int current = listView.getSelectionModel().getSelectedIndex();
            if (current > 0) {
                listView.getSelectionModel().select(current - 1);
                listView.scrollTo(current - 1);
            }
            event.consume();
        } else if (event.getCode() == KeyCode.ENTER) {
            CommandItem selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                executeItem(selected);
            }
            event.consume();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            close();
            event.consume();
        }
    }

    private void executeItem(CommandItem item) {
        close();
        if (item.action() != null) {
            item.action().run();
        }
    }

    public void open(
            EngineFacade facade,
            Runnable navDashboard,
            Runnable navWorkflows,
            Runnable navTemplates,
            Runnable navActivity,
            Runnable navSettings,
            Runnable newWorkflow,
            Consumer<Workflow> onEditWorkflow,
            Consumer<Workflow> onUseTemplate,
            Supplier<List<WorkflowTemplate>> templatesSupplier
    ) {
        allItems.clear();

        // 1. Navigation items
        allItems.add(new CommandItem("Navigation", "Go to Dashboard", "Alt+1", "dashboard", navDashboard));
        allItems.add(new CommandItem("Navigation", "Go to Workflows", "Alt+2", "workflows", navWorkflows));
        allItems.add(new CommandItem("Navigation", "Go to Templates", "Alt+3", "templates", navTemplates));
        allItems.add(new CommandItem("Navigation", "Go to Activity", "Alt+4", "activity", navActivity));
        allItems.add(new CommandItem("Navigation", "Go to Settings", "Alt+5", "settings", navSettings));

        // 2. Action items
        allItems.add(new CommandItem("Actions", "New workflow", "Ctrl+N", "plus", newWorkflow));
        if (facade.isRunning()) {
            allItems.add(new CommandItem("Actions", "Stop engine", "", "stop", facade::stop));
        } else {
            allItems.add(new CommandItem("Actions", "Start engine", "", "play", facade::start));
        }

        // 3. Workflows
        for (Workflow wf : facade.allWorkflows()) {
            allItems.add(new CommandItem(
                    "Workflows",
                    "Run: " + wf.name(),
                    "Manual run",
                    "play",
                    () -> facade.executeWorkflowManually(wf.id(), Event.empty())
            ));
            allItems.add(new CommandItem(
                    "Workflows",
                    "Edit: " + wf.name(),
                    "Open editor",
                    "editor",
                    () -> onEditWorkflow.accept(wf)
            ));
        }

        // 4. Templates
        if (templatesSupplier != null) {
            for (WorkflowTemplate tpl : templatesSupplier.get()) {
                allItems.add(new CommandItem(
                        "Templates",
                        "Template: " + tpl.title(),
                        tpl.category(),
                        tpl.iconName(),
                        () -> onUseTemplate.accept(tpl.instantiate())
                ));
            }
        }

        searchField.setText("");
        filterList("");

        setVisible(true);
        setManaged(true);
        Platform.runLater(searchField::requestFocus);
        UiMotion.pageTransition(this);
    }

    public void close() {
        setVisible(false);
        setManaged(false);
        onClose.run();
    }

    private void filterList(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        List<CommandItem> filtered = allItems.stream()
                .filter(item -> q.isEmpty()
                        || item.title().toLowerCase().contains(q)
                        || item.category().toLowerCase().contains(q))
                .toList();

        listView.getItems().setAll(filtered);
        if (!filtered.isEmpty()) {
            listView.getSelectionModel().select(0);
        }
    }

    public boolean isOpen() {
        return isVisible();
    }

    public int getItemCount() {
        return listView.getItems().size();
    }

    public List<CommandItem> getItems() {
        return List.copyOf(listView.getItems());
    }

    public CommandItem getSelectedItem() {
        return listView.getSelectionModel().getSelectedItem();
    }

    public void setSearchText(String text) {
        searchField.setText(text);
        filterList(text);
    }

    public void executeSelected() {
        CommandItem selected = listView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            executeItem(selected);
        }
    }
}
