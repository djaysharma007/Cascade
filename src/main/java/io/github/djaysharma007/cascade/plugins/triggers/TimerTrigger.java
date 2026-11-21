package io.github.djaysharma007.cascade.plugins.triggers;

import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.Plugin;
import io.github.djaysharma007.cascade.core.PluginCategory;
import io.github.djaysharma007.cascade.core.Trigger;
import io.github.djaysharma007.cascade.core.TriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Periodic timer trigger firing at configured intervals.
 */
@Plugin(id = "timer", displayName = "Timer", category = PluginCategory.TRIGGER)
public final class TimerTrigger implements Trigger {

    private static final Logger log = LoggerFactory.getLogger(TimerTrigger.class);

    private final long intervalSeconds;
    private final boolean fireImmediately;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;
    private volatile TriggerListener listener;

    public TimerTrigger(long intervalSeconds, boolean fireImmediately) {
        if (intervalSeconds < 1) {
            throw new IllegalArgumentException("intervalSeconds must be at least 1");
        }
        this.intervalSeconds = intervalSeconds;
        this.fireImmediately = fireImmediately;
    }

    @Override
    public synchronized void start(TriggerListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("TimerTrigger already started");
        }
        this.listener = listener;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cascade-timer-trigger");
            t.setDaemon(true);
            return t;
        });

        long initialDelay = fireImmediately ? 0 : intervalSeconds;
        scheduler.scheduleAtFixedRate(this::tick, initialDelay, intervalSeconds, TimeUnit.SECONDS);
        log.debug("TimerTrigger started with interval {}s", intervalSeconds);
    }

    private void tick() {
        TriggerListener current = this.listener;
        if (started.get() && current != null) {
            try {
                Event event = Event.of(Map.of("firedAt", Instant.now().toString()));
                current.onEvent(event);
            } catch (Exception e) {
                log.warn("Error invoking listener in TimerTrigger", e);
            }
        }
    }

    @Override
    public synchronized void stop() {
        started.set(false);
        this.listener = null;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}
