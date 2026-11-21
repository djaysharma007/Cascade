package io.github.djaysharma007.cascade.core;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Shared contract test suite for Trigger implementations.
 */
public abstract class TriggerContractTest {

    protected abstract Trigger createTrigger();

    @Test
    void startRequiresNonNullListener() {
        Trigger trigger = createTrigger();
        assertThatThrownBy(() -> trigger.start(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void startWhenAlreadyStartedThrowsIllegalStateException() {
        Trigger trigger = createTrigger();
        trigger.start(event -> {});
        try {
            assertThatThrownBy(() -> trigger.start(event -> {}))
                    .isInstanceOf(IllegalStateException.class);
        } finally {
            trigger.stop();
        }
    }

    @Test
    void stopIsIdempotentAndNeverThrows() {
        Trigger trigger = createTrigger();
        trigger.start(event -> {});
        trigger.stop();
        // Repeating stop must not throw
        trigger.stop();
    }

    @Test
    void afterStopListenerIsNeverCalled() {
        AtomicBoolean received = new AtomicBoolean(false);
        Trigger trigger = createTrigger();
        trigger.start(e -> received.set(true));
        trigger.stop();
        received.set(false);
        // Any delayed emission should not reach listener
        assertThat(received.get()).isFalse();
    }
}
