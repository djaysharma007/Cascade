package io.github.djaysharma007.cascade.ui.components;

import io.github.djaysharma007.cascade.core.ParamSpec;
import io.github.djaysharma007.cascade.core.ParamType;
import io.github.djaysharma007.cascade.registry.TemplateResolver;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Generates form controls dynamically from plugin ParamSpecs with inline validation.
 */
public final class DynamicFormBuilder {

    private final List<ParamSpec> specs;
    private final Map<String, Object> initialValues;
    private final Map<String, FormField> fieldMap = new LinkedHashMap<>();
    private final VBox container = new VBox(12);

    private record FormField(
            ParamSpec spec,
            Node inputControl,
            Label errorLabel,
            java.util.function.Supplier<Object> valueGetter
    ) {}

    public DynamicFormBuilder(List<ParamSpec> specs, Map<String, Object> initialValues) {
        this.specs = specs != null ? specs : List.of();
        this.initialValues = initialValues != null ? initialValues : Map.of();
        buildForm();
    }

    private void buildForm() {
        container.getChildren().clear();
        fieldMap.clear();

        for (ParamSpec spec : specs) {
            VBox fieldBox = new VBox(4);

            Label nameLabel = new Label(spec.name() + (spec.required() ? " *" : ""));
            nameLabel.getStyleClass().add("secondary-text");

            Label errorLabel = new Label();
            errorLabel.getStyleClass().add("field-error-label");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);

            Object initVal = initialValues.getOrDefault(spec.name(), spec.defaultValue());
            FormField formField = createControl(spec, initVal, errorLabel);

            fieldMap.put(spec.name(), formField);
            fieldBox.getChildren().addAll(nameLabel, formField.inputControl(), errorLabel);
            container.getChildren().add(fieldBox);
        }
    }

    private FormField createControl(ParamSpec spec, Object initVal, Label errorLabel) {
        ParamType type = spec.type();

        if (type == ParamType.BOOLEAN) {
            CheckBox cb = new CheckBox();
            cb.setSelected(Boolean.TRUE.equals(initVal));
            return new FormField(spec, cb, errorLabel, cb::isSelected);
        }

        if (type == ParamType.CHOICE) {
            ComboBox<String> combo = new ComboBox<>();
            combo.getStyleClass().add("combo-box");
            combo.getItems().addAll(spec.options());
            if (initVal != null) {
                combo.setValue(initVal.toString());
            } else if (!spec.options().isEmpty()) {
                combo.setValue(spec.options().get(0));
            }
            return new FormField(spec, combo, errorLabel, combo::getValue);
        }

        if (type == ParamType.FOLDER) {
            HBox folderBox = new HBox(8);
            folderBox.setAlignment(Pos.CENTER_LEFT);

            TextField tf = new TextField(initVal != null ? initVal.toString() : "");
            tf.getStyleClass().add("text-input");
            HBox.setHgrow(tf, Priority.ALWAYS);

            Button browseBtn = new Button("Browse");
            browseBtn.getStyleClass().add("button-secondary");
            browseBtn.setOnAction(e -> {
                DirectoryChooser dc = new DirectoryChooser();
                dc.setTitle("Select folder");
                if (!tf.getText().isBlank()) {
                    File f = new File(tf.getText());
                    if (f.exists() && f.isDirectory()) {
                        dc.setInitialDirectory(f);
                    }
                }
                File selected = dc.showDialog(folderBox.getScene().getWindow());
                if (selected != null) {
                    tf.setText(selected.getAbsolutePath());
                    errorLabel.setVisible(false);
                    errorLabel.setManaged(false);
                }
            });

            folderBox.getChildren().addAll(tf, browseBtn);
            return new FormField(spec, folderBox, errorLabel, tf::getText);
        }

        // TEXT, TEMPLATE, NUMBER
        TextField tf = new TextField(initVal != null ? initVal.toString() : "");
        tf.getStyleClass().add("text-input");
        tf.textProperty().addListener((obs, oldV, newV) -> {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        });

        java.util.function.Supplier<Object> getter;
        if (type == ParamType.NUMBER) {
            getter = () -> {
                String text = tf.getText().trim();
                if (text.isEmpty()) return null;
                try {
                    return text.contains(".") ? Double.parseDouble(text) : Long.parseLong(text);
                } catch (NumberFormatException e) {
                    return null;
                }
            };
        } else {
            getter = tf::getText;
        }

        return new FormField(spec, tf, errorLabel, getter);
    }

    public boolean validate() {
        return validate(Set.of());
    }

    public boolean validate(Set<String> availablePayloadKeys) {
        boolean valid = true;
        for (FormField field : fieldMap.values()) {
            ParamSpec spec = field.spec();
            Object val = field.valueGetter().get();

            if (spec.required() && (val == null || (val instanceof String s && s.isBlank()))) {
                showError(field, "This field is required.");
                valid = false;
                continue;
            }

            if (spec.type() == ParamType.NUMBER && val == null && spec.required()) {
                showError(field, "Must be a valid number.");
                valid = false;
                continue;
            }

            if (spec.type() == ParamType.TEMPLATE && val instanceof String s) {
                Set<String> used = TemplateResolver.extractPlaceholders(s);
                Set<String> known = TemplateResolver.KNOWN_PLACEHOLDERS;
                for (String placeholder : used) {
                    if (!known.contains(placeholder)) {
                        showError(field, "Unknown placeholder: {" + placeholder + "}");
                        valid = false;
                        break;
                    }
                }
            }
        }
        return valid;
    }

    private void showError(FormField field, String message) {
        field.errorLabel().setText(message);
        field.errorLabel().setVisible(true);
        field.errorLabel().setManaged(true);
    }

    public Map<String, Object> getValues() {
        Map<String, Object> values = new HashMap<>();
        for (Map.Entry<String, FormField> entry : fieldMap.entrySet()) {
            Object val = entry.getValue().valueGetter().get();
            if (val != null) {
                values.put(entry.getKey(), val);
            }
        }
        return values;
    }

    public Node getNode() {
        return container;
    }
}
