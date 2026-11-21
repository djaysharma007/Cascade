package io.github.djaysharma007.cascade.core;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventAndConditionInvariantTest {

    @Test
    void eventPayloadIsDefensivelyCopiedAndUnmodifiable() {
        Map<String, Object> map = new HashMap<>();
        map.put("fileName", "doc.pdf");

        Event event = Event.of(map);
        assertThat(event.payload()).containsEntry("fileName", "doc.pdf");

        map.put("fileSize", 1024);
        assertThat(event.payload()).doesNotContainKey("fileSize");

        assertThatThrownBy(() -> event.payload().put("other", 1))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void conditionSpecInvariants() {
        ConditionSpec leaf = ConditionSpec.leaf("extension-is", Map.of("value", ".pdf"));
        assertThat(leaf.isComposite()).isFalse();

        // all-of must have at least 1 child
        assertThatThrownBy(() -> ConditionSpec.allOf(List.of()))
                .isInstanceOf(IllegalArgumentException.class);

        ConditionSpec allOf = ConditionSpec.allOf(List.of(leaf));
        assertThat(allOf.isComposite()).isTrue();
        assertThat(allOf.children()).hasSize(1);

        // not must have exactly 1 child
        assertThatThrownBy(() -> new ConditionSpec(ConditionSpec.TYPE_NOT, Map.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConditionSpec(ConditionSpec.TYPE_NOT, Map.of(), List.of(leaf, leaf)))
                .isInstanceOf(IllegalArgumentException.class);

        ConditionSpec not = ConditionSpec.not(leaf);
        assertThat(not.isComposite()).isTrue();
        assertThat(not.children()).hasSize(1);
    }
}
