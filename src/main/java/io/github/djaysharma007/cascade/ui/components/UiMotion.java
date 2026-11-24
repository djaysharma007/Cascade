package io.github.djaysharma007.cascade.ui.components;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.util.List;

/**
 * Utility for smooth, accessible UI transitions and animations across Cascade.
 * Standard duration: 120ms to 250ms with Interpolator.EASE_OUT.
 * Automatically respects the global Reduce Motion preference.
 */
public final class UiMotion {

    private static final BooleanProperty reduceMotion = new SimpleBooleanProperty(false);

    private UiMotion() {
    }

    public static BooleanProperty reduceMotionProperty() {
        return reduceMotion;
    }

    public static boolean isReduceMotion() {
        return reduceMotion.get();
    }

    public static void setReduceMotion(boolean reduce) {
        reduceMotion.set(reduce);
    }

    public static void pageTransition(Node node) {
        if (node == null) {
            return;
        }
        if (isReduceMotion()) {
            node.setOpacity(1.0);
            node.setTranslateY(0);
            return;
        }

        node.setOpacity(0.0);
        node.setTranslateY(10.0);

        FadeTransition fade = new FadeTransition(Duration.millis(180), node);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(180), node);
        slide.setToY(0.0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition transition = new ParallelTransition(fade, slide);
        transition.play();
    }

    public static void animateHover(ButtonBase button) {
        if (button == null) {
            return;
        }
        button.setOnMouseEntered(e -> {
            if (isReduceMotion() || button.isDisabled()) {
                return;
            }
            ScaleTransition st = new ScaleTransition(Duration.millis(120), button);
            st.setToX(1.02);
            st.setToY(1.02);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });

        button.setOnMouseExited(e -> {
            if (isReduceMotion()) {
                button.setScaleX(1.0);
                button.setScaleY(1.0);
                return;
            }
            ScaleTransition st = new ScaleTransition(Duration.millis(120), button);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });

        button.setOnMousePressed(e -> {
            if (isReduceMotion() || button.isDisabled()) {
                return;
            }
            ScaleTransition st = new ScaleTransition(Duration.millis(80), button);
            st.setToX(0.98);
            st.setToY(0.98);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });

        button.setOnMouseReleased(e -> {
            if (isReduceMotion()) {
                button.setScaleX(1.0);
                button.setScaleY(1.0);
                return;
            }
            ScaleTransition st = new ScaleTransition(Duration.millis(120), button);
            st.setToX(button.isHover() ? 1.02 : 1.0);
            st.setToY(button.isHover() ? 1.02 : 1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });
    }

    public static void staggerFadeIn(List<? extends Node> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        if (isReduceMotion()) {
            for (Node n : nodes) {
                n.setOpacity(1.0);
                n.setTranslateY(0);
            }
            return;
        }

        int delayMs = 0;
        for (Node n : nodes) {
            n.setOpacity(0.0);
            n.setTranslateY(8.0);

            FadeTransition fade = new FadeTransition(Duration.millis(150), n);
            fade.setToValue(1.0);
            fade.setInterpolator(Interpolator.EASE_OUT);
            fade.setDelay(Duration.millis(delayMs));

            TranslateTransition slide = new TranslateTransition(Duration.millis(150), n);
            slide.setToY(0.0);
            slide.setInterpolator(Interpolator.EASE_OUT);
            slide.setDelay(Duration.millis(delayMs));

            new ParallelTransition(fade, slide).play();
            delayMs += 35;
        }
    }

    public static void animateNumber(Label label, int start, int target, String suffix) {
        if (label == null) {
            return;
        }
        if (isReduceMotion() || start == target) {
            label.setText(target + (suffix != null ? suffix : ""));
            return;
        }

        int steps = 10;
        Timeline timeline = new Timeline();
        double durationPerStep = 250.0 / steps;

        for (int i = 0; i <= steps; i++) {
            double fraction = (double) i / steps;
            int current = (int) Math.round(start + (target - start) * fraction);
            String text = current + (suffix != null ? suffix : "");
            timeline.getKeyFrames().add(new KeyFrame(Duration.millis(i * durationPerStep), e -> label.setText(text)));
        }
        timeline.play();
    }

    public static Timeline pulse(Node node) {
        if (node == null) {
            return null;
        }
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(node.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(800), new KeyValue(node.opacityProperty(), 0.45, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(1600), new KeyValue(node.opacityProperty(), 1.0, Interpolator.EASE_BOTH))
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        if (!isReduceMotion()) {
            timeline.play();
        }
        return timeline;
    }

    public static void slideInToast(Node node, Runnable onFinished) {
        if (node == null) {
            return;
        }
        if (isReduceMotion()) {
            node.setOpacity(1.0);
            node.setTranslateY(0);
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        node.setOpacity(0.0);
        node.setTranslateY(24.0);

        FadeTransition fade = new FadeTransition(Duration.millis(200), node);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(200), node);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(fade, slide);
        if (onFinished != null) {
            pt.setOnFinished(e -> onFinished.run());
        }
        pt.play();
    }

    public static void slideOutToast(Node node, Runnable onFinished) {
        if (node == null) {
            return;
        }
        if (isReduceMotion()) {
            node.setOpacity(0.0);
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        FadeTransition fade = new FadeTransition(Duration.millis(180), node);
        fade.setToValue(0.0);
        fade.setInterpolator(Interpolator.EASE_IN);

        TranslateTransition slide = new TranslateTransition(Duration.millis(180), node);
        slide.setToY(24.0);
        slide.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition pt = new ParallelTransition(fade, slide);
        if (onFinished != null) {
            pt.setOnFinished(e -> onFinished.run());
        }
        pt.play();
    }
}
