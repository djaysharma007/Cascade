package io.github.djaysharma007.cascade.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowInvariantTest {

    @Test
    void validWorkflowBuildsSuccessfully() {
        PluginSpec trigger = PluginSpec.of("timer", null);
        PluginSpec action = PluginSpec.of("print-message", null);

        Workflow workflow = Workflow.builder()
                .id(WorkflowId.random())
                .name("Test Workflow")
                .enabled(true)
                .trigger(trigger)
                .addAction(action)
                .build();

        assertThat(workflow.name()).isEqualTo("Test Workflow");
        assertThat(workflow.enabled()).isTrue();
        assertThat(workflow.actions()).hasSize(1);
    }

    @Test
    void rejectsBlankOrExcessiveName() {
        PluginSpec trigger = PluginSpec.of("timer", null);
        PluginSpec action = PluginSpec.of("print-message", null);

        assertThatThrownBy(() -> Workflow.builder()
                .name("   ")
                .trigger(trigger)
                .addAction(action)
                .build())
                .isInstanceOf(IllegalArgumentException.class);

        String longName = "A".repeat(81);
        assertThatThrownBy(() -> Workflow.builder()
                .name(longName)
                .trigger(trigger)
                .addAction(action)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingTriggerOrEmptyActions() {
        PluginSpec action = PluginSpec.of("print-message", null);

        assertThatThrownBy(() -> Workflow.builder()
                .name("Test")
                .addAction(action)
                .build())
                .isInstanceOf(NullPointerException.class);

        PluginSpec trigger = PluginSpec.of("timer", null);
        assertThatThrownBy(() -> Workflow.builder()
                .name("Test")
                .trigger(trigger)
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actionsListIsDefensivelyCopied() {
        PluginSpec trigger = PluginSpec.of("timer", null);
        PluginSpec action1 = PluginSpec.of("print-message", null);

        List<PluginSpec> list = new ArrayList<>();
        list.add(action1);

        Workflow workflow = Workflow.builder()
                .name("Test")
                .trigger(trigger)
                .actions(list)
                .build();

        // Mutating external list does not affect workflow
        PluginSpec action2 = PluginSpec.of("move-file", null);
        list.add(action2);
        assertThat(workflow.actions()).hasSize(1);

        // Mutating returned list throws UnsupportedOperationException
        assertThatThrownBy(() -> workflow.actions().add(action2))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
