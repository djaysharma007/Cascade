package io.github.djaysharma007.cascade.plugins.triggers;

import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.Plugin;
import io.github.djaysharma007.cascade.core.PluginCategory;
import io.github.djaysharma007.cascade.core.Trigger;
import io.github.djaysharma007.cascade.core.TriggerListener;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manual trigger for user-initiated workflow test runs.
 */
@Plugin(id = "manual", displayName = "Manual", category = PluginCategory.TRIGGER)
public final class ManualTrigger implements Trigger {

    private final String label;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private volatile TriggerListener listener;

    public ManualTrigger(String label) {
        this.label = label;
    }

    @Override
    public synchronized void start(TriggerListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("ManualTrigger already started");
        }
        this.listener = listener;
    }

    @Override
    public synchronized void stop() {
        started.set(false);
        this.listener = null;
    }

    public void fire(Event event) {
        TriggerListener current = this.listener;
        if (started.get() && current != null) {
            current.onEvent(event != null ? event : Event.empty());
        }
    }

    public String label() {
        return label;
    }
}
