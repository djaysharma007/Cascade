package io.github.djaysharma007.cascade.core;

import java.util.Objects;
import java.util.UUID;

/**
 * An id is never null and two ids are equal only if their UUIDs are equal;
 * they are never interchangeable with other id types.
 *
 * @param value the wrapped UUID, never null
 */
public record RunId(UUID value) {

    public RunId {
        Objects.requireNonNull(value, "RunId value must not be null");
    }

    public static RunId random() {
        return new RunId(UUID.randomUUID());
    }

    public static RunId of(String uuidStr) {
        Objects.requireNonNull(uuidStr, "UUID string must not be null");
        return new RunId(UUID.fromString(uuidStr));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
