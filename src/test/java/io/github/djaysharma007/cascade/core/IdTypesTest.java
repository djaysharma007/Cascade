package io.github.djaysharma007.cascade.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdTypesTest {

    @Test
    void workflowIdGuarantees() {
        UUID u1 = UUID.randomUUID();
        WorkflowId id1 = new WorkflowId(u1);
        WorkflowId id2 = new WorkflowId(u1);
        WorkflowId id3 = WorkflowId.random();

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        assertThat(id1).isNotEqualTo(id3);

        Map<WorkflowId, String> map = new HashMap<>();
        map.put(id1, "val");
        assertThat(map.get(id2)).isEqualTo("val");

        assertThatThrownBy(() -> new WorkflowId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void runIdGuarantees() {
        UUID u1 = UUID.randomUUID();
        RunId id1 = new RunId(u1);
        RunId id2 = new RunId(u1);

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());

        assertThatThrownBy(() -> new RunId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void eventIdGuarantees() {
        UUID u1 = UUID.randomUUID();
        EventId id1 = new EventId(u1);
        EventId id2 = new EventId(u1);

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());

        assertThatThrownBy(() -> new EventId(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void pluginIdValidationAndEquality() {
        PluginId p1 = new PluginId("file-created");
        PluginId p2 = PluginId.of("file-created");
        PluginId p3 = new PluginId("timer");

        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
        assertThat(p1).isNotEqualTo(p3);

        Map<PluginId, String> map = new HashMap<>();
        map.put(p1, "file");
        assertThat(map.get(p2)).isEqualTo("file");

        // Invalids
        assertThatThrownBy(() -> new PluginId(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PluginId("1abc"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PluginId("ab"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PluginId("Invalid_Uppercase"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
