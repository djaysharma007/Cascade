package io.github.djaysharma007.cascade.plugins.triggers;

import io.github.djaysharma007.cascade.core.Event;
import io.github.djaysharma007.cascade.core.Plugin;
import io.github.djaysharma007.cascade.core.PluginCategory;
import io.github.djaysharma007.cascade.core.Trigger;
import io.github.djaysharma007.cascade.core.TriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Triggers workflow execution on a scheduled time-of-day daily or on specific days of the week.
 */
@Plugin(id = "schedule", displayName = "Schedule", category = PluginCategory.TRIGGER)
public final class ScheduleTrigger implements Trigger {

    private static final Logger log = LoggerFactory.getLogger(ScheduleTrigger.class);
    private static final LocalTime DEFAULT_TIME = LocalTime.of(9, 0);

    private final LocalTime scheduledTime;
    private final String schedulePattern;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> nextRunFuture;
    private volatile TriggerListener listener;

    public ScheduleTrigger(String timeStr, String schedulePattern) {
        this.scheduledTime = parseTime(timeStr);
        this.schedulePattern = schedulePattern != null && !schedulePattern.isBlank()
                ? schedulePattern.trim().toLowerCase(Locale.ROOT)
                : "daily";
    }

    private static LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            return DEFAULT_TIME;
        }
        try {
            return LocalTime.parse(timeStr.trim());
        } catch (DateTimeParseException e) {
            log.warn("Invalid time string '{}', using default 09:00", timeStr);
            return DEFAULT_TIME;
        }
    }

    @Override
    public synchronized void start(TriggerListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("ScheduleTrigger already started");
        }
        this.listener = listener;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cascade-schedule-trigger");
            t.setDaemon(true);
            return t;
        });

        scheduleNextExecution();
        log.info("ScheduleTrigger started for {} ({})", scheduledTime, schedulePattern);
    }

    @Override
    public synchronized void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        if (nextRunFuture != null) {
            nextRunFuture.cancel(false);
            nextRunFuture = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        listener = null;
        log.info("ScheduleTrigger stopped");
    }

    private synchronized void scheduleNextExecution() {
        if (!running.get() || scheduler == null || scheduler.isShutdown()) {
            return;
        }
        long delaySeconds = computeDelayToNextRun(LocalDateTime.now());
        nextRunFuture = scheduler.schedule(this::onScheduledTimeReached, delaySeconds, TimeUnit.SECONDS);
    }

    private void onScheduledTimeReached() {
        if (!running.get()) {
            return;
        }
        try {
            TriggerListener l = this.listener;
            if (l != null) {
                Instant now = Instant.now();
                Event event = Event.of(Map.of(
                        "firedAt", now.toString(),
                        "scheduledTime", scheduledTime.toString(),
                        "schedulePattern", schedulePattern
                ));
                l.onEvent(event);
            }
        } catch (Exception e) {
            log.error("Error firing scheduled event", e);
        } finally {
            scheduleNextExecution();
        }
    }

    public long computeDelayToNextRun(LocalDateTime now) {
        LocalDateTime candidate = now.with(scheduledTime);
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }

        while (!matchesDayPattern(candidate.toLocalDate())) {
            candidate = candidate.plusDays(1);
        }

        return Math.max(1L, Duration.between(now, candidate).toSeconds());
    }

    private boolean matchesDayPattern(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return switch (schedulePattern) {
            case "daily", "all" -> true;
            case "weekdays" -> dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
            case "weekends" -> dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
            case "monday" -> dow == DayOfWeek.MONDAY;
            case "tuesday" -> dow == DayOfWeek.TUESDAY;
            case "wednesday" -> dow == DayOfWeek.WEDNESDAY;
            case "thursday" -> dow == DayOfWeek.THURSDAY;
            case "friday" -> dow == DayOfWeek.FRIDAY;
            case "saturday" -> dow == DayOfWeek.SATURDAY;
            case "sunday" -> dow == DayOfWeek.SUNDAY;
            default -> true;
        };
    }

    public LocalTime getScheduledTime() {
        return scheduledTime;
    }

    public String getSchedulePattern() {
        return schedulePattern;
    }

    public boolean isRunning() {
        return running.get();
    }
}
