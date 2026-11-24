package io.github.djaysharma007.cascade.ui.components;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.SkinBase;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Calm, accessible toggle switch control with full track thumb travel.
 * Track: 40x22 px (sand when off, petrol when on).
 * Thumb: 18 px diameter (paper fill).
 * Travel distance: 18 px (from centerX=11.0 to centerX=29.0 with 2.0 px symmetric padding).
 */
public final class ToggleSwitch extends Control {

    public static final double TRACK_WIDTH = 40.0;
    public static final double TRACK_HEIGHT = 22.0;
    public static final double THUMB_RADIUS = 9.0;
    public static final double PADDING = (TRACK_HEIGHT - (2.0 * THUMB_RADIUS)) / 2.0; // 2.0 px
    public static final double THUMB_OFF_CENTER_X = PADDING + THUMB_RADIUS;           // 11.0 px
    public static final double THUMB_ON_CENTER_X = TRACK_WIDTH - PADDING - THUMB_RADIUS; // 29.0 px
    public static final double TRAVEL_DISTANCE = THUMB_ON_CENTER_X - THUMB_OFF_CENTER_X; // 18.0 px

    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private ToggleSwitchSkin skinInstance;

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
        this.skinInstance = new ToggleSwitchSkin(this);
        return skinInstance;
    }

    public double getThumbCenterX() {
        if (skinInstance != null) {
            return skinInstance.getThumbCenterX();
        }
        return isSelected() ? THUMB_ON_CENTER_X : THUMB_OFF_CENTER_X;
    }

    public static final class ToggleSwitchSkin extends SkinBase<ToggleSwitch> {

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
            thumb.setCenterX(THUMB_OFF_CENTER_X);
            thumb.setCenterY(TRACK_HEIGHT / 2.0);

            container = new Pane(track, thumb);
            container.setPrefSize(TRACK_WIDTH, TRACK_HEIGHT);
            container.setMaxSize(TRACK_WIDTH, TRACK_HEIGHT);
            container.setMinSize(TRACK_WIDTH, TRACK_HEIGHT);

            getChildren().add(container);

            // Initialize position immediately without animation
            updateVisualState(control.isSelected(), false);

            control.selectedProperty().addListener((obs, oldV, isSel) -> {
                updateVisualState(isSel, !UiMotion.isReduceMotion());
            });

            control.focusedProperty().addListener((obs, oldV, isFocused) -> {
                if (isFocused) {
                    track.setStyle("-fx-stroke: -color-copper; -fx-stroke-width: 2px;");
                } else {
                    track.setStyle("");
                }
            });
        }

        private void updateVisualState(boolean active, boolean animate) {
            double targetTranslateX = active ? TRAVEL_DISTANCE : 0.0;

            if (active) {
                if (!track.getStyleClass().contains("toggle-switch-track-active")) {
                    track.getStyleClass().add("toggle-switch-track-active");
                }
            } else {
                track.getStyleClass().remove("toggle-switch-track-active");
            }

            if (animate) {
                TranslateTransition tt = new TranslateTransition(Duration.millis(180), thumb);
                tt.setToX(targetTranslateX);
                tt.setInterpolator(Interpolator.EASE_OUT);
                tt.setOnFinished(e -> thumb.setTranslateX(targetTranslateX));
                tt.play();
            } else {
                thumb.setTranslateX(targetTranslateX);
            }
        }

        double getThumbCenterX() {
            return thumb.getCenterX() + thumb.getTranslateX();
        }
    }
}
