package io.github.djaysharma007.cascade.ui.components;

import javafx.application.Platform;
import javafx.scene.shape.SVGPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UiComponentsTest {

    private static boolean platformStarted = false;

    @BeforeAll
    static synchronized void initPlatform() {
        if (!platformStarted) {
            try {
                Platform.startup(() -> {});
                platformStarted = true;
            } catch (IllegalStateException ignored) {
                platformStarted = true;
            }
        }
    }

    @Test
    void testPluginDisplayNameResolver() {
        assertThat(PluginDisplayNameResolver.resolve("file-created")).isEqualTo("File created");
        assertThat(PluginDisplayNameResolver.resolve("move-file")).isEqualTo("Move file");
        assertThat(PluginDisplayNameResolver.resolve("schedule")).isEqualTo("Scheduled time");
        assertThat(PluginDisplayNameResolver.resolve("custom-plugin-id")).isEqualTo("Custom Plugin Id");
        assertThat(PluginDisplayNameResolver.resolve(null)).isEqualTo("Unknown");
        assertThat(PluginDisplayNameResolver.resolve("")).isEqualTo("Unknown");
    }

    @Test
    void testCascadeIconsCreation() {
        SVGPath dashIcon = CascadeIcons.icon("dashboard");
        assertThat(dashIcon).isNotNull();
        assertThat(dashIcon.getContent()).isNotEmpty();

        SVGPath workflowsIcon = CascadeIcons.icon("workflows");
        assertThat(workflowsIcon).isNotNull();
        assertThat(workflowsIcon.getContent()).isNotEmpty();

        SVGPath unknownIcon = CascadeIcons.icon("non-existent");
        assertThat(unknownIcon).isNotNull();
        assertThat(unknownIcon.getContent()).isNotEmpty();
    }

    @Test
    void testToggleSwitchState() {
        ToggleSwitch toggle = new ToggleSwitch(false);
        assertThat(toggle.isSelected()).isFalse();

        toggle.toggle();
        assertThat(toggle.isSelected()).isTrue();

        toggle.setSelected(false);
        assertThat(toggle.isSelected()).isFalse();
    }

    @Test
    void testToggleSwitchThumbEndPositionsInBothStates() {
        boolean originalMotion = UiMotion.isReduceMotion();
        try {
            UiMotion.setReduceMotion(true);

            // Off state on initial render
            ToggleSwitch toggleOff = new ToggleSwitch(false);
            new javafx.scene.Scene(toggleOff);
            assertThat(toggleOff.getThumbCenterX())
                    .as("Thumb center X in OFF state")
                    .isEqualTo(ToggleSwitch.THUMB_OFF_CENTER_X);
            assertThat(ToggleSwitch.THUMB_OFF_CENTER_X).isEqualTo(11.0);

            // On state on initial render
            ToggleSwitch toggleOn = new ToggleSwitch(true);
            new javafx.scene.Scene(toggleOn);
            assertThat(toggleOn.getThumbCenterX())
                    .as("Thumb center X in ON state")
                    .isEqualTo(ToggleSwitch.THUMB_ON_CENTER_X);
            assertThat(ToggleSwitch.THUMB_ON_CENTER_X).isEqualTo(29.0);

            // Transition from off to on
            toggleOff.setSelected(true);
            assertThat(toggleOff.getThumbCenterX()).isEqualTo(ToggleSwitch.THUMB_ON_CENTER_X);

            // Transition from on to off
            toggleOff.setSelected(false);
            assertThat(toggleOff.getThumbCenterX()).isEqualTo(ToggleSwitch.THUMB_OFF_CENTER_X);
        } finally {
            UiMotion.setReduceMotion(originalMotion);
        }
    }

    @Test
    void testUiMotionReduceMotion() {
        boolean original = UiMotion.isReduceMotion();
        try {
            UiMotion.setReduceMotion(true);
            assertThat(UiMotion.isReduceMotion()).isTrue();

            UiMotion.setReduceMotion(false);
            assertThat(UiMotion.isReduceMotion()).isFalse();
        } finally {
            UiMotion.setReduceMotion(original);
        }
    }

    @Test
    void testIbmPlexFontsLoaded() throws Exception {
        String[] fontPaths = {
                "/fonts/IBMPlexSans-Regular.ttf",
                "/fonts/IBMPlexSans-SemiBold.ttf",
                "/fonts/IBMPlexSerif-Regular.ttf",
                "/fonts/IBMPlexSerif-SemiBold.ttf",
                "/fonts/IBMPlexMono-Regular.ttf"
        };
        for (String path : fontPaths) {
            try (var is = getClass().getResourceAsStream(path)) {
                assertThat(is).as("Resource " + path).isNotNull();
                javafx.scene.text.Font font = javafx.scene.text.Font.loadFont(is, 14);
                assertThat(font).as("Font for " + path).isNotNull();
                assertThat(font.getFamily()).startsWith("IBM Plex");
            }
        }
    }
}
