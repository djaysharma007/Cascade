package io.github.djaysharma007.cascade.core;

import java.util.Objects;
import java.util.UUID;

/**
 * An id is never null and two ids are equal only if their UUIDs are equal;
 * they are never interchangeable with other id types.
 *
 * @param value the wrapped UUID, never null
 */
public record WorkflowId(UUID value) {

    public WorkflowId {
        Objects.requireNonNull(value, "WorkflowId value must not be null");
    }

    public static WorkflowId random() {
        return new WorkflowId(UUID.randomUUID());
    }

    public static WorkflowId of(String uuidStr) {
        Objects.requireNonNull(uuidStr, "UUID string must not be null");
        return new WorkflowId(UUID.fromString(uuidStr));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
