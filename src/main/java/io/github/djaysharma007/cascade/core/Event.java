package io.github.djaysharma007.cascade.core;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * An event has an id, a timestamp and an unmodifiable payload, and is never mutated after creation.
 *
 * @param id unique event identifier, never null
 * @param timestamp time of occurrence, never null
 * @param payload unmodifiable key-value attributes of the event, never null
 */
public record Event(EventId id, Instant timestamp, Map<String, Object> payload) {

    public Event {
        Objects.requireNonNull(id, "Event id must not be null");
        Objects.requireNonNull(timestamp, "Event timestamp must not be null");
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public static Event of(Map<String, Object> payload) {
        return new Event(EventId.random(), Instant.now(), payload);
    }

    public static Event empty() {
        return new Event(EventId.random(), Instant.now(), Map.of());
    }
}
