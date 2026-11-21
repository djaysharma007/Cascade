package io.github.djaysharma007.cascade.core;

import java.util.Objects;
import java.util.UUID;

/**
 * An id is never null and two ids are equal only if their UUIDs are equal;
 * they are never interchangeable with other id types.
 *
 * @param value the wrapped UUID, never null
 */
public record EventId(UUID value) {

    public EventId {
        Objects.requireNonNull(value, "EventId value must not be null");
    }

    public static EventId random() {
        return new EventId(UUID.randomUUID());
    }

    public static EventId of(String uuidStr) {
        Objects.requireNonNull(uuidStr, "UUID string must not be null");
        return new EventId(UUID.fromString(uuidStr));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
