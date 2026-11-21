package io.github.djaysharma007.cascade.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Shared contract test suite for Condition implementations.
 */
public abstract class ConditionContractTest {

    protected abstract Condition createCondition();

    @Test
    void testRequiresNonNullEvent() {
        Condition condition = createCondition();
        assertThatThrownBy(() -> condition.test(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testIsDeterministic() {
        Condition condition = createCondition();
        Event event = Event.empty();
        boolean first = condition.test(event);
        boolean second = condition.test(event);
        assertThat(first).isEqualTo(second);
    }
}
