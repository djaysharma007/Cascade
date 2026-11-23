package io.github.djaysharma007.cascade.ui.components;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Modern toggle switch control with smooth sliding animation and full keyboard accessibility.
 */
public final class ToggleSwitch extends Control {

    private static final double TRACK_WIDTH = 42.0;
    private static final double TRACK_HEIGHT = 22.0;
    private static final double THUMB_RADIUS = 8.0;
    private static final double THUMB_LEFT = 3.0 + THUMB_RADIUS;
    private static final double THUMB_RIGHT = TRACK_WIDTH - 3.0 - THUMB_RADIUS;

    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public ToggleSwitch() {
        this(false);
    }

    public ToggleSwitch(boolean initialSelected) {
        getStyleClass().add("toggle-switch");
        setFocusTraversable(true);
        setSelected(initialSelected);

        setOnMouseClicked(e -> toggle());
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) {
                toggle();
                e.consume();
            }
        });
    }

    public void toggle() {
        if (!isDisabled()) {
            setSelected(!isSelected());
        }
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean val) {
        selected.set(val);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new ToggleSwitchSkin(this);
    }

    private static final class ToggleSwitchSkin extends SkinBase<ToggleSwitch> {

        private final Rectangle track;
        private final Circle thumb;
        private final Pane container;

        ToggleSwitchSkin(ToggleSwitch control) {
            super(control);

            track = new Rectangle(TRACK_WIDTH, TRACK_HEIGHT);
            track.setArcWidth(TRACK_HEIGHT);
            track.setArcHeight(TRACK_HEIGHT);
            track.getStyleClass().add("toggle-switch-track");

            thumb = new Circle(THUMB_RADIUS);
            thumb.getStyleClass().add("toggle-switch-thumb");
            thumb.setCenterY(TRACK_HEIGHT / 2.0);

            container = new Pane(track, thumb);
            container.setPrefSize(TRACK_WIDTH, TRACK_HEIGHT);
            container.setMaxSize(TRACK_WIDTH, TRACK_HEIGHT);
            container.setMinSize(TRACK_WIDTH, TRACK_HEIGHT);

            getChildren().add(container);

            updateVisualState(control.isSelected(), false);

            control.selectedProperty().addListener((obs, oldV, isSel) -> {
                updateVisualState(isSel, !UiMotion.isReduceMotion());
            });

            control.focusedProperty().addListener((obs, oldV, isFocused) -> {
                if (isFocused) {
                    track.setStyle("-fx-stroke: -color-accent-bright; -fx-stroke-width: 2px;");
                } else {
                    track.setStyle("");
                }
            });
        }

        private void updateVisualState(boolean active, boolean animate) {
            double targetX = active ? THUMB_RIGHT : THUMB_LEFT;
            if (active) {
                if (!track.getStyleClass().contains("toggle-switch-track-active")) {
                    track.getStyleClass().add("toggle-switch-track-active");
                }
            } else {
                track.getStyleClass().remove("toggle-switch-track-active");
            }

            if (animate) {
                TranslateTransition tt = new TranslateTransition(Duration.millis(150), thumb);
                tt.setToX(targetX - THUMB_LEFT);
                tt.setInterpolator(Interpolator.EASE_OUT);
                tt.play();
            } else {
                thumb.setTranslateX(targetX - THUMB_LEFT);
            }
        }
    }
}
