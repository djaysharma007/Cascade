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
}
