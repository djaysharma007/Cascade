package io.github.djaysharma007.cascade.ui.components;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Objects;

/**
 * Toast notification overlay anchored to the bottom-right of the window.
 * Slides in, displays with icon and action confirmation, and auto-dismisses.
 */
public final class ToastOverlay extends VBox {

    public enum Type {
        INFO, SUCCESS, ERROR
    }

    public ToastOverlay() {
        setPickOnBounds(false);
        setAlignment(Pos.BOTTOM_RIGHT);
        setPadding(new Insets(24));
        setSpacing(10);
        setMaxWidth(420);
    }

    public void showToast(String message, Type type) {
        Objects.requireNonNull(message, "message must not be null");

        HBox toast = new HBox(12);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.getStyleClass().addAll("toast-container", switch (type) {
            case SUCCESS -> "toast-success";
            case ERROR -> "toast-error";
            default -> "toast-info";
        });

        String iconName = switch (type) {
            case SUCCESS -> "check";
            case ERROR -> "close";
            default -> "bell";
        };
        var icon = CascadeIcons.icon(iconName, 0.9, switch (type) {
            case SUCCESS -> "icon-success";
            case ERROR -> "icon-error";
            default -> "icon-functional";
        });

        Label label = new Label(message);
        label.getStyleClass().add("body-text");
        label.setWrapText(true);
        HBox.setHgrow(label, Priority.ALWAYS);

        toast.getChildren().addAll(icon, label);

        getChildren().add(toast);
        UiMotion.slideInToast(toast, null);

        PauseTransition displayPause = new PauseTransition(Duration.millis(3500));
        displayPause.setOnFinished(e -> {
            UiMotion.slideOutToast(toast, () -> getChildren().remove(toast));
        });
        displayPause.play();
    }
}
